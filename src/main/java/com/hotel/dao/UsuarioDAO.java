package com.hotel.dao;

import com.hotel.modelo.Usuario;
import com.hotel.util.ConexionBD;
import com.hotel.util.Seguridad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los usuarios del sistema.
 *
 * Aqui vive la unica consulta que lee el salt y el hash de una
 * contrasena. Ningun otro DAO ni Servlet toca esos campos.
 *
 * @author Ronald Perea
 */
public class UsuarioDAO {

    /**
     * Valida las credenciales de inicio de sesion.
     *
     * El proceso es: se busca al usuario por su nombre de ingreso, se
     * recalcula el hash con el salt guardado y la clave recien escrita, y
     * se comparan los dos hashes. La contrasena en texto plano nunca se
     * compara contra la base ni se guarda en ningun lado.
     *
     * Si las credenciales no coinciden se devuelve null sin decir si
     * fallo el usuario o la contrasena. Distinguirlos le permitiria a un
     * atacante averiguar que nombres de usuario existen.
     *
     * @param nombreUsuario nombre de ingreso escrito en el formulario
     * @param clave         contrasena escrita en el formulario
     * @return el usuario autenticado sin sus datos sensibles, o null
     * @throws SQLException si falla la consulta
     */
    public Usuario autenticar(String nombreUsuario, String clave) throws SQLException {
        String sql = "SELECT id_usuario, nombre_completo, usuario, salt, clave_hash, "
                   + "       rol, activo, fecha_registro, ultimo_acceso "
                   + "FROM usuario WHERE usuario = ? AND activo = 1";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombreUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // el usuario no existe o esta desactivado
                }

                String salt = rs.getString("salt");
                String hashGuardado = rs.getString("clave_hash");

                if (!Seguridad.verificar(clave, salt, hashGuardado)) {
                    return null; // contrasena incorrecta
                }

                Usuario usuario = mapear(rs);
                // Se limpian los datos sensibles antes de devolver el
                // objeto, porque este se guardara en la sesion HTTP.
                usuario.setSalt(null);
                usuario.setClaveHash(null);
                return usuario;
            }
        }
    }

    /**
     * Deja constancia de la fecha y hora del ultimo ingreso.
     * Se llama justo despues de un inicio de sesion correcto.
     *
     * @param idUsuario usuario que acaba de entrar
     * @throws SQLException si falla la actualizacion
     */
    public void registrarAcceso(int idUsuario) throws SQLException {
        String sql = "UPDATE usuario SET ultimo_acceso = NOW() WHERE id_usuario = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    /**
     * Inserta un usuario nuevo generando su salt y su hash.
     *
     * @param usuario datos del empleado
     * @param clave   contrasena en texto plano, que no se guarda
     * @return el identificador asignado por la base de datos
     * @throws SQLException si el nombre de usuario ya existe o falla la insercion
     */
    public int insertar(Usuario usuario, String clave) throws SQLException {
        String salt = Seguridad.generarSalt();
        String hash = Seguridad.calcularHash(salt, clave);

        String sql = "INSERT INTO usuario "
                   + "(nombre_completo, usuario, salt, clave_hash, rol, activo) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombreCompleto());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, salt);
            ps.setString(4, hash);
            ps.setString(5, usuario.getRol());
            ps.setBoolean(6, usuario.isActivo());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    usuario.setIdUsuario(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Actualiza los datos de un usuario sin tocar su contrasena.
     *
     * @param usuario datos a guardar, con su identificador
     * @throws SQLException si falla la actualizacion
     */
    public void actualizar(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuario SET nombre_completo = ?, usuario = ?, "
                   + "       rol = ?, activo = ? WHERE id_usuario = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario.getNombreCompleto());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, usuario.getRol());
            ps.setBoolean(4, usuario.isActivo());
            ps.setInt(5, usuario.getIdUsuario());
            ps.executeUpdate();
        }
    }

    /**
     * Cambia la contrasena de un usuario, generando un salt nuevo.
     *
     * Renovar el salt en cada cambio es deliberado: si alguien hubiera
     * copiado el hash anterior, el nuevo no le sirve de nada.
     *
     * @param idUsuario  usuario a modificar
     * @param claveNueva contrasena nueva en texto plano
     * @throws SQLException si falla la actualizacion
     */
    public void cambiarClave(int idUsuario, String claveNueva) throws SQLException {
        String salt = Seguridad.generarSalt();
        String hash = Seguridad.calcularHash(salt, claveNueva);

        String sql = "UPDATE usuario SET salt = ?, clave_hash = ? WHERE id_usuario = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, salt);
            ps.setString(2, hash);
            ps.setInt(3, idUsuario);
            ps.executeUpdate();
        }
    }

    /**
     * Activa o desactiva un usuario.
     *
     * Los usuarios no se borran nunca: quedan como autores de reservas,
     * cargos y facturas, y eliminarlos romperia esa trazabilidad. En su
     * lugar se desactivan para que ya no puedan iniciar sesion.
     *
     * @param idUsuario usuario a modificar
     * @param activo    nuevo estado
     * @throws SQLException si falla la actualizacion
     */
    public void cambiarEstado(int idUsuario, boolean activo) throws SQLException {
        String sql = "UPDATE usuario SET activo = ? WHERE id_usuario = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    /**
     * Busca un usuario por su identificador.
     *
     * @param idUsuario identificador a buscar
     * @return el usuario encontrado, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Usuario buscarPorId(int idUsuario) throws SQLException {
        String sql = "SELECT id_usuario, nombre_completo, usuario, rol, activo, "
                   + "       fecha_registro, ultimo_acceso "
                   + "FROM usuario WHERE id_usuario = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Verifica si un nombre de ingreso ya esta tomado.
     *
     * Se consulta antes de insertar para poder mostrar un mensaje claro
     * en lugar de dejar que estalle la restriccion UNIQUE de la base.
     *
     * @param nombreUsuario nombre a verificar
     * @param idExcluir     usuario a ignorar en la busqueda; se usa al
     *                      editar, para que no choque consigo mismo
     * @return true si el nombre ya pertenece a otro usuario
     * @throws SQLException si falla la consulta
     */
    public boolean existeNombreUsuario(String nombreUsuario, int idExcluir) throws SQLException {
        String sql = "SELECT 1 FROM usuario WHERE usuario = ? AND id_usuario <> ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Lista todos los usuarios registrados, activos e inactivos.
     *
     * @return lista ordenada por nombre
     * @throws SQLException si falla la consulta
     */
    public List<Usuario> listarTodos() throws SQLException {
        String sql = "SELECT id_usuario, nombre_completo, usuario, rol, activo, "
                   + "       fecha_registro, ultimo_acceso "
                   + "FROM usuario ORDER BY nombre_completo";
        List<Usuario> lista = new ArrayList<>();
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
     * Convierte la fila actual del ResultSet en un objeto Usuario.
     *
     * Los campos salt y clave_hash solo se leen si vienen en la consulta;
     * la mayoria de los SELECT de esta clase ni siquiera los piden.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto Usuario con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombreCompleto(rs.getString("nombre_completo"));
        usuario.setUsuario(rs.getString("usuario"));
        usuario.setRol(rs.getString("rol"));
        usuario.setActivo(rs.getBoolean("activo"));
        usuario.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        usuario.setUltimoAcceso(rs.getObject("ultimo_acceso", LocalDateTime.class));

        if (tieneColumna(rs, "salt")) {
            usuario.setSalt(rs.getString("salt"));
            usuario.setClaveHash(rs.getString("clave_hash"));
        }
        return usuario;
    }

    /**
     * Indica si el ResultSet incluye una columna con ese nombre.
     * Permite reutilizar el mismo metodo mapear() para consultas que
     * traen el salt y para las que no.
     *
     * @param rs     cursor a inspeccionar
     * @param nombre nombre de columna a buscar
     * @return true si la columna forma parte del resultado
     * @throws SQLException si falla la lectura de los metadatos
     */
    private boolean tieneColumna(ResultSet rs, String nombre) throws SQLException {
        var meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (nombre.equalsIgnoreCase(meta.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
}
