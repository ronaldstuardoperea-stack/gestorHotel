package com.hotel.controlador;

import com.hotel.dao.OcupanteDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Ocupante;
import com.hotel.modelo.Reserva;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del registro de ocupantes de una reserva.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "OcupanteServlet", urlPatterns = {"/ocupantes"})
public class OcupanteServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final OcupanteDAO ocupanteDAO = new OcupanteDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("eliminar".equals(accion(request, ""))) {
                eliminar(request, response);
            } else {
                // Los ocupantes siempre se ven dentro de la ficha de su
                // reserva; no tienen pantalla propia.
                redirigir(request, response, "reservas");
            }
        } catch (Exception e) {
            manejarError(request, response, e, "reservas");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            exigirPermisoDeOperacion(request);
            guardar(request, response);
        } catch (Exception e) {
            int idReserva = parametroEntero(request, "idReserva", 0);
            manejarError(request, response, e, "reservas?accion=detalle&id=" + idReserva);
        }
    }

    /**
     * Agrega un ocupante a la reserva.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int idReserva = Validador.entero(request.getParameter("idReserva"), "reserva");
        Reserva reserva = reservaDAO.buscarPorId(idReserva);

        if (reserva == null) {
            throw new ValidacionException("La reserva indicada no existe.");
        }
        if (Reserva.ESTADO_CANCELADA.equals(reserva.getEstado())
                || Reserva.ESTADO_FINALIZADA.equals(reserva.getEstado())) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta "
                    + reserva.getEstadoLegible().toLowerCase()
                    + " y ya no admite cambios en sus ocupantes.");
        }

        // No se pueden meter mas personas de las que se declararon al
        // reservar, porque excederia la capacidad de la habitacion.
        int yaRegistrados = ocupanteDAO.contarPorReserva(idReserva);
        if (yaRegistrados >= reserva.getTotalHuespedes()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " es para "
                    + reserva.getTotalHuespedes() + " huespedes y ya estan todos "
                    + "registrados. Modifique la reserva si necesita agregar mas.");
        }

        Ocupante ocupante = new Ocupante();
        ocupante.setIdReserva(idReserva);
        ocupante.setNombreCompleto(Validador.textoRequerido(
                request.getParameter("nombreCompleto"), "nombre del ocupante", 150));
        ocupante.setDocumento(Validador.textoOpcional(request.getParameter("documento"), 25));

        String edadTexto = Validador.limpiar(request.getParameter("edad"));
        if (edadTexto != null) {
            ocupante.setEdad(Validador.enteroEnRango(edadTexto, "edad", 0, 120));
        }

        // El primer ocupante que se registra queda como titular, que es
        // quien firma la tarjeta de registro.
        ocupante.setEsTitular(yaRegistrados == 0
                || "1".equals(request.getParameter("esTitular")));

        ocupanteDAO.insertar(ocupante);
        avisarExito(request, "Se registro a " + ocupante.getNombreCompleto()
                + " como ocupante.");
        redirigir(request, response, "reservas?accion=detalle&id=" + idReserva);
    }

    /**
     * Quita un ocupante de la reserva.
     */
    private void eliminar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirPermisoDeOperacion(request);

        int id = parametroEntero(request, "id", 0);
        Ocupante ocupante = ocupanteDAO.buscarPorId(id);

        if (ocupante == null) {
            throw new ValidacionException("El ocupante indicado no existe.");
        }

        // Se consulta primero para poder explicar el motivo. Si se
        // intentara borrar directamente, la llave foranea lo impediria
        // con un error tecnico que no le dice nada al usuario.
        if (ocupanteDAO.tieneCargos(id)) {
            throw new ValidacionException(
                    "No se puede quitar a " + ocupante.getNombreCompleto()
                    + " porque tiene consumos registrados a su nombre.");
        }

        int idReserva = ocupante.getIdReserva();
        ocupanteDAO.eliminar(id);
        avisarExito(request, "Se quito a " + ocupante.getNombreCompleto()
                + " de la lista de ocupantes.");
        redirigir(request, response, "reservas?accion=detalle&id=" + idReserva);
    }
}
