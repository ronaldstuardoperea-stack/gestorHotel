package com.hotel.dao;

import com.hotel.modelo.DetalleFactura;
import com.hotel.modelo.Factura;
import com.hotel.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la facturacion.
 *
 * Es el DAO con mas cuidado transaccional del sistema, porque emitir una
 * factura implica escribir en tres tablas a la vez y todas deben quedar
 * consistentes entre si.
 *
 * @author Ronald Perea
 */
public class FacturaDAO {

    /** Consulta base con los JOIN que necesitan todas las lecturas. */
    private static final String SELECT_BASE =
            "SELECT f.*, r.codigo AS codigo_reserva, h.numero AS numero_habitacion, "
          + "       o.nombre_completo AS nombre_ocupante, "
          + "       u.nombre_completo AS nombre_usuario "
          + "FROM factura f "
          + "JOIN reserva r     ON r.id_reserva    = f.id_reserva "
          + "JOIN habitacion h  ON h.id_habitacion = r.id_habitacion "
          + "LEFT JOIN ocupante o ON o.id_ocupante = f.id_ocupante "
          + "JOIN usuario u     ON u.id_usuario    = f.id_usuario ";

    /**
     * Emite una factura completa.
     *
     * Son tres operaciones que forman una sola unidad indivisible:
     *
     *  1. Se inserta el encabezado de la factura.
     *  2. Se insertan sus renglones de detalle.
     *  3. Se marcan los consumos incluidos como ya facturados.
     *
     * Las tres van dentro de una transaccion. Si fallara la tercera
     * despues de haber hecho las dos primeras, quedaria una factura
     * emitida cuyos consumos siguen apareciendo como pendientes, y el
     * cliente terminaria pagando dos veces lo mismo. El rollback evita
     * exactamente eso: o queda todo, o no queda nada.
     *
     * El numero correlativo se calcula dentro de la misma transaccion,
     * despues de bloquear la tabla con FOR UPDATE, para que dos cajeros
     * facturando al mismo tiempo no obtengan el mismo numero.
     *
     * @param factura   encabezado con sus totales ya calculados
     * @param idsCargos consumos que se incluyen; puede venir vacia
     * @return el identificador de la factura creada
     * @throws SQLException si falla cualquiera de los tres pasos
     */
    public int emitir(Factura factura, List<Integer> idsCargos) throws SQLException {
        String sqlNumero = "SELECT COALESCE(MAX(numero), 0) + 1 FROM factura "
                         + " WHERE serie = ? FOR UPDATE";

        String sqlFactura = "INSERT INTO factura "
                          + "(serie, numero, id_reserva, id_cliente, id_ocupante, "
                          + " nombre_receptor, nit_receptor, tipo, subtotal, descuento, "
                          + " total, estado, observaciones, id_usuario) "
                          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'EMITIDA', ?, ?)";

        String sqlDetalle = "INSERT INTO detalle_factura "
                          + "(id_factura, descripcion, cantidad, precio_unitario, importe) "
                          + "VALUES (?, ?, ?, ?, ?)";

        String sqlMarcar = "UPDATE cargo SET id_factura = ? "
                         + " WHERE id_cargo = ? AND id_factura IS NULL";

        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            // Paso 1: reservar el siguiente correlativo de la serie.
            int numero;
            try (PreparedStatement ps = con.prepareStatement(sqlNumero)) {
                ps.setString(1, factura.getSerie());
                try (ResultSet rs = ps.executeQuery()) {
                    numero = rs.next() ? rs.getInt(1) : 1;
                }
            }
            factura.setNumero(numero);

            // Paso 2: insertar el encabezado.
            int idFactura;
            try (PreparedStatement ps = con.prepareStatement(sqlFactura,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, factura.getSerie());
                ps.setInt(2, numero);
                ps.setInt(3, factura.getIdReserva());
                ps.setInt(4, factura.getIdCliente());
                if (factura.getIdOcupante() == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, factura.getIdOcupante());
                }
                ps.setString(6, factura.getNombreReceptor());
                ps.setString(7, factura.getNitReceptor());
                ps.setString(8, factura.getTipo());
                ps.setBigDecimal(9, factura.getSubtotal());
                ps.setBigDecimal(10, factura.getDescuento());
                ps.setBigDecimal(11, factura.getTotal());
                ps.setString(12, factura.getObservaciones());
                ps.setInt(13, factura.getIdUsuario());
                ps.executeUpdate();

                try (ResultSet llaves = ps.getGeneratedKeys()) {
                    idFactura = llaves.next() ? llaves.getInt(1) : 0;
                }
            }
            factura.setIdFactura(idFactura);

            // Paso 3: insertar los renglones del detalle.
            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                for (DetalleFactura detalle : factura.getDetalles()) {
                    ps.setInt(1, idFactura);
                    ps.setString(2, detalle.getDescripcion());
                    ps.setBigDecimal(3, detalle.getCantidad());
                    ps.setBigDecimal(4, detalle.getPrecioUnitario());
                    ps.setBigDecimal(5, detalle.getImporte());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Paso 4: marcar los consumos como ya facturados.
            if (idsCargos != null && !idsCargos.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(sqlMarcar)) {
                    for (Integer idCargo : idsCargos) {
                        ps.setInt(1, idFactura);
                        ps.setInt(2, idCargo);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            con.commit();
            return idFactura;

        } catch (SQLException e) {
            deshacer(con);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Anula una factura emitida por error.
     *
     * La factura no se borra: se marca como ANULADA para que la
     * numeracion no deje huecos, que es lo que exige cualquier control
     * fiscal. Los consumos que incluia quedan liberados poniendo su
     * id_factura de vuelta en NULL, de modo que puedan facturarse
     * correctamente en un documento nuevo.
     *
     * Las dos operaciones van en una transaccion: anular la factura sin
     * liberar los cargos dejaria consumos que ya nadie podria cobrar.
     *
     * @param idFactura     factura a anular
     * @param motivo        razon de la anulacion, que queda registrada
     * @throws SQLException si falla alguno de los dos cambios
     */
    public void anular(int idFactura, String motivo) throws SQLException {
        String sqlAnular = "UPDATE factura "
                         + "   SET estado = 'ANULADA', "
                         + "       observaciones = CONCAT(COALESCE(observaciones, ''), "
                         + "                              ' [ANULADA: ', ?, ']') "
                         + " WHERE id_factura = ? AND estado = 'EMITIDA'";

        String sqlLiberar = "UPDATE cargo SET id_factura = NULL WHERE id_factura = ?";

        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlAnular)) {
                ps.setString(1, motivo);
                ps.setInt(2, idFactura);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(sqlLiberar)) {
                ps.setInt(1, idFactura);
                ps.executeUpdate();
            }
            con.commit();

        } catch (SQLException e) {
            deshacer(con);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Busca una factura con todos sus renglones de detalle.
     *
     * @param idFactura identificador a buscar
     * @return la factura completa, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Factura buscarPorId(int idFactura) throws SQLException {
        String sql = SELECT_BASE + "WHERE f.id_factura = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Factura factura = mapear(rs);
                factura.setDetalles(listarDetalles(idFactura));
                return factura;
            }
        }
    }

    /**
     * Lista los renglones de una factura.
     *
     * @param idFactura factura a consultar
     * @return lista de detalles en el orden en que se emitieron
     * @throws SQLException si falla la consulta
     */
    public List<DetalleFactura> listarDetalles(int idFactura) throws SQLException {
        String sql = "SELECT * FROM detalle_factura WHERE id_factura = ? ORDER BY id_detalle";
        List<DetalleFactura> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleFactura detalle = new DetalleFactura();
                    detalle.setIdDetalle(rs.getInt("id_detalle"));
                    detalle.setIdFactura(rs.getInt("id_factura"));
                    detalle.setDescripcion(rs.getString("descripcion"));
                    detalle.setCantidad(rs.getBigDecimal("cantidad"));
                    detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    detalle.setImporte(rs.getBigDecimal("importe"));
                    lista.add(detalle);
                }
            }
        }
        return lista;
    }

    /**
     * Lista las facturas de una reserva.
     *
     * @param idReserva reserva a consultar
     * @return lista ordenada de la mas reciente a la mas antigua
     * @throws SQLException si falla la consulta
     */
    public List<Factura> listarPorReserva(int idReserva) throws SQLException {
        String sql = SELECT_BASE + "WHERE f.id_reserva = ? ORDER BY f.fecha_emision DESC";
        return ejecutarConsulta(sql, List.of(idReserva));
    }

    /**
     * Busca facturas por texto, estado y rango de fechas de emision.
     *
     * @param texto  numero de factura, nombre del receptor, NIT o codigo
     *               de reserva; puede venir vacio
     * @param estado EMITIDA o ANULADA, o null para ambas
     * @param desde  fecha minima de emision, o null
     * @param hasta  fecha maxima de emision, o null
     * @return lista de facturas que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Factura> buscar(String texto, String estado,
                                LocalDate desde, LocalDate hasta) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE + "WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append("AND (CONCAT(f.serie, '-', LPAD(f.numero, 6, '0')) LIKE ? "
                     + "  OR f.nombre_receptor LIKE ? "
                     + "  OR f.nit_receptor LIKE ? "
                     + "  OR r.codigo LIKE ?) ");
            String patron = "%" + texto.trim() + "%";
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }
        if (estado != null && !estado.isBlank()) {
            sql.append("AND f.estado = ? ");
            parametros.add(estado);
        }
        if (desde != null) {
            sql.append("AND DATE(f.fecha_emision) >= ? ");
            parametros.add(desde);
        }
        if (hasta != null) {
            sql.append("AND DATE(f.fecha_emision) <= ? ");
            parametros.add(hasta);
        }
        sql.append("ORDER BY f.fecha_emision DESC");

        return ejecutarConsulta(sql.toString(), parametros);
    }

    /**
     * Suma el monto facturado en un periodo, sin contar las anuladas.
     *
     * @param desde primer dia del periodo
     * @param hasta ultimo dia del periodo
     * @return total facturado
     * @throws SQLException si falla la consulta
     */
    public BigDecimal sumarFacturado(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM factura "
                   + " WHERE estado = 'EMITIDA' AND DATE(fecha_emision) BETWEEN ? AND ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Cuenta las facturas emitidas en un periodo.
     *
     * @param desde primer dia del periodo
     * @param hasta ultimo dia del periodo
     * @return cantidad de documentos vigentes
     * @throws SQLException si falla la consulta
     */
    public int contarEmitidas(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COUNT(*) FROM factura "
                   + " WHERE estado = 'EMITIDA' AND DATE(fecha_emision) BETWEEN ? AND ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Ejecuta una consulta que devuelve facturas, sin cargar detalles.
     *
     * @param sql        consulta a ejecutar
     * @param parametros valores en el orden de los signos de interrogacion
     * @return lista de facturas
     * @throws SQLException si falla la consulta
     */
    private List<Factura> ejecutarConsulta(String sql, List<Object> parametros)
            throws SQLException {
        List<Factura> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Revierte una transaccion sin propagar un fallo secundario.
     *
     * @param con conexion a revertir, puede ser null
     */
    private void deshacer(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ignorada) {
                // Se ignora para no tapar la excepcion original.
            }
        }
    }

    /**
     * Devuelve la conexion a su modo normal y la cierra.
     *
     * @param con conexion a cerrar, puede ser null
     */
    private void cerrar(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
                con.close();
            } catch (SQLException ignorada) {
                // La conexion se descarta de todos modos.
            }
        }
    }

    /**
     * Convierte la fila actual del ResultSet en una Factura.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila, sin sus detalles
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Factura mapear(ResultSet rs) throws SQLException {
        Factura factura = new Factura();
        factura.setIdFactura(rs.getInt("id_factura"));
        factura.setSerie(rs.getString("serie"));
        factura.setNumero(rs.getInt("numero"));
        factura.setIdReserva(rs.getInt("id_reserva"));
        factura.setIdCliente(rs.getInt("id_cliente"));

        int ocupanteLeido = rs.getInt("id_ocupante");
        factura.setIdOcupante(rs.wasNull() ? null : ocupanteLeido);

        factura.setNombreReceptor(rs.getString("nombre_receptor"));
        factura.setNitReceptor(rs.getString("nit_receptor"));
        factura.setTipo(rs.getString("tipo"));
        factura.setSubtotal(rs.getBigDecimal("subtotal"));
        factura.setDescuento(rs.getBigDecimal("descuento"));
        factura.setTotal(rs.getBigDecimal("total"));
        factura.setEstado(rs.getString("estado"));
        factura.setObservaciones(rs.getString("observaciones"));
        factura.setFechaEmision(rs.getObject("fecha_emision", LocalDateTime.class));
        factura.setIdUsuario(rs.getInt("id_usuario"));

        factura.setCodigoReserva(rs.getString("codigo_reserva"));
        factura.setNumeroHabitacion(rs.getString("numero_habitacion"));
        factura.setNombreOcupante(rs.getString("nombre_ocupante"));
        factura.setNombreUsuario(rs.getString("nombre_usuario"));
        return factura;
    }
}
