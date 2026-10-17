package com.hotel.controlador;

import com.hotel.modelo.Usuario;
import com.hotel.util.ValidacionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Clase base de la que heredan todos los Servlets del sistema.
 *
 * Reune el comportamiento que de otro modo habria que repetir en cada
 * controlador: leer el usuario de la sesion, verificar permisos, mostrar
 * una vista, guardar mensajes de exito o de error y traducir las
 * excepciones a algo que el recepcionista pueda entender.
 *
 * Es una aplicacion directa de la herencia: el codigo comun vive en la
 * clase padre y cada Servlet hijo se concentra unicamente en su modulo.
 *
 * @author Ronald Perea
 */
public abstract class ServletBase extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Carpeta donde viven las vistas JSP.
     *
     * Estan dentro de WEB-INF a proposito: el contenedor no sirve nada
     * de esa carpeta directamente al navegador. Asi nadie puede escribir
     * la direccion de un JSP y saltarse el Servlet que carga los datos y
     * verifica los permisos.
     */
    protected static final String VISTAS = "/WEB-INF/vistas/";

    /** Bitacora para dejar constancia de los errores tecnicos. */
    protected static final Logger LOG = Logger.getLogger("com.hotel");

    /**
     * Devuelve el usuario que tiene la sesion abierta.
     *
     * @param request peticion en curso
     * @return el usuario, o null si no hay sesion iniciada
     */
    protected Usuario usuarioActual(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            return null;
        }
        return (Usuario) sesion.getAttribute("usuario");
    }

    /**
     * Exige que el usuario tenga permiso para registrar movimientos.
     *
     * El rol AUDITOR solo consulta. El filtro de autenticacion ya oculta
     * los botones, pero esta verificacion es la que de verdad protege:
     * un auditor que escribiera a mano la direccion de guardar seguiria
     * sin poder modificar nada.
     *
     * @param request peticion en curso
     * @throws ValidacionException si el usuario no puede operar
     */
    protected void exigirPermisoDeOperacion(HttpServletRequest request) {
        Usuario usuario = usuarioActual(request);
        if (usuario == null || !usuario.isOperador()) {
            throw new ValidacionException(
                    "Su usuario tiene permisos de solo consulta y no puede "
                    + "realizar esta operacion.");
        }
    }

    /**
     * Exige que el usuario sea administrador.
     *
     * @param request peticion en curso
     * @throws ValidacionException si el usuario no es administrador
     */
    protected void exigirAdministrador(HttpServletRequest request) {
        Usuario usuario = usuarioActual(request);
        if (usuario == null || !usuario.isAdministrador()) {
            throw new ValidacionException(
                    "Esta operacion solo la puede realizar un administrador.");
        }
    }

    /**
     * Devuelve el identificador del usuario en sesion, para registrarlo
     * como autor de la operacion.
     *
     * @param request peticion en curso
     * @return identificador del usuario, o 0 si no hay sesion
     */
    protected int idUsuarioActual(HttpServletRequest request) {
        Usuario usuario = usuarioActual(request);
        return usuario != null ? usuario.getIdUsuario() : 0;
    }

    /**
     * Muestra una vista JSP pasandole los datos ya cargados.
     *
     * @param request  peticion en curso
     * @param response respuesta en curso
     * @param vista    nombre del archivo, por ejemplo "reservas/lista.jsp"
     * @throws ServletException si falla el procesamiento del JSP
     * @throws IOException      si falla la escritura de la respuesta
     */
    protected void mostrar(HttpServletRequest request, HttpServletResponse response,
                           String vista) throws ServletException, IOException {
        request.getRequestDispatcher(VISTAS + vista).forward(request, response);
    }

    /**
     * Guarda un mensaje de exito que se mostrara en la siguiente pagina.
     *
     * Se guarda en la sesion y no en la peticion porque despues de
     * guardar un registro se hace una redireccion, y los datos de la
     * peticion se pierden en el camino. La vista lo muestra y lo borra.
     *
     * @param request peticion en curso
     * @param mensaje texto a mostrar
     */
    protected void avisarExito(HttpServletRequest request, String mensaje) {
        request.getSession().setAttribute("mensajeExito", mensaje);
    }

    /**
     * Guarda un mensaje de error que se mostrara en la siguiente pagina.
     *
     * @param request peticion en curso
     * @param mensaje texto a mostrar
     */
    protected void avisarError(HttpServletRequest request, String mensaje) {
        request.getSession().setAttribute("mensajeError", mensaje);
    }

    /**
     * Redirige a una direccion dentro de la aplicacion.
     *
     * Despues de guardar siempre se redirige en lugar de mostrar el JSP
     * directamente. Es el patron conocido como Post-Redirect-Get: evita
     * que el usuario, al refrescar la pagina, vuelva a enviar el
     * formulario y duplique el registro que acaba de crear.
     *
     * @param request  peticion en curso
     * @param response respuesta en curso
     * @param ruta     direccion relativa, por ejemplo "reservas?accion=listar"
     * @throws IOException si falla el envio de la redireccion
     */
    protected void redirigir(HttpServletRequest request, HttpServletResponse response,
                             String ruta) throws IOException {
        response.sendRedirect(request.getContextPath() + "/" + ruta);
    }

    /**
     * Traduce una excepcion a un mensaje para el usuario.
     *
     * La distincion es importante:
     *
     *  - ValidacionException significa que la persona escribio algo que
     *    no corresponde. Su mensaje ya esta redactado para ella, asi que
     *    se muestra tal cual.
     *
     *  - Cualquier otra excepcion es una falla tecnica. Al usuario se le
     *    da un aviso generico, y el detalle completo se escribe en la
     *    bitacora del servidor. Mostrarle el texto de una SQLException no
     *    le serviria de nada y ademas revelaria la estructura interna de
     *    la base de datos.
     *
     * @param request peticion en curso
     * @param e       excepcion capturada
     * @return mensaje apto para mostrar en pantalla
     */
    protected String traducirError(HttpServletRequest request, Exception e) {
        if (e instanceof ValidacionException) {
            return e.getMessage();
        }
        LOG.log(Level.SEVERE, "Error tecnico en " + request.getRequestURI(), e);

        if (e instanceof SQLException) {
            return "No se pudo completar la operacion por un problema con la base "
                 + "de datos. Verifique que MySQL este iniciado en XAMPP e "
                 + "intentelo de nuevo.";
        }
        return "Ocurrio un error inesperado al procesar la solicitud. "
             + "Si el problema persiste, avise al administrador del sistema.";
    }

    /**
     * Atiende un error guardando el aviso y redirigiendo.
     *
     * @param request  peticion en curso
     * @param response respuesta en curso
     * @param e        excepcion capturada
     * @param destino  direccion a la que volver
     * @throws IOException si falla el envio de la redireccion
     */
    protected void manejarError(HttpServletRequest request, HttpServletResponse response,
                               Exception e, String destino) throws IOException {
        avisarError(request, traducirError(request, e));
        redirigir(request, response, destino);
    }

    /**
     * Lee un parametro entero de la peticion.
     *
     * @param request peticion en curso
     * @param nombre  nombre del parametro
     * @param porOmision valor a usar si no viene o no es un numero
     * @return el valor leido
     */
    protected int parametroEntero(HttpServletRequest request, String nombre, int porOmision) {
        String valor = request.getParameter(nombre);
        if (valor == null || valor.isBlank()) {
            return porOmision;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return porOmision;
        }
    }

    /**
     * Lee el parametro 'accion' que indica que operacion se pide.
     *
     * @param request    peticion en curso
     * @param porOmision accion a usar si no viene ninguna
     * @return nombre de la accion solicitada
     */
    protected String accion(HttpServletRequest request, String porOmision) {
        String accion = request.getParameter("accion");
        return (accion == null || accion.isBlank()) ? porOmision : accion.trim();
    }
}
