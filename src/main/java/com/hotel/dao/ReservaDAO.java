package com.hotel.dao;

import com.hotel.modelo.EstadoCuenta;
import com.hotel.modelo.Reserva;
import com.hotel.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de las reservas.
 *
 * Es el DAO mas extenso del sistema porque la reserva es la entidad
 * central: de ella dependen el panel de recepcion, los estados de cuenta
 * y los reportes de ocupacion.
 *
 * @author Ronald Perea
 */
public class ReservaDAO {

    /**
     * Consulta base con los JOIN que necesitan todas las lecturas.
     * Trae el nombre del cliente, el numero de habitacion y el empleado
     * que registro la reserva, para que las vistas no tengan que hacer
     * una consulta adicional por cada fila de la tabla.
     */
    private static final String SELECT_BASE =
            "SELECT r.*, "
          + "       CONCAT(c.nombre, ' ', c.apellido) AS nombre_cliente, "
          + "       c.documento AS documento_cliente, "
          + "       c.telefono  AS telefono_cliente, "
          + "       h.numero    AS numero_habitacion, "
          + "       t.nombre    AS nombre_tipo, "
          + "       u.nombre_completo AS nombre_usuario "
          + "FROM reserva r "
          + "JOIN cliente c         ON c.id_cliente    = r.id_cliente "
          + "JOIN habitacion h      ON h.id_habitacion = r.id_habitacion "
          + "JOIN tipo_habitacion t ON t.id_tipo       = h.id_tipo "
          + "JOIN usuario u         ON u.id_usuario    = r.id_usuario ";

    // -----------------------------------------------------------------
    // Escritura
    // -----------------------------------------------------------------

    /**
     * Inserta una reserva nueva.
     *
     * @param reserva datos de la reserva, con la tarifa ya calculada
     * @return el identificador asignado por la base de datos
     * @throws SQLException si falla la insercion
     */
    public int insertar(Reserva reserva) throws SQLException {
        String sql = "INSERT INTO reserva "
                   + "(codigo, id_cliente, id_habitacion, fecha_entrada, fecha_salida, "
                   + " adultos, ninos, tarifa_noche, noches, estado, observaciones, id_usuario) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, reserva.getCodigo());
            ps.setInt(2, reserva.getIdCliente());
            ps.setInt(3, reserva.getIdHabitacion());
            ps.setObject(4, reserva.getFechaEntrada());
            ps.setObject(5, reserva.getFechaSalida());
            ps.setInt(6, reserva.getAdultos());
            ps.setInt(7, reserva.getNinos());
            ps.setBigDecimal(8, reserva.getTarifaNoche());
            ps.setInt(9, reserva.getNoches());
            ps.setString(10, reserva.getEstado());
            ps.setString(11, reserva.getObservaciones());
            ps.setInt(12, reserva.getIdUsuario());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    reserva.setIdReserva(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Actualiza los datos modificables de una reserva.
     *
     * No se toca el codigo, que es inmutable, ni las fechas de check-in y
     * check-out, que las escriben sus propios metodos.
     *
     * @param reserva datos a guardar, con su identificador
     * @throws SQLException si falla la actualizacion
     */
    public void actualizar(Reserva reserva) throws SQLException {
        String sql = "UPDATE reserva SET id_cliente = ?, id_habitacion = ?, "
                   + "       fecha_entrada = ?, fecha_salida = ?, adultos = ?, "
                   + "       ninos = ?, tarifa_noche = ?, noches = ?, "
                   + "       observaciones = ? WHERE id_reserva = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reserva.getIdCliente());
            ps.setInt(2, reserva.getIdHabitacion());
            ps.setObject(3, reserva.getFechaEntrada());
            ps.setObject(4, reserva.getFechaSalida());
            ps.setInt(5, reserva.getAdultos());
            ps.setInt(6, reserva.getNinos());
            ps.setBigDecimal(7, reserva.getTarifaNoche());
            ps.setInt(8, reserva.getNoches());
            ps.setString(9, reserva.getObservaciones());
            ps.setInt(10, reserva.getIdReserva());
            ps.executeUpdate();
        }
    }

    /**
     * Cambia unicamente el estado de una reserva.
     *
     * @param idReserva reserva a modificar
     * @param estado    uno de los ESTADO_* de la clase Reserva
     * @throws SQLException si falla la actualizacion
     */
    public void cambiarEstado(int idReserva, String estado) throws SQLException {
        String sql = "UPDATE reserva SET estado = ? WHERE id_reserva = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idReserva);
            ps.executeUpdate();
        }
    }

    /**
     * Registra la entrada del huesped.
     *
     * Son dos cambios que deben ocurrir juntos: la reserva pasa a EN_CASA
     * y la habitacion pasa a OCUPADA. Se ejecutan dentro de una
     * transaccion para que no pueda quedar una reserva en casa con la
     * habitacion marcada como libre, lo que provocaria que recepcion se
     * la vendiera a otra persona.
     *
     * @param idReserva    reserva que hace el check-in
     * @param idHabitacion habitacion que se ocupa
     * @throws SQLException si falla alguno de los dos cambios
     */
    public void registrarCheckin(int idReserva, int idHabitacion) throws SQLException {
        String sqlReserva = "UPDATE reserva SET estado = 'EN_CASA', fecha_checkin = NOW() "
                          + " WHERE id_reserva = ?";
        String sqlHabitacion = "UPDATE habitacion SET estado = 'OCUPADA' "
                             + " WHERE id_habitacion = ?";

        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false); // inicia la transaccion

            try (PreparedStatement ps = con.prepareStatement(sqlReserva)) {
                ps.setInt(1, idReserva);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(sqlHabitacion)) {
                ps.setInt(1, idHabitacion);
                ps.executeUpdate();
            }
            con.commit(); // los dos cambios quedan firmes

        } catch (SQLException e) {
            deshacer(con);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Registra la salida del huesped.
     *
     * Igual que el check-in, son dos cambios atomicos: la reserva pasa a
     * FINALIZADA y la habitacion queda en LIMPIEZA, no en DISPONIBLE,
     * porque antes de recibir al siguiente huesped tiene que asearse.
     *
     * @param idReserva    reserva que hace el check-out
     * @param idHabitacion habitacion que se desocupa
     * @throws SQLException si falla alguno de los dos cambios
     */
    public void registrarCheckout(int idReserva, int idHabitacion) throws SQLException {
        String sqlReserva = "UPDATE reserva SET estado = 'FINALIZADA', fecha_checkout = NOW() "
                          + " WHERE id_reserva = ?";
        String sqlHabitacion = "UPDATE habitacion SET estado = 'LIMPIEZA' "
                             + " WHERE id_habitacion = ?";

        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlReserva)) {
                ps.setInt(1, idReserva);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(sqlHabitacion)) {
                ps.setInt(1, idHabitacion);
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
     * Genera el siguiente codigo correlativo de reserva.
     *
     * El formato es RES-aaaa-nnnn, por ejemplo RES-2026-0011. Se busca
     * el mayor correlativo del ano en curso y se le suma uno.
     *
     * @return codigo listo para asignar a una reserva nueva
     * @throws SQLException si falla la consulta
     */
    public String generarCodigo() throws SQLException {
        int anio = LocalDate.now().getYear();
        String prefijo = "RES-" + anio + "-";

        // SUBSTRING extrae los cuatro digitos finales y CAST los vuelve
        // numero, para que 0010 se ordene despues de 0009 y no antes.
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 10) AS UNSIGNED)), 0) "
                   + "FROM reserva WHERE codigo LIKE ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefijo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int siguiente = rs.next() ? rs.getInt(1) + 1 : 1;
                return prefijo + String.format("%04d", siguiente);
            }
        }
    }

    // -----------------------------------------------------------------
    // Lectura
    // -----------------------------------------------------------------

    /**
     * Busca una reserva por su identificador.
     *
     * @param idReserva identificador a buscar
     * @return la reserva encontrada, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Reserva buscarPorId(int idReserva) throws SQLException {
        String sql = SELECT_BASE + "WHERE r.id_reserva = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Busca una reserva por su codigo visible.
     *
     * @param codigo codigo tipo RES-2026-0001
     * @return la reserva encontrada, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Reserva buscarPorCodigo(String codigo) throws SQLException {
        String sql = SELECT_BASE + "WHERE r.codigo = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Busca reservas combinando texto libre, estado y rango de fechas.
     *
     * Es el buscador general del modulo de reservas. Todos los filtros
     * son opcionales y se acumulan: se puede pedir solo las canceladas,
     * solo las de un cliente, o las canceladas de ese cliente en marzo.
     *
     * @param texto      codigo, nombre del cliente, documento o numero
     *                   de habitacion; puede venir vacio
     * @param estado     estado a filtrar, o null para todos
     * @param desde      fecha minima de entrada, o null
     * @param hasta      fecha maxima de entrada, o null
     * @return lista de reservas que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Reserva> buscar(String texto, String estado,
                                LocalDate desde, LocalDate hasta) throws SQLException {

        StringBuilder sql = new StringBuilder(SELECT_BASE + "WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append("AND (r.codigo LIKE ? "
                     + "  OR CONCAT(c.nombre, ' ', c.apellido) LIKE ? "
                     + "  OR c.documento LIKE ? "
                     + "  OR h.numero LIKE ?) ");
            String patron = "%" + texto.trim() + "%";
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }
        if (estado != null && !estado.isBlank()) {
            sql.append("AND r.estado = ? ");
            parametros.add(estado);
        }
        if (desde != null) {
            sql.append("AND r.fecha_entrada >= ? ");
            parametros.add(desde);
        }
        if (hasta != null) {
            sql.append("AND r.fecha_entrada <= ? ");
            parametros.add(hasta);
        }
        sql.append("ORDER BY r.fecha_entrada DESC, r.id_reserva DESC");

        return ejecutarConsulta(sql.toString(), parametros);
    }

    /**
     * Lista las reservas de un cliente, para su ficha de historial.
     *
     * @param idCliente cliente a consultar
     * @return lista ordenada de la mas reciente a la mas antigua
     * @throws SQLException si falla la consulta
     */
    public List<Reserva> listarPorCliente(int idCliente) throws SQLException {
        String sql = SELECT_BASE + "WHERE r.id_cliente = ? "
                   + "ORDER BY r.fecha_entrada DESC";
        return ejecutarConsulta(sql, List.of(idCliente));
    }

    /**
     * Lista las llegadas previstas para hoy que aun no han hecho
     * check-in. Alimenta el panel de recepcion.
     *
     * @return lista ordenada por numero de habitacion
     * @throws SQLException si falla la consulta
     */
    public List<Reserva> listarLlegadasDeHoy() throws SQLException {
        String sql = SELECT_BASE
                   + "WHERE r.fecha_entrada = CURDATE() "
                   + "  AND r.estado IN ('PENDIENTE','CONFIRMADA') "
                   + "ORDER BY h.numero";
        return ejecutarConsulta(sql, List.of());
    }

    /**
     * Lista las salidas previstas para hoy, mas las que ya se pasaron de
     * su fecha y siguen en casa.
     *
     * Incluir las vencidas es deliberado: si un huesped debio salir ayer
     * y nadie lo registro, tiene que seguir apareciendo en el panel hasta
     * que recepcion lo resuelva, no desaparecer de la pantalla.
     *
     * @return lista ordenada por fecha de salida
     * @throws SQLException si falla la consulta
     */
    public List<Reserva> listarSalidasDeHoy() throws SQLException {
        String sql = SELECT_BASE
                   + "WHERE r.estado = 'EN_CASA' AND r.fecha_salida <= CURDATE() "
                   + "ORDER BY r.fecha_salida, h.numero";
        return ejecutarConsulta(sql, List.of());
    }

    /**
     * Lista los huespedes que estan hospedados en este momento.
     *
     * @return lista ordenada por numero de habitacion
     * @throws SQLException si falla la consulta
     */
    public List<Reserva> listarEnCasa() throws SQLException {
        String sql = SELECT_BASE + "WHERE r.estado = 'EN_CASA' ORDER BY h.numero";
        return ejecutarConsulta(sql, List.of());
    }

    /**
     * Devuelve el estado de cuenta de una reserva.
     *
     * @param idReserva reserva a consultar
     * @return su estado de cuenta, o null si la reserva no existe
     * @throws SQLException si falla la consulta
     */
    public EstadoCuenta obtenerEstadoCuenta(int idReserva) throws SQLException {
        String sql = "SELECT * FROM vw_estado_cuenta WHERE id_reserva = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearEstadoCuenta(rs) : null;
            }
        }
    }

    /**
     * Lista las reservas que tienen saldo pendiente de cobro.
     *
     * @return lista ordenada por saldo descendente
     * @throws SQLException si falla la consulta
     */
    public List<EstadoCuenta> listarConSaldoPendiente() throws SQLException {
        String sql = "SELECT * FROM vw_estado_cuenta "
                   + " WHERE saldo_pendiente > 0 AND estado <> 'CANCELADA' "
                   + " ORDER BY saldo_pendiente DESC";
        List<EstadoCuenta> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearEstadoCuenta(rs));
            }
        }
        return lista;
    }

    // -----------------------------------------------------------------
    // Indicadores para el tablero
    // -----------------------------------------------------------------

    /**
     * Cuenta las reservas agrupadas por estado.
     *
     * @return mapa de estado a cantidad, con todos los estados presentes
     * @throws SQLException si falla la consulta
     */
    public Map<String, Integer> contarPorEstado() throws SQLException {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        conteo.put(Reserva.ESTADO_PENDIENTE, 0);
        conteo.put(Reserva.ESTADO_CONFIRMADA, 0);
        conteo.put(Reserva.ESTADO_EN_CASA, 0);
        conteo.put(Reserva.ESTADO_FINALIZADA, 0);
        conteo.put(Reserva.ESTADO_CANCELADA, 0);
        conteo.put(Reserva.ESTADO_NO_SHOW, 0);

        String sql = "SELECT estado, COUNT(*) AS cantidad FROM reserva GROUP BY estado";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                conteo.put(rs.getString("estado"), rs.getInt("cantidad"));
            }
        }
        return conteo;
    }

    /**
     * Suma los ingresos cobrados dentro de un rango de fechas.
     *
     * Se suman los PAGOS y no las facturas emitidas, porque lo que
     * interesa para la caja es el dinero que efectivamente entro. Los
     * reembolsos se restan.
     *
     * @param desde primer dia del periodo
     * @param hasta ultimo dia del periodo
     * @return total cobrado en el periodo
     * @throws SQLException si falla la consulta
     */
    public BigDecimal sumarIngresos(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(CASE WHEN tipo = 'REEMBOLSO' "
                   + "                          THEN -monto ELSE monto END), 0) "
                   + "FROM pago WHERE DATE(fecha) BETWEEN ? AND ?";
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
     * Calcula el porcentaje de ocupacion del hotel el dia de hoy.
     *
     * Es la proporcion entre las habitaciones con huesped dentro y el
     * total de habitaciones vendibles. Se excluyen del divisor las que
     * estan en mantenimiento o bloqueadas, porque no estaban a la venta
     * y contarlas castigaria el indicador injustamente.
     *
     * @return porcentaje entre 0 y 100
     * @throws SQLException si falla la consulta
     */
    public int calcularOcupacionHoy() throws SQLException {
        String sql = "SELECT "
                   + "  (SELECT COUNT(*) FROM reserva "
                   + "    WHERE estado = 'EN_CASA') AS ocupadas, "
                   + "  (SELECT COUNT(*) FROM habitacion "
                   + "    WHERE activo = 1 "
                   + "      AND estado NOT IN ('MANTENIMIENTO','BLOQUEADA')) AS vendibles";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int ocupadas = rs.getInt("ocupadas");
                int vendibles = rs.getInt("vendibles");
                if (vendibles <= 0) {
                    return 0;
                }
                return (int) Math.round((ocupadas * 100.0) / vendibles);
            }
        }
        return 0;
    }

    // -----------------------------------------------------------------
    // Metodos internos de apoyo
    // -----------------------------------------------------------------

    /**
     * Ejecuta una consulta que devuelve reservas, asignando los
     * parametros que reciba.
     *
     * @param sql        consulta a ejecutar
     * @param parametros valores en el mismo orden que los signos de
     *                   interrogacion de la consulta
     * @return lista de reservas
     * @throws SQLException si falla la consulta
     */
    private List<Reserva> ejecutarConsulta(String sql, List<Object> parametros)
            throws SQLException {
        List<Reserva> lista = new ArrayList<>();
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
     * Si el rollback tambien falla no tiene sentido lanzar esa segunda
     * excepcion, porque taparia la original, que es la que explica el
     * problema de verdad.
     *
     * @param con conexion a revertir, puede ser null
     */
    private void deshacer(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ignorada) {
                // Se ignora a proposito: ver el comentario de arriba.
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
     * Convierte la fila actual del ResultSet en una Reserva, incluidos
     * los datos que traen los JOIN.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Reserva mapear(ResultSet rs) throws SQLException {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(rs.getInt("id_reserva"));
        reserva.setCodigo(rs.getString("codigo"));
        reserva.setIdCliente(rs.getInt("id_cliente"));
        reserva.setIdHabitacion(rs.getInt("id_habitacion"));
        reserva.setFechaEntrada(rs.getObject("fecha_entrada", LocalDate.class));
        reserva.setFechaSalida(rs.getObject("fecha_salida", LocalDate.class));
        reserva.setAdultos(rs.getInt("adultos"));
        reserva.setNinos(rs.getInt("ninos"));
        reserva.setTarifaNoche(rs.getBigDecimal("tarifa_noche"));
        reserva.setNoches(rs.getInt("noches"));
        reserva.setEstado(rs.getString("estado"));
        reserva.setFechaCheckin(rs.getObject("fecha_checkin", LocalDateTime.class));
        reserva.setFechaCheckout(rs.getObject("fecha_checkout", LocalDateTime.class));
        reserva.setObservaciones(rs.getString("observaciones"));
        reserva.setIdUsuario(rs.getInt("id_usuario"));
        reserva.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));

        reserva.setNombreCliente(rs.getString("nombre_cliente"));
        reserva.setDocumentoCliente(rs.getString("documento_cliente"));
        reserva.setTelefonoCliente(rs.getString("telefono_cliente"));
        reserva.setNumeroHabitacion(rs.getString("numero_habitacion"));
        reserva.setNombreTipo(rs.getString("nombre_tipo"));
        reserva.setNombreUsuario(rs.getString("nombre_usuario"));
        return reserva;
    }

    /**
     * Convierte la fila actual del ResultSet en un EstadoCuenta.
     *
     * @param rs cursor posicionado en una fila de vw_estado_cuenta
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private EstadoCuenta mapearEstadoCuenta(ResultSet rs) throws SQLException {
        EstadoCuenta cuenta = new EstadoCuenta();
        cuenta.setIdReserva(rs.getInt("id_reserva"));
        cuenta.setCodigo(rs.getString("codigo"));
        cuenta.setIdCliente(rs.getInt("id_cliente"));
        cuenta.setCliente(rs.getString("cliente"));
        cuenta.setHabitacion(rs.getString("habitacion"));
        cuenta.setFechaEntrada(rs.getObject("fecha_entrada", LocalDate.class));
        cuenta.setFechaSalida(rs.getObject("fecha_salida", LocalDate.class));
        cuenta.setNoches(rs.getInt("noches"));
        cuenta.setTarifaNoche(rs.getBigDecimal("tarifa_noche"));
        cuenta.setEstado(rs.getString("estado"));
        cuenta.setTotalHospedaje(rs.getBigDecimal("total_hospedaje"));
        cuenta.setTotalConsumos(rs.getBigDecimal("total_consumos"));
        cuenta.setTotalPagado(rs.getBigDecimal("total_pagado"));
        cuenta.setSaldoPendiente(rs.getBigDecimal("saldo_pendiente"));
        return cuenta;
    }
}
