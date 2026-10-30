package com.hotel.filtro;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Filtro que fuerza la codificacion UTF-8 en toda la aplicacion.
 *
 * Sin el, los datos que llegan de los formularios se interpretan con la
 * codificacion por omision del servidor, y los nombres con tilde o con
 * letra n llegan convertidos en simbolos raros a la base de datos.
 *
 * Tiene que ejecutarse antes de que cualquier Servlet lea un parametro:
 * una vez que se pide el primer parametro, el contenedor ya fijo la
 * codificacion y llamar a setCharacterEncoding despues ya no surte
 * efecto.
 *
 * Por eso este filtro NO usa la anotacion WebFilter: el orden entre
 * filtros anotados no esta definido por la especificacion, y aqui el
 * orden es justamente lo que importa. Se declara en web.xml, donde su
 * mapeo aparece antes que el del filtro de autenticacion y queda
 * garantizado que corre primero.
 *
 * @author Ronald Perea
 */
public class CodificacionFiltro implements Filter {

    /** Codificacion usada por la aplicacion, la base y las vistas. */
    private static final String CODIFICACION = "UTF-8";

    @Override
    public void init(FilterConfig config) throws ServletException {
        // Este filtro no necesita configuracion inicial.
    }

    @Override
    public void doFilter(ServletRequest solicitud, ServletResponse respuesta,
                         FilterChain cadena) throws IOException, ServletException {

        solicitud.setCharacterEncoding(CODIFICACION);
        respuesta.setCharacterEncoding(CODIFICACION);

        cadena.doFilter(solicitud, respuesta);
    }

    @Override
    public void destroy() {
        // No hay recursos que liberar.
    }
}
