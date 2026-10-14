package com.hotel.dao;

import com.hotel.modelo.Cliente;
import com.hotel.modelo.HistorialCliente;
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
 * Acceso a datos de los clientes del hotel.
 *
 * Incluye la consulta del historial economico, que se apoya en la vista
 * vw_historial_cliente de la base de datos.
 *
 * @author Ronald Perea
 */
public class ClienteDAO {

    /**
     * Inserta un cliente nuevo.
     *
     * @param cliente datos del cliente
     * @return el identificador asignado por la base de datos
     * @throws SQLException si el documento ya existe o falla la insercion
     */
    public int insertar(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO cliente "
                   + "(nombre, apellido, tipo_documento, documento, nit, telefono, "
                   + " correo, direccion, pais, notas, activo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignarParametros(ps, cliente);
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    cliente.setIdCliente(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param cliente datos a guardar, con su identificador
     * @throws SQLException si falla la actualizacion
     */
    public void actualizar(Cliente cliente) throws SQLException {
        String sql = "UPDATE cliente SET nombre = ?, apellido = ?, tipo_documento = ?, "
                   + "       documento = ?, nit = ?, telefono = ?, correo = ?, "
                   + "       direccion = ?, pais = ?, notas = ?, activo = ? "
                   + "WHERE id_cliente = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            asignarParametros(ps, cliente);
            ps.setInt(12, cliente.getIdCliente());
            ps.executeUpdate();
        }
    }

    /**
     * Da de baja a un cliente sin borrar su historial.
     *
     * Igual que con los usuarios y las habitaciones, no se elimina el
     * registro: sus reservas y facturas pasadas seguirian apuntando a el.
     *
     * @param idCliente cliente a desactivar
     * @throws SQLException si falla la actualizacion
     */
    public void desactivar(int idCliente) throws SQLException {
        String sql = "UPDATE cliente SET activo = 0 WHERE id_cliente = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
        }
    }

    /**
     * Vuelve a activar a un cliente dado de baja.
     *
     * @param idCliente cliente a reactivar
     * @throws SQLException si falla la actualizacion
     */
    public void activar(int idCliente) throws SQLException {
        String sql = "UPDATE cliente SET activo = 1 WHERE id_cliente = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
        }
    }

    /**
     * Busca un cliente por su identificador.
     *
     * @param idCliente identificador a buscar
     * @return el cliente encontrado, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Cliente buscarPorId(int idCliente) throws SQLException {
        String sql = "SELECT * FROM cliente WHERE id_cliente = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Verifica si un documento de identificacion ya esta registrado.
     *
     * @param tipoDocumento tipo de documento
     * @param documento     numero del documento
     * @param idExcluir     cliente a ignorar; se usa al editar
     * @return true si el documento pertenece a otro cliente
     * @throws SQLException si falla la consulta
     */
    public boolean existeDocumento(String tipoDocumento, String documento, int idExcluir)
            throws SQLException {
        String sql = "SELECT 1 FROM cliente "
                   + " WHERE tipo_documento = ? AND documento = ? AND id_cliente <> ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipoDocumento);
            ps.setString(2, documento);
            ps.setInt(3, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Lista los clientes activos.
     *
     * @return lista ordenada por apellido y nombre
     * @throws SQLException si falla la consulta
     */
    public List<Cliente> listarActivos() throws SQLException {
        String sql = "SELECT * FROM cliente WHERE activo = 1 ORDER BY apellido, nombre";
        List<Cliente> lista = new ArrayList<>();
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
     * Busca clientes por nombre, apellido, documento, NIT, telefono o
     * correo, todo desde un solo cuadro de busqueda.
     *
     * El texto viaja como parametro del PreparedStatement, nunca
     * concatenado a la consulta. Si alguien escribiera algo como
     * "'; DROP TABLE cliente; --", el driver lo trataria como texto a
     * buscar y no como instrucciones SQL.
     *
     * @param texto        lo que escribio el usuario; si viene vacio se
     *                     devuelven todos los clientes
     * @param incluirBajas si es true tambien trae los desactivados
     * @return lista de clientes que coinciden
     * @throws SQLException si falla la consulta
     */
    public List<Cliente> buscar(String texto, boolean incluirBajas) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM cliente WHERE 1 = 1 ");

        if (!incluirBajas) {
            sql.append("AND activo = 1 ");
        }

        boolean hayTexto = texto != null && !texto.isBlank();
        if (hayTexto) {
            sql.append("AND (nombre LIKE ? OR apellido LIKE ? "
                     + "  OR CONCAT(nombre, ' ', apellido) LIKE ? "
                     + "  OR documento LIKE ? OR nit LIKE ? "
                     + "  OR telefono LIKE ? OR correo LIKE ?) ");
        }
        sql.append("ORDER BY apellido, nombre");

        List<Cliente> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            if (hayTexto) {
                String patron = "%" + texto.trim() + "%";
                for (int i = 1; i <= 7; i++) {
                    ps.setString(i, patron);
                }
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
     * Devuelve el perfil economico acumulado de un cliente.
     *
     * @param idCliente cliente a consultar
     * @return su historial, o null si el cliente no existe
     * @throws SQLException si falla la consulta
     */
    public HistorialCliente obtenerHistorial(int idCliente) throws SQLException {
        String sql = "SELECT * FROM vw_historial_cliente WHERE id_cliente = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearHistorial(rs) : null;
            }
        }
    }

    /**
     * Lista el historial economico de todos los clientes, para el
     * reporte de mejores huespedes.
     *
     * @return lista ordenada por consumo historico descendente
     * @throws SQLException si falla la consulta
     */
    public List<HistorialCliente> listarHistorial() throws SQLException {
        String sql = "SELECT * FROM vw_historial_cliente ORDER BY consumo_historico DESC";
        List<HistorialCliente> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearHistorial(rs));
            }
        }
        return lista;
    }

    /**
     * Cuenta los clientes activos, para la tarjeta del tablero.
     *
     * @return cantidad de clientes vigentes
     * @throws SQLException si falla la consulta
     */
    public int contarActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM cliente WHERE activo = 1";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Asigna al PreparedStatement los once campos comunes al insertar y
     * al actualizar, en el mismo orden en ambas consultas.
     *
     * @param ps      sentencia preparada
     * @param cliente datos a asignar
     * @throws SQLException si falla la asignacion
     */
    private void asignarParametros(PreparedStatement ps, Cliente cliente) throws SQLException {
        ps.setString(1, cliente.getNombre());
        ps.setString(2, cliente.getApellido());
        ps.setString(3, cliente.getTipoDocumento());
        ps.setString(4, cliente.getDocumento());
        ps.setString(5, cliente.getNit());
        ps.setString(6, cliente.getTelefono());
        ps.setString(7, cliente.getCorreo());
        ps.setString(8, cliente.getDireccion());
        ps.setString(9, cliente.getPais());
        ps.setString(10, cliente.getNotas());
        ps.setBoolean(11, cliente.isActivo());
    }

    /**
     * Convierte la fila actual del ResultSet en un Cliente.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(
                rs.getInt("id_cliente"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("tipo_documento"),
                rs.getString("documento"),
                rs.getString("nit"),
                rs.getString("telefono"),
                rs.getString("correo"),
                rs.getString("direccion"),
                rs.getString("pais"),
                rs.getString("notas"),
                rs.getBoolean("activo"));
        cliente.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        return cliente;
    }

    /**
     * Convierte la fila actual del ResultSet en un HistorialCliente.
     *
     * @param rs cursor posicionado en una fila de vw_historial_cliente
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private HistorialCliente mapearHistorial(ResultSet rs) throws SQLException {
        HistorialCliente historial = new HistorialCliente();
        historial.setIdCliente(rs.getInt("id_cliente"));
        historial.setCliente(rs.getString("cliente"));
        historial.setDocumento(rs.getString("documento"));
        historial.setTelefono(rs.getString("telefono"));
        historial.setCorreo(rs.getString("correo"));
        historial.setTotalReservas(rs.getInt("total_reservas"));
        historial.setVisitasCompletadas(rs.getInt("visitas_completadas"));
        historial.setCancelaciones(rs.getInt("cancelaciones"));
        historial.setNochesTotales(rs.getInt("noches_totales"));
        historial.setConsumoHistorico(rs.getBigDecimal("consumo_historico"));
        historial.setSaldoActual(rs.getBigDecimal("saldo_actual"));
        historial.setUltimaVisita(rs.getObject("ultima_visita", LocalDate.class));
        return historial;
    }
}
