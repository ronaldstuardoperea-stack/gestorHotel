package com.hotel.controlador;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controlador del cierre de sesion.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    /**
     * Cierra la sesion y devuelve al usuario a la pantalla de ingreso.
     *
     * Se llama a invalidate() y no solo se quita el atributo del
     * usuario: eso descarta la sesion completa en el servidor, de modo
     * que no quede ningun dato del empleado anterior disponible para
     * quien use la computadora despues.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }

        // El aviso se guarda en una sesion nueva, creada despues de
        // invalidar la anterior, para que el mensaje si llegue a
        // mostrarse en la pantalla de ingreso.
        request.getSession(true).setAttribute("mensajeExito",
                "Su sesion se cerro correctamente.");

        redirigir(request, response, "login");
    }
}
