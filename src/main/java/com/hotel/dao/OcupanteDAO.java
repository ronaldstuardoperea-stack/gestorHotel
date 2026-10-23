package com.hotel.dao;

import com.hotel.modelo.Ocupante;
import com.hotel.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los ocupantes de cada reserva.
 *
 * @author Ronald Perea
 */
public class OcupanteDAO {

    /**
     * Agrega un ocupante a una reserva.
     *
     * @param ocupante datos de la persona
     * @return el identificador asignado por la base de datos
     * @throws SQLException si falla la insercion
     */
    public int insertar(Ocupante ocupante) throws SQLException {
        String sql = "INSERT INTO ocupante "
                   + "(id_reserva, nombre_completo, documento, edad, es_titular) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, ocupante.getIdReserva());
            ps.setString(2, ocupante.getNombreCompleto());
            ps.setString(3, ocupante.getDocumento());

            // La edad es opcional. Si no se captura hay que enviar NULL
            // explicitamente: setInt(0) guardaria un cero, que en la
            // tabla significaria un bebe recien nacido y no un dato
            // desconocido.
            if (ocupante.getEdad() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, ocupante.getEdad());
            }

            ps.setBoolean(5, ocupante.isEsTitular());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    ocupante.setIdOcupante(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Actualiza los datos de un ocupante.
     *
     * @param ocupante datos a guardar, con su identificador
     * @throws SQLException si falla la actualizacion
     */
    public void actualizar(Ocupante ocupante) throws SQLException {
        String sql = "UPDATE ocupante SET nombre_completo = ?, documento = ?, "
                   + "       edad = ?, es_titular = ? WHERE id_ocupante = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ocupante.getNombreCompleto());
            ps.setString(2, ocupante.getDocumento());
            if (ocupante.getEdad() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, ocupante.getEdad());
            }
            ps.setBoolean(4, ocupante.isEsTitular());
            ps.setInt(5, ocupante.getIdOcupante());
            ps.executeUpdate();
        }
    }

    /**
     * Quita un ocupante de la reserva.
     *
     * Solo se permite si no tiene consumos atribuidos; de lo contrario
     * la llave foranea de la tabla cargo lo impediria. El Servlet
     * consulta {@link #tieneCargos} antes de llamar a este metodo, para
     * poder explicarle al usuario por que no se puede borrar.
     *
     * @param idOcupante ocupante a eliminar
     * @throws SQLException si el ocupante tiene consumos asociados
     */
    public void eliminar(int idOcupante) throws SQLException {
        String sql = "DELETE FROM ocupante WHERE id_ocupante = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idOcupante);
            ps.executeUpdate();
        }
    }

    /**
     * Verifica si un ocupante tiene consumos a su nombre.
     *
     * @param idOcupante ocupante a revisar
     * @return true si tiene al menos un cargo atribuido
     * @throws SQLException si falla la consulta
     */
    public boolean tieneCargos(int idOcupante) throws SQLException {
        String sql = "SELECT 1 FROM cargo WHERE id_ocupante = ? LIMIT 1";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idOcupante);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Busca un ocupante por su identificador.
     *
     * @param idOcupante identificador a buscar
     * @return el ocupante encontrado, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Ocupante buscarPorId(int idOcupante) throws SQLException {
        String sql = "SELECT * FROM ocupante WHERE id_ocupante = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idOcupante);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Lista los ocupantes de una reserva.
     *
     * @param idReserva reserva a consultar
     * @return lista con el titular primero
     * @throws SQLException si falla la consulta
     */
    public List<Ocupante> listarPorReserva(int idReserva) throws SQLException {
        String sql = "SELECT * FROM ocupante WHERE id_reserva = ? "
                   + "ORDER BY es_titular DESC, nombre_completo";
        List<Ocupante> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Cuenta los ocupantes registrados en una reserva.
     *
     * Se compara contra la cantidad de huespedes declarada al reservar,
     * para avisar en el check-in si falta registrar a alguien.
     *
     * @param idReserva reserva a consultar
     * @return cantidad de ocupantes registrados
     * @throws SQLException si falla la consulta
     */
    public int contarPorReserva(int idReserva) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ocupante WHERE id_reserva = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Convierte la fila actual del ResultSet en un Ocupante.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Ocupante mapear(ResultSet rs) throws SQLException {
        // getInt devuelve 0 cuando la columna es NULL, asi que hay que
        // preguntarle al ResultSet si el ultimo valor leido era nulo
        // para distinguir "sin edad" de "cero anos".
        int edadLeida = rs.getInt("edad");
        Integer edad = rs.wasNull() ? null : edadLeida;

        return new Ocupante(
                rs.getInt("id_ocupante"),
                rs.getInt("id_reserva"),
                rs.getString("nombre_completo"),
                rs.getString("documento"),
                edad,
                rs.getBoolean("es_titular"));
    }
}
