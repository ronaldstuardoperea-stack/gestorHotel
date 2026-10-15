package com.hotel.controlador;

import com.hotel.dao.UsuarioDAO;
import com.hotel.modelo.Usuario;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador de la administracion de usuarios del sistema.
 *
 * Todo el modulo es exclusivo del rol ADMINISTRADOR.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "UsuarioServlet", urlPatterns = {"/usuarios"})
public class UsuarioServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    /** Largo minimo aceptado para una contrasena. */
    private static final int LARGO_MINIMO_CLAVE = 6;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            exigirAdministrador(request);

            switch (accion) {
                case "nuevo":
                    mostrar(request, response, "usuarios/formulario.jsp");
                    break;

                case "editar":
                    int id = parametroEntero(request, "id", 0);
                    Usuario usuario = usuarioDAO.buscarPorId(id);
                    if (usuario == null) {
                        throw new ValidacionException("El usuario indicado no existe.");
                    }
                    request.setAttribute("usuarioEditado", usuario);
                    mostrar(request, response, "usuarios/formulario.jsp");
                    break;

                case "activar":
                    cambiarEstado(request, response, true);
                    break;

                case "desactivar":
                    cambiarEstado(request, response, false);
                    break;

                case "listar":
                default:
                    request.setAttribute("usuarios", usuarioDAO.listarTodos());
                    mostrar(request, response, "usuarios/lista.jsp");
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "usuarios");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "guardar");
        try {
            exigirAdministrador(request);

            if ("clave".equals(accion)) {
                cambiarClave(request, response);
            } else {
                guardar(request, response);
            }
        } catch (Exception e) {
            manejarError(request, response, e, "usuarios");
        }
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * Crea un usuario nuevo o actualiza los datos de uno existente.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "idUsuario", 0);

        String nombreUsuario = Validador.textoRequerido(
                request.getParameter("usuario"), "nombre de usuario", 40);

        if (usuarioDAO.existeNombreUsuario(nombreUsuario, id)) {
            throw new ValidacionException(
                    "El nombre de usuario '" + nombreUsuario + "' ya esta ocupado.");
        }

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreCompleto(Validador.textoRequerido(
                request.getParameter("nombreCompleto"), "nombre completo", 120));
        usuario.setUsuario(nombreUsuario);
        usuario.setRol(Validador.opcion(request.getParameter("rol"), "rol",
                Usuario.ROL_ADMINISTRADOR, Usuario.ROL_RECEPCION, Usuario.ROL_AUDITOR));
        usuario.setActivo("1".equals(request.getParameter("activo")));

        if (id == 0) {
            String clave = validarClave(request);
            usuarioDAO.insertar(usuario, clave);
            avisarExito(request, "Se creo el usuario '" + nombreUsuario + "'.");
        } else {
            // Un administrador no puede quitarse a si mismo el rol ni
            // desactivarse: si fuera el unico, nadie podria volver a
            // entrar a administrar el sistema.
            if (id == idUsuarioActual(request)) {
                if (!usuario.isAdministrador()) {
                    throw new ValidacionException(
                            "No puede quitarse a si mismo el rol de administrador.");
                }
                if (!usuario.isActivo()) {
                    throw new ValidacionException(
                            "No puede desactivar su propio usuario.");
                }
            }
            usuarioDAO.actualizar(usuario);
            avisarExito(request, "Se actualizo el usuario '" + nombreUsuario + "'.");
        }
        redirigir(request, response, "usuarios");
    }

    /**
     * Cambia la contrasena de un usuario.
     */
    private void cambiarClave(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = Validador.entero(request.getParameter("idUsuario"), "usuario");
        Usuario usuario = usuarioDAO.buscarPorId(id);

        if (usuario == null) {
            throw new ValidacionException("El usuario indicado no existe.");
        }

        String clave = validarClave(request);
        usuarioDAO.cambiarClave(id, clave);

        avisarExito(request, "Se cambio la contrasena de '" + usuario.getUsuario() + "'.");
        redirigir(request, response, "usuarios");
    }

    /**
     * Activa o desactiva un usuario.
     *
     * @param activar true para reactivarlo, false para desactivarlo
     */
    private void cambiarEstado(HttpServletRequest request, HttpServletResponse response,
                               boolean activar) throws Exception {

        int id = parametroEntero(request, "id", 0);
        Usuario usuario = usuarioDAO.buscarPorId(id);

        if (usuario == null) {
            throw new ValidacionException("El usuario indicado no existe.");
        }
        if (!activar && id == idUsuarioActual(request)) {
            throw new ValidacionException("No puede desactivar su propio usuario.");
        }

        usuarioDAO.cambiarEstado(id, activar);
        avisarExito(request, "El usuario '" + usuario.getUsuario() + "' quedo "
                + (activar ? "activo." : "desactivado."));
        redirigir(request, response, "usuarios");
    }

    /**
     * Valida que la contrasena cumpla el largo minimo y que ambas
     * casillas coincidan.
     *
     * @param request peticion en curso
     * @return la contrasena validada
     * @throws ValidacionException si es muy corta o no coinciden
     */
    private String validarClave(HttpServletRequest request) {
        String clave = Validador.textoRequerido(request.getParameter("clave"), "contrasena");
        String confirmacion = Validador.textoRequerido(
                request.getParameter("confirmacion"), "confirmacion de la contrasena");

        if (clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ValidacionException(
                    "La contrasena debe tener al menos " + LARGO_MINIMO_CLAVE
                    + " caracteres.");
        }
        if (!clave.equals(confirmacion)) {
            throw new ValidacionException("Las dos contrasenas no coinciden.");
        }
        return clave;
    }
}
