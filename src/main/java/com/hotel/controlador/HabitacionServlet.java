package com.hotel.controlador;

import com.hotel.dao.BloqueoDAO;
import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.TipoHabitacionDAO;
import com.hotel.modelo.Habitacion;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del inventario de habitaciones.
 *
 * Atiende el listado con filtros, el alta, la edicion, el cambio rapido
 * de estado y la baja logica.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "HabitacionServlet", urlPatterns = {"/habitaciones"})
public class HabitacionServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final HabitacionDAO habitacionDAO = new HabitacionDAO();
    private final TipoHabitacionDAO tipoDAO = new TipoHabitacionDAO();
    private final BloqueoDAO bloqueoDAO = new BloqueoDAO();

    /**
     * Atiende las peticiones de consulta y las acciones sin formulario.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            switch (accion) {
                case "nuevo":
                    exigirPermisoDeOperacion(request);
                    request.setAttribute("tipos", tipoDAO.listarActivos());
                    mostrar(request, response, "habitaciones/formulario.jsp");
                    break;

                case "editar":
                    exigirPermisoDeOperacion(request);
                    mostrarFormularioEdicion(request, response);
                    break;

                case "detalle":
                    mostrarDetalle(request, response);
                    break;

                case "estado":
                    cambiarEstado(request, response);
                    break;

                case "eliminar":
                    darDeBaja(request, response);
                    break;

                case "listar":
                default:
                    listar(request, response);
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "habitaciones");
        }
    }

    /**
     * Atiende el envio del formulario de alta y edicion.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            exigirPermisoDeOperacion(request);
            guardar(request, response);
        } catch (Exception e) {
            manejarError(request, response, e, "habitaciones");
        }
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * Muestra el listado aplicando los filtros seleccionados.
     */
    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String estado = Validador.limpiar(request.getParameter("estado"));
        int idTipo = parametroEntero(request, "idTipo", 0);
        int piso = parametroEntero(request, "piso", 0);

        request.setAttribute("habitaciones",
                habitacionDAO.listarConFiltros(estado, idTipo, piso));
        request.setAttribute("conteo", habitacionDAO.contarPorEstado());
        request.setAttribute("tipos", tipoDAO.listarActivos());
        request.setAttribute("pisos", habitacionDAO.listarPisos());

        // Se devuelven los filtros a la vista para que los combos
        // conserven lo que el usuario habia seleccionado.
        request.setAttribute("filtroEstado", estado);
        request.setAttribute("filtroTipo", idTipo);
        request.setAttribute("filtroPiso", piso);

        mostrar(request, response, "habitaciones/lista.jsp");
    }

    /**
     * Carga una habitacion en el formulario de edicion.
     */
    private void mostrarFormularioEdicion(HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {
        int id = parametroEntero(request, "id", 0);
        Habitacion habitacion = habitacionDAO.buscarPorId(id);

        if (habitacion == null) {
            throw new ValidacionException("La habitacion solicitada no existe.");
        }
        request.setAttribute("habitacion", habitacion);
        request.setAttribute("tipos", tipoDAO.listarActivos());
        mostrar(request, response, "habitaciones/formulario.jsp");
    }

    /**
     * Muestra la ficha de una habitacion con sus bloqueos.
     */
    private void mostrarDetalle(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        int id = parametroEntero(request, "id", 0);
        Habitacion habitacion = habitacionDAO.buscarPorId(id);

        if (habitacion == null) {
            throw new ValidacionException("La habitacion solicitada no existe.");
        }
        request.setAttribute("habitacion", habitacion);
        request.setAttribute("bloqueos", bloqueoDAO.listarPorHabitacion(id));
        mostrar(request, response, "habitaciones/detalle.jsp");
    }

    /**
     * Guarda una habitacion nueva o los cambios de una existente.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "idHabitacion", 0);

        String numero = Validador.textoRequerido(
                request.getParameter("numero"), "numero de habitacion", 10);
        int idTipo = Validador.entero(request.getParameter("idTipo"), "tipo de habitacion");
        int piso = Validador.enteroEnRango(request.getParameter("piso"), "piso", 1, 50);
        String estado = Validador.opcion(request.getParameter("estado"), "estado",
                Habitacion.ESTADO_DISPONIBLE, Habitacion.ESTADO_OCUPADA,
                Habitacion.ESTADO_LIMPIEZA, Habitacion.ESTADO_MANTENIMIENTO,
                Habitacion.ESTADO_BLOQUEADA);
        String descripcion = Validador.textoOpcional(request.getParameter("descripcion"), 255);

        // Se verifica el numero antes de insertar para poder dar un
        // mensaje claro, en lugar de dejar que estalle la restriccion
        // UNIQUE con un error tecnico de MySQL.
        if (habitacionDAO.existeNumero(numero, id)) {
            throw new ValidacionException(
                    "Ya existe una habitacion con el numero " + numero + ".");
        }

        Habitacion habitacion = new Habitacion();
        habitacion.setIdHabitacion(id);
        habitacion.setNumero(numero);
        habitacion.setIdTipo(idTipo);
        habitacion.setPiso(piso);
        habitacion.setEstado(estado);
        habitacion.setDescripcion(descripcion);
        habitacion.setActivo(true);

        if (id == 0) {
            habitacionDAO.insertar(habitacion);
            avisarExito(request, "Se registro la habitacion " + numero + ".");
        } else {
            habitacionDAO.actualizar(habitacion);
            avisarExito(request, "Se actualizo la habitacion " + numero + ".");
        }
        redirigir(request, response, "habitaciones");
    }

    /**
     * Cambia el estado operativo de una habitacion.
     *
     * Es la accion que usa el personal de limpieza: marcar una
     * habitacion como aseada y lista para el siguiente huesped.
     */
    private void cambiarEstado(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirPermisoDeOperacion(request);

        int id = parametroEntero(request, "id", 0);
        String estado = Validador.opcion(request.getParameter("valor"), "estado",
                Habitacion.ESTADO_DISPONIBLE, Habitacion.ESTADO_OCUPADA,
                Habitacion.ESTADO_LIMPIEZA, Habitacion.ESTADO_MANTENIMIENTO,
                Habitacion.ESTADO_BLOQUEADA);

        Habitacion habitacion = habitacionDAO.buscarPorId(id);
        if (habitacion == null) {
            throw new ValidacionException("La habitacion solicitada no existe.");
        }

        // Marcar como libre una habitacion con huesped dentro provocaria
        // que recepcion se la vendiera a otra persona.
        if (Habitacion.ESTADO_OCUPADA.equals(habitacion.getEstado())
                && Habitacion.ESTADO_DISPONIBLE.equals(estado)) {
            throw new ValidacionException(
                    "La habitacion " + habitacion.getNumero() + " tiene un huesped "
                    + "hospedado. Registre primero su check-out.");
        }

        habitacionDAO.cambiarEstado(id, estado);
        avisarExito(request, "La habitacion " + habitacion.getNumero()
                + " quedo en estado " + estado.toLowerCase() + ".");
        redirigir(request, response, "habitaciones");
    }

    /**
     * Retira una habitacion del inventario activo.
     */
    private void darDeBaja(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirAdministrador(request);

        int id = parametroEntero(request, "id", 0);
        Habitacion habitacion = habitacionDAO.buscarPorId(id);

        if (habitacion == null) {
            throw new ValidacionException("La habitacion solicitada no existe.");
        }
        if (Habitacion.ESTADO_OCUPADA.equals(habitacion.getEstado())) {
            throw new ValidacionException(
                    "No se puede dar de baja la habitacion " + habitacion.getNumero()
                    + " porque tiene un huesped hospedado.");
        }

        habitacionDAO.desactivar(id);
        avisarExito(request, "Se dio de baja la habitacion " + habitacion.getNumero() + ".");
        redirigir(request, response, "habitaciones");
    }
}
