package com.hotel.dao;

import com.hotel.modelo.Cargo;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de los consumos cargados a las reservas.
 *
 * @author Ronald Perea
 */
public class CargoDAO {

    /** Consulta base con los JOIN que necesitan todas las lecturas. */
    private static final String SELECT_BASE =
            "SELECT cg.*, o.nombre_completo AS nombre_ocupante, "
          + "       u.nombre_completo AS nombre_usuario, r.codigo AS codigo_reserva "
          + "FROM cargo cg "
          + "LEFT JOIN ocupante o ON o.id_ocupante = cg.id_ocupante "
          + "JOIN usuario u       ON u.id_usuario  = cg.id_usuario "
          + "JOIN reserva r       ON r.id_reserva  = cg.id_reserva ";

    /**
     * Registra un consumo nuevo.
     *
     * @param cargo datos del consumo, con el monto ya calculado
     * @return el identificador asignado por la base de datos
     * @throws SQLException si falla la insercion
     */
    public int insertar(Cargo cargo) throws SQLException {
        String sql = "INSERT INTO cargo "
                   + "(id_reserva, id_ocupante, concepto, categoria, cantidad, "
                   + " precio_unitario, monto, id_usuario) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, cargo.getIdReserva());
            if (cargo.getIdOcupante() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, cargo.getIdOcupante());
            }
            ps.setString(3, cargo.getConcepto());
            ps.setString(4, cargo.getCategoria());
            ps.setBigDecimal(5, cargo.getCantidad());
            ps.setBigDecimal(6, cargo.getPrecioUnitario());
            ps.setBigDecimal(7, cargo.getMonto());
            ps.setInt(8, cargo.getIdUsuario());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    cargo.setIdCargo(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Elimina un consumo que aun no ha sido facturado.
     *
     * La condicion id_factura IS NULL forma parte del WHERE y no de una
     * verificacion previa en Java. Asi, aunque dos recepcionistas
     * intentaran borrar y facturar el mismo cargo al mismo tiempo, la
     * base de datos garantiza que no se pueda borrar algo ya facturado.
     *
     * @param idCargo consumo a eliminar
     * @return true si se elimino; false si ya estaba facturado
     * @throws SQLException si falla la eliminacion
     */
    public boolean eliminarSiNoFacturado(int idCargo) throws SQLException {
        String sql = "DELETE FROM cargo WHERE id_cargo = ? AND id_factura IS NULL";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCargo);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Busca un consumo por su identificador.
     *
     * @param idCargo identificador a buscar
     * @return el consumo encontrado, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Cargo buscarPorId(int idCargo) throws SQLException {
        String sql = SELECT_BASE + "WHERE cg.id_cargo = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCargo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Lista todos los consumos de una reserva.
     *
     * @param idReserva reserva a consultar
     * @return lista ordenada del mas reciente al mas antiguo
     * @throws SQLException si falla la consulta
     */
    public List<Cargo> listarPorReserva(int idReserva) throws SQLException {
        String sql = SELECT_BASE + "WHERE cg.id_reserva = ? ORDER BY cg.fecha DESC";
        return ejecutarConsulta(sql, List.of(idReserva));
    }

    /**
     * Lista los consumos de una reserva que aun no se han facturado.
     *
     * Es la lista que alimenta la facturacion de tipo CONSUMOS, para no
     * cobrar dos veces lo mismo.
     *
     * @param idReserva reserva a consultar
     * @return lista ordenada por fecha
     * @throws SQLException si falla la consulta
     */
    public List<Cargo> listarPendientesDeFacturar(int idReserva) throws SQLException {
        String sql = SELECT_BASE
                   + "WHERE cg.id_reserva = ? AND cg.id_factura IS NULL "
                   + "ORDER BY cg.fecha";
        return ejecutarConsulta(sql, List.of(idReserva));
    }

    /**
     * Lista los consumos sin facturar de un ocupante en particular.
     *
     * Alimenta la facturacion de tipo OCUPANTE, que es la que permite
     * dividir la cuenta entre varios huespedes de la misma habitacion.
     *
     * @param idReserva  reserva a consultar
     * @param idOcupante huesped cuyos consumos se quieren facturar
     * @return lista ordenada por fecha
     * @throws SQLException si falla la consulta
     */
    public List<Cargo> listarPendientesPorOcupante(int idReserva, int idOcupante)
            throws SQLException {
        String sql = SELECT_BASE
                   + "WHERE cg.id_reserva = ? AND cg.id_ocupante = ? "
                   + "  AND cg.id_factura IS NULL ORDER BY cg.fecha";
        return ejecutarConsulta(sql, List.of(idReserva, idOcupante));
    }

    /**
     * Suma los consumos pendientes de facturar de una reserva.
     *
     * @param idReserva reserva a consultar
     * @return total pendiente, o cero si no hay
     * @throws SQLException si falla la consulta
     */
    public BigDecimal sumarPendientes(int idReserva) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM cargo "
                   + " WHERE id_reserva = ? AND id_factura IS NULL";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Suma el total de consumos de una reserva, facturados o no.
     *
     * @param idReserva reserva a consultar
     * @return total consumido
     * @throws SQLException si falla la consulta
     */
    public BigDecimal sumarTotal(int idReserva) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM cargo WHERE id_reserva = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Agrupa los consumos de un periodo por categoria.
     *
     * Sirve para el reporte que muestra de donde vienen los ingresos
     * adicionales del hotel: cuanto del restaurante, cuanto del spa.
     *
     * @param desde primer dia del periodo
     * @param hasta ultimo dia del periodo
     * @return mapa de categoria a total, de mayor a menor
     * @throws SQLException si falla la consulta
     */
    public Map<String, BigDecimal> totalizarPorCategoria(LocalDate desde, LocalDate hasta)
            throws SQLException {
        String sql = "SELECT categoria, SUM(monto) AS total FROM cargo "
                   + " WHERE DATE(fecha) BETWEEN ? AND ? "
                   + " GROUP BY categoria ORDER BY total DESC";

        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totales.put(rs.getString("categoria"), rs.getBigDecimal("total"));
                }
            }
        }
        return totales;
    }

    /**
     * Ejecuta una consulta que devuelve consumos.
     *
     * @param sql        consulta a ejecutar
     * @param parametros valores en el orden de los signos de interrogacion
     * @return lista de consumos
     * @throws SQLException si falla la consulta
     */
    private List<Cargo> ejecutarConsulta(String sql, List<Object> parametros)
            throws SQLException {
        List<Cargo> lista = new ArrayList<>();
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
     * Convierte la fila actual del ResultSet en un Cargo.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Cargo mapear(ResultSet rs) throws SQLException {
        Cargo cargo = new Cargo();
        cargo.setIdCargo(rs.getInt("id_cargo"));
        cargo.setIdReserva(rs.getInt("id_reserva"));

        int ocupanteLeido = rs.getInt("id_ocupante");
        cargo.setIdOcupante(rs.wasNull() ? null : ocupanteLeido);

        cargo.setConcepto(rs.getString("concepto"));
        cargo.setCategoria(rs.getString("categoria"));
        cargo.setCantidad(rs.getBigDecimal("cantidad"));
        cargo.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        cargo.setMonto(rs.getBigDecimal("monto"));

        int facturaLeida = rs.getInt("id_factura");
        cargo.setIdFactura(rs.wasNull() ? null : facturaLeida);

        cargo.setFecha(rs.getObject("fecha", LocalDateTime.class));
        cargo.setIdUsuario(rs.getInt("id_usuario"));
        cargo.setNombreOcupante(rs.getString("nombre_ocupante"));
        cargo.setNombreUsuario(rs.getString("nombre_usuario"));
        cargo.setCodigoReserva(rs.getString("codigo_reserva"));
        return cargo;
    }
}
