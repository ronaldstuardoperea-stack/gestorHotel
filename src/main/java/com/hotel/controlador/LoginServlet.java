package com.hotel.controlador;

import com.hotel.dao.UsuarioDAO;
import com.hotel.modelo.Usuario;
import com.hotel.util.ConexionBD;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controlador del inicio de sesion.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * Muestra la pantalla de ingreso.
     *
     * Si ya hay una sesion abierta se va directo al tablero, para que
     * nadie tenga que volver a escribir su clave por haber tecleado la
     * direccion del login.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (usuarioActual(request) != null) {
            redirigir(request, response, "inicio");
            return;
        }

        // Se avisa de inmediato si MySQL no esta levantado, en lugar de
        // dejar que el usuario escriba su clave y reciba un error
        // tecnico despues de enviarla.
        if (!ConexionBD.hayConexion()) {
            request.setAttribute("error",
                    "No hay conexion con la base de datos. Inicie MySQL desde el "
                    + "panel de control de XAMPP y vuelva a cargar esta pagina.");
        }
        mostrar(request, response, "login.jsp");
    }

    /**
     * Valida las credenciales y abre la sesion.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String nombreUsuario = request.getParameter("usuario");
        String clave = request.getParameter("clave");

        if (nombreUsuario == null || nombreUsuario.isBlank()
                || clave == null || clave.isBlank()) {
            request.setAttribute("error", "Escriba su usuario y su contrasena.");
            request.setAttribute("usuarioEscrito", nombreUsuario);
            mostrar(request, response, "login.jsp");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(nombreUsuario.trim(), clave);

            if (usuario == null) {
                // Mensaje deliberadamente vago: no se dice si fallo el
                // usuario o la contrasena, para no confirmarle a nadie
                // que cierto nombre de usuario existe.
                request.setAttribute("error", "Usuario o contrasena incorrectos.");
                request.setAttribute("usuarioEscrito", nombreUsuario);
                mostrar(request, response, "login.jsp");
                return;
            }

            // Se descarta la sesion anterior y se crea una nueva. Esto
            // evita el ataque de fijacion de sesion, en el que alguien
            // induce a la victima a usar un identificador que el ya
            // conoce y queda dentro de su cuenta al iniciar ella sesion.
            HttpSession sesionPrevia = request.getSession(false);
            if (sesionPrevia != null) {
                sesionPrevia.invalidate();
            }
            HttpSession sesion = request.getSession(true);
            sesion.setAttribute("usuario", usuario);
            sesion.setMaxInactiveInterval(30 * 60); // 30 minutos

            usuarioDAO.registrarAcceso(usuario.getIdUsuario());

            avisarExito(request, "Bienvenido, " + usuario.getNombreCompleto() + ".");
            redirigir(request, response, "inicio");

        } catch (Exception e) {
            request.setAttribute("error", traducirError(request, e));
            request.setAttribute("usuarioEscrito", nombreUsuario);
            mostrar(request, response, "login.jsp");
        }
    }
}
