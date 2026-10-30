package com.hotel.filtro;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Filtro que exige tener la sesion iniciada para entrar al sistema.
 *
 * Un filtro se ejecuta ANTES que el Servlet al que va dirigida la
 * peticion. Eso lo convierte en el lugar correcto para revisar la
 * sesion: si la verificacion estuviera dentro de cada Servlet, bastaria
 * olvidarla en uno solo para dejar una puerta abierta.
 *
 * El filtro cubre toda la aplicacion y deja pasar sin sesion unicamente
 * la pantalla de ingreso y los archivos de estilo e imagenes.
 *
 * Se declara en web.xml, despues del filtro de codificacion, para que
 * el orden de ejecucion entre ambos quede garantizado.
 *
 * @author Ronald Perea
 */
public class AutenticacionFiltro implements Filter {

    /**
     * Direcciones que se pueden abrir sin haber iniciado sesion.
     * Cualquier otra exige usuario autenticado.
     */
    private static final String[] RUTAS_LIBRES = {
        "/login",
        "/logout",
        "/recursos/",   // hojas de estilo, scripts e imagenes propias
        "/favicon.ico"
    };

    @Override
    public void init(FilterConfig config) throws ServletException {
        // Este filtro no necesita configuracion inicial.
    }

    /**
     * Revisa cada peticion antes de dejarla continuar.
     */
    @Override
    public void doFilter(ServletRequest solicitud, ServletResponse respuesta,
                         FilterChain cadena) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) solicitud;
        HttpServletResponse response = (HttpServletResponse) respuesta;

        // getRequestURI() incluye el nombre de la aplicacion. Se le resta
        // el contexto para quedarse solo con la ruta interna, que es la
        // que se compara contra la lista de rutas libres.
        String ruta = request.getRequestURI().substring(request.getContextPath().length());

        if (esRutaLibre(ruta)) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        HttpSession sesion = request.getSession(false);
        boolean autenticado = sesion != null && sesion.getAttribute("usuario") != null;

        if (autenticado) {
            // Se le pide al navegador que no guarde estas paginas en
            // cache. De lo contrario, al cerrar sesion y pulsar el boton
            // Atras, seguirian viendose datos del usuario anterior.
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            cadena.doFilter(solicitud, respuesta);
            return;
        }

        // Sin sesion: se manda al login. Si la peticion venia de un
        // formulario que llevaba rato abierto, se explica por que.
        HttpSession nueva = request.getSession(true);
        nueva.setAttribute("mensajeError",
                "Su sesion expiro o aun no ha iniciado sesion. Ingrese nuevamente.");

        response.sendRedirect(request.getContextPath() + "/login");
    }

    @Override
    public void destroy() {
        // No hay recursos que liberar.
    }

    /**
     * Indica si una ruta se puede abrir sin haber iniciado sesion.
     *
     * La raiz de la aplicacion tambien se deja pasar, porque es la que
     * redirige al login o al tablero segun corresponda.
     *
     * @param ruta ruta interna solicitada
     * @return true si no requiere sesion
     */
    private boolean esRutaLibre(String ruta) {
        if (ruta.isEmpty() || "/".equals(ruta)) {
            return true;
        }
        for (String libre : RUTAS_LIBRES) {
            if (ruta.startsWith(libre)) {
                return true;
            }
        }
        return false;
    }
}
