package com.hotel.dao;

import com.hotel.modelo.Temporada;
import com.hotel.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de las temporadas tarifarias.
 *
 * @author Ronald Perea
 */
public class TemporadaDAO {

    /**
     * Inserta una temporada nueva.
     *
     * @param temporada datos del periodo
     * @return el identificador asignado por la base de datos
     * @throws SQLException si falla la insercion
     */
    public int insertar(Temporada temporada) throws SQLException {
        String sql = "INSERT INTO temporada "
                   + "(nombre, fecha_inicio, fecha_fin, factor, activo) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, temporada.getNombre());
            ps.setObject(2, temporada.getFechaInicio());
            ps.setObject(3, temporada.getFechaFin());
            ps.setBigDecimal(4, temporada.getFactor());
            ps.setBoolean(5, temporada.isActivo());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    temporada.setIdTemporada(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Actualiza una temporada existente.
     *
     * @param temporada datos a guardar, con su identificador
     * @throws SQLException si falla la actualizacion
     */
    public void actualizar(Temporada temporada) throws SQLException {
        String sql = "UPDATE temporada SET nombre = ?, fecha_inicio = ?, "
                   + "       fecha_fin = ?, factor = ?, activo = ? "
                   + "WHERE id_temporada = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, temporada.getNombre());
            ps.setObject(2, temporada.getFechaInicio());
            ps.setObject(3, temporada.getFechaFin());
            ps.setBigDecimal(4, temporada.getFactor());
            ps.setBoolean(5, temporada.isActivo());
            ps.setInt(6, temporada.getIdTemporada());
            ps.executeUpdate();
        }
    }

    /**
     * Elimina una temporada.
     *
     * A diferencia de las demas entidades, esta si se puede borrar sin
     * riesgo: ninguna reserva apunta a una temporada. La temporada solo
     * se consulta al momento de sugerir el precio, y ese precio queda
     * congelado dentro de la reserva.
     *
     * @param idTemporada temporada a eliminar
     * @throws SQLException si falla la eliminacion
     */
    public void eliminar(int idTemporada) throws SQLException {
        String sql = "DELETE FROM temporada WHERE id_temporada = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTemporada);
            ps.executeUpdate();
        }
    }

    /**
     * Busca una temporada por su identificador.
     *
     * @param idTemporada identificador a buscar
     * @return la temporada encontrada, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Temporada buscarPorId(int idTemporada) throws SQLException {
        String sql = "SELECT * FROM temporada WHERE id_temporada = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTemporada);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Busca la temporada activa que cubre una fecha determinada.
     *
     * Si dos temporadas se traslaparan por un error de captura, se toma
     * la de mayor factor. Es una decision conservadora: ante la duda, el
     * sistema sugiere el precio mas alto y deja que recepcion lo baje a
     * mano, en lugar de regalar una noche sin que nadie se entere.
     *
     * @param fecha dia a consultar
     * @return la temporada vigente, o null si esa fecha no tiene ninguna
     * @throws SQLException si falla la consulta
     */
    public Temporada buscarPorFecha(LocalDate fecha) throws SQLException {
        String sql = "SELECT * FROM temporada "
                   + " WHERE activo = 1 AND ? BETWEEN fecha_inicio AND fecha_fin "
                   + " ORDER BY factor DESC LIMIT 1";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Lista todas las temporadas registradas.
     *
     * @return lista ordenada por fecha de inicio
     * @throws SQLException si falla la consulta
     */
    public List<Temporada> listarTodas() throws SQLException {
        String sql = "SELECT * FROM temporada ORDER BY fecha_inicio";
        List<Temporada> lista = new ArrayList<>();
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
     * Convierte la fila actual del ResultSet en una Temporada.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Temporada mapear(ResultSet rs) throws SQLException {
        return new Temporada(
                rs.getInt("id_temporada"),
                rs.getString("nombre"),
                rs.getObject("fecha_inicio", LocalDate.class),
                rs.getObject("fecha_fin", LocalDate.class),
                rs.getBigDecimal("factor"),
                rs.getBoolean("activo"));
    }
}
