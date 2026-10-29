package com.hotel.dao;

import com.hotel.modelo.Pago;
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
 * Acceso a datos de los pagos y anticipos.
 *
 * @author Ronald Perea
 */
public class PagoDAO {

    /** Consulta base con los JOIN que necesitan todas las lecturas. */
    private static final String SELECT_BASE =
            "SELECT p.*, u.nombre_completo AS nombre_usuario, "
          + "       r.codigo AS codigo_reserva, "
          + "       CONCAT(c.nombre, ' ', c.apellido) AS nombre_cliente "
          + "FROM pago p "
          + "JOIN usuario u ON u.id_usuario = p.id_usuario "
          + "JOIN reserva r ON r.id_reserva = p.id_reserva "
          + "JOIN cliente c ON c.id_cliente = r.id_cliente ";

    /**
     * Registra un pago, anticipo o reembolso.
     *
     * @param pago datos del movimiento
     * @return el identificador asignado por la base de datos
     * @throws SQLException si falla la insercion
     */
    public int insertar(Pago pago) throws SQLException {
        String sql = "INSERT INTO pago "
                   + "(id_reserva, tipo, metodo, monto, referencia, id_usuario) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pago.getIdReserva());
            ps.setString(2, pago.getTipo());
            ps.setString(3, pago.getMetodo());
            ps.setBigDecimal(4, pago.getMonto());
            ps.setString(5, pago.getReferencia());
            ps.setInt(6, pago.getIdUsuario());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    pago.setIdPago(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Elimina un pago registrado por error.
     *
     * Solo el administrador puede hacerlo. Un recepcionista que se
     * equivoque debe registrar un REEMBOLSO, que deja constancia del
     * movimiento en lugar de borrarlo del historial de caja.
     *
     * @param idPago pago a eliminar
     * @throws SQLException si falla la eliminacion
     */
    public void eliminar(int idPago) throws SQLException {
        String sql = "DELETE FROM pago WHERE id_pago = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPago);
            ps.executeUpdate();
        }
    }

    /**
     * Busca un pago por su identificador.
     *
     * @param idPago identificador a buscar
     * @return el pago encontrado, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Pago buscarPorId(int idPago) throws SQLException {
        String sql = SELECT_BASE + "WHERE p.id_pago = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPago);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Lista los movimientos de dinero de una reserva.
     *
     * @param idReserva reserva a consultar
     * @return lista ordenada del mas reciente al mas antiguo
     * @throws SQLException si falla la consulta
     */
    public List<Pago> listarPorReserva(int idReserva) throws SQLException {
        String sql = SELECT_BASE + "WHERE p.id_reserva = ? ORDER BY p.fecha DESC";
        return ejecutarConsulta(sql, List.of(idReserva));
    }

    /**
     * Lista los pagos recibidos dentro de un rango de fechas.
     * Es el corte de caja del periodo.
     *
     * @param desde primer dia del periodo
     * @param hasta ultimo dia del periodo
     * @return lista ordenada por fecha descendente
     * @throws SQLException si falla la consulta
     */
    public List<Pago> listarPorFechas(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = SELECT_BASE
                   + "WHERE DATE(p.fecha) BETWEEN ? AND ? ORDER BY p.fecha DESC";
        return ejecutarConsulta(sql, List.of(desde, hasta));
    }

    /**
     * Suma lo pagado por una reserva, restando los reembolsos.
     *
     * @param idReserva reserva a consultar
     * @return total neto pagado
     * @throws SQLException si falla la consulta
     */
    public BigDecimal sumarPorReserva(int idReserva) throws SQLException {
        String sql = "SELECT COALESCE(SUM(CASE WHEN tipo = 'REEMBOLSO' "
                   + "                          THEN -monto ELSE monto END), 0) "
                   + "FROM pago WHERE id_reserva = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Agrupa el dinero recibido en un periodo por metodo de pago.
     *
     * Sirve para cuadrar la caja al cierre del dia: cuanto debe haber en
     * efectivo, cuanto entro por tarjeta y cuanto por transferencia.
     *
     * @param desde primer dia del periodo
     * @param hasta ultimo dia del periodo
     * @return mapa de metodo a total neto
     * @throws SQLException si falla la consulta
     */
    public Map<String, BigDecimal> totalizarPorMetodo(LocalDate desde, LocalDate hasta)
            throws SQLException {
        String sql = "SELECT metodo, "
                   + "       SUM(CASE WHEN tipo = 'REEMBOLSO' THEN -monto ELSE monto END) AS total "
                   + "FROM pago WHERE DATE(fecha) BETWEEN ? AND ? "
                   + "GROUP BY metodo ORDER BY total DESC";

        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totales.put(rs.getString("metodo"), rs.getBigDecimal("total"));
                }
            }
        }
        return totales;
    }

    /**
     * Ejecuta una consulta que devuelve pagos.
     *
     * @param sql        consulta a ejecutar
     * @param parametros valores en el orden de los signos de interrogacion
     * @return lista de pagos
     * @throws SQLException si falla la consulta
     */
    private List<Pago> ejecutarConsulta(String sql, List<Object> parametros)
            throws SQLException {
        List<Pago> lista = new ArrayList<>();
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
     * Convierte la fila actual del ResultSet en un Pago.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Pago mapear(ResultSet rs) throws SQLException {
        Pago pago = new Pago();
        pago.setIdPago(rs.getInt("id_pago"));
        pago.setIdReserva(rs.getInt("id_reserva"));
        pago.setTipo(rs.getString("tipo"));
        pago.setMetodo(rs.getString("metodo"));
        pago.setMonto(rs.getBigDecimal("monto"));
        pago.setReferencia(rs.getString("referencia"));
        pago.setFecha(rs.getObject("fecha", LocalDateTime.class));
        pago.setIdUsuario(rs.getInt("id_usuario"));
        pago.setNombreUsuario(rs.getString("nombre_usuario"));
        pago.setCodigoReserva(rs.getString("codigo_reserva"));
        pago.setNombreCliente(rs.getString("nombre_cliente"));
        return pago;
    }
}
