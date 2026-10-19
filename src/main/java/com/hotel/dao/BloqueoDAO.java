package com.hotel.dao;

import com.hotel.modelo.BloqueoHabitacion;
import com.hotel.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los bloqueos de habitaciones.
 *
 * @author Ronald Perea
 */
public class BloqueoDAO {

    /** Consulta base con los JOIN que necesitan todas las lecturas. */
    private static final String SELECT_BASE =
            "SELECT b.*, h.numero AS numero_habitacion, t.nombre AS nombre_tipo, "
          + "       u.nombre_completo AS nombre_usuario "
          + "FROM bloqueo_habitacion b "
          + "JOIN habitacion h      ON h.id_habitacion = b.id_habitacion "
          + "JOIN tipo_habitacion t ON t.id_tipo       = h.id_tipo "
          + "JOIN usuario u         ON u.id_usuario    = b.id_usuario ";

    /**
     * Registra un bloqueo nuevo.
     *
     * @param bloqueo datos del bloqueo
     * @return el identificador asignado por la base de datos
     * @throws SQLException si falla la insercion
     */
    public int insertar(BloqueoHabitacion bloqueo) throws SQLException {
        String sql = "INSERT INTO bloqueo_habitacion "
                   + "(id_habitacion, fecha_inicio, fecha_fin, motivo, id_usuario, activo) "
                   + "VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bloqueo.getIdHabitacion());
            ps.setObject(2, bloqueo.getFechaInicio());
            ps.setObject(3, bloqueo.getFechaFin());
            ps.setString(4, bloqueo.getMotivo());
            ps.setInt(5, bloqueo.getIdUsuario());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    bloqueo.setIdBloqueo(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Levanta un bloqueo antes de su fecha de fin.
     *
     * No se borra el registro: queda como constancia de por que la
     * habitacion estuvo fuera de venta y quien lo autorizo.
     *
     * @param idBloqueo bloqueo a levantar
     * @throws SQLException si falla la actualizacion
     */
    public void levantar(int idBloqueo) throws SQLException {
        String sql = "UPDATE bloqueo_habitacion SET activo = 0 WHERE id_bloqueo = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBloqueo);
            ps.executeUpdate();
        }
    }

    /**
     * Busca un bloqueo por su identificador.
     *
     * @param idBloqueo identificador a buscar
     * @return el bloqueo encontrado, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public BloqueoHabitacion buscarPorId(int idBloqueo) throws SQLException {
        String sql = SELECT_BASE + "WHERE b.id_bloqueo = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBloqueo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Lista los bloqueos, opcionalmente solo los que siguen activos.
     *
     * @param soloActivos true para excluir los ya levantados
     * @return lista ordenada por fecha de inicio descendente
     * @throws SQLException si falla la consulta
     */
    public List<BloqueoHabitacion> listar(boolean soloActivos) throws SQLException {
        String sql = SELECT_BASE
                   + (soloActivos ? "WHERE b.activo = 1 " : "")
                   + "ORDER BY b.fecha_inicio DESC";
        List<BloqueoHabitacion> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Lista los bloqueos activos de una habitacion en particular.
     *
     * @param idHabitacion habitacion a consultar
     * @return lista ordenada por fecha de inicio
     * @throws SQLException si falla la consulta
     */
    public List<BloqueoHabitacion> listarPorHabitacion(int idHabitacion) throws SQLException {
        String sql = SELECT_BASE
                   + "WHERE b.id_habitacion = ? AND b.activo = 1 "
                   + "ORDER BY b.fecha_inicio";
        List<BloqueoHabitacion> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idHabitacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Verifica si una habitacion tiene reservas vigentes que chocarian
     * con un bloqueo que se quiere registrar.
     *
     * Se consulta antes de crear el bloqueo: no tiene sentido sacar de
     * venta una habitacion que ya le fue prometida a un huesped, porque
     * al llegar no tendria donde dormir.
     *
     * @param idHabitacion habitacion a bloquear
     * @param desde        primer dia del bloqueo
     * @param hasta        ultimo dia del bloqueo
     * @return cantidad de reservas que se cruzan con el periodo
     * @throws SQLException si falla la consulta
     */
    public int contarReservasEnConflicto(int idHabitacion, LocalDate desde, LocalDate hasta)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM reserva "
                   + " WHERE id_habitacion = ? "
                   + "   AND estado IN ('PENDIENTE','CONFIRMADA','EN_CASA') "
                   + "   AND fecha_entrada <= ? "
                   + "   AND fecha_salida  >  ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idHabitacion);
            ps.setObject(2, hasta);
            ps.setObject(3, desde);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Convierte la fila actual del ResultSet en un BloqueoHabitacion.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private BloqueoHabitacion mapear(ResultSet rs) throws SQLException {
        BloqueoHabitacion bloqueo = new BloqueoHabitacion(
                rs.getInt("id_bloqueo"),
                rs.getInt("id_habitacion"),
                rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class),
                rs.getString("motivo"),
                rs.getInt("id_usuario"),
                rs.getBoolean("activo"));

        bloqueo.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        bloqueo.setNumeroHabitacion(rs.getString("numero_habitacion"));
        bloqueo.setNombreTipo(rs.getString("nombre_tipo"));
        bloqueo.setNombreUsuario(rs.getString("nombre_usuario"));
        return bloqueo;
    }
}
