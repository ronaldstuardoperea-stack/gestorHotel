package com.hotel.dao;

import com.hotel.modelo.TipoHabitacion;
import com.hotel.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos del catalogo de tipos de habitacion.
 *
 * @author Ronald Perea
 */
public class TipoHabitacionDAO {

    /**
     * Inserta una categoria nueva.
     *
     * @param tipo datos de la categoria
     * @return el identificador asignado por la base de datos
     * @throws SQLException si el nombre ya existe o falla la insercion
     */
    public int insertar(TipoHabitacion tipo) throws SQLException {
        String sql = "INSERT INTO tipo_habitacion "
                   + "(nombre, descripcion, capacidad_adultos, capacidad_ninos, "
                   + " tarifa_base, activo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setInt(3, tipo.getCapacidadAdultos());
            ps.setInt(4, tipo.getCapacidadNinos());
            ps.setBigDecimal(5, tipo.getTarifaBase());
            ps.setBoolean(6, tipo.isActivo());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    tipo.setIdTipo(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Actualiza una categoria existente.
     *
     * Cambiar la tarifa base afecta solo a las reservas futuras: las ya
     * creadas conservan el precio que se congelo en su momento.
     *
     * @param tipo datos a guardar, con su identificador
     * @throws SQLException si falla la actualizacion
     */
    public void actualizar(TipoHabitacion tipo) throws SQLException {
        String sql = "UPDATE tipo_habitacion SET nombre = ?, descripcion = ?, "
                   + "       capacidad_adultos = ?, capacidad_ninos = ?, "
                   + "       tarifa_base = ?, activo = ? WHERE id_tipo = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setInt(3, tipo.getCapacidadAdultos());
            ps.setInt(4, tipo.getCapacidadNinos());
            ps.setBigDecimal(5, tipo.getTarifaBase());
            ps.setBoolean(6, tipo.isActivo());
            ps.setInt(7, tipo.getIdTipo());
            ps.executeUpdate();
        }
    }

    /**
     * Busca una categoria por su identificador.
     *
     * @param idTipo identificador a buscar
     * @return la categoria encontrada, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public TipoHabitacion buscarPorId(int idTipo) throws SQLException {
        String sql = "SELECT * FROM tipo_habitacion WHERE id_tipo = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Lista todas las categorias, incluidas las desactivadas, con la
     * cantidad de habitaciones que tiene cada una.
     *
     * @return lista ordenada por tarifa ascendente
     * @throws SQLException si falla la consulta
     */
    public List<TipoHabitacion> listarTodos() throws SQLException {
        String sql = "SELECT t.*, "
                   + "       (SELECT COUNT(*) FROM habitacion h "
                   + "         WHERE h.id_tipo = t.id_tipo AND h.activo = 1) AS total_habitaciones "
                   + "FROM tipo_habitacion t ORDER BY t.tarifa_base";
        List<TipoHabitacion> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TipoHabitacion tipo = mapear(rs);
                tipo.setTotalHabitaciones(rs.getInt("total_habitaciones"));
                lista.add(tipo);
            }
        }
        return lista;
    }

    /**
     * Lista unicamente las categorias que se siguen ofreciendo.
     * Es la que alimenta los combos de los formularios de reserva.
     *
     * @return lista ordenada por tarifa ascendente
     * @throws SQLException si falla la consulta
     */
    public List<TipoHabitacion> listarActivos() throws SQLException {
        String sql = "SELECT * FROM tipo_habitacion WHERE activo = 1 ORDER BY tarifa_base";
        List<TipoHabitacion> lista = new ArrayList<>();
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
     * Cuenta cuantas habitaciones dependen de una categoria.
     *
     * Se consulta antes de desactivarla, para advertirle al
     * administrador cuantas habitaciones quedarian sin categoria activa.
     *
     * @param idTipo categoria a revisar
     * @return cantidad de habitaciones asociadas
     * @throws SQLException si falla la consulta
     */
    public int contarHabitaciones(int idTipo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM habitacion WHERE id_tipo = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Convierte la fila actual del ResultSet en un TipoHabitacion.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private TipoHabitacion mapear(ResultSet rs) throws SQLException {
        return new TipoHabitacion(
                rs.getInt("id_tipo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getInt("capacidad_adultos"),
                rs.getInt("capacidad_ninos"),
                rs.getBigDecimal("tarifa_base"),
                rs.getBoolean("activo"));
    }
}
