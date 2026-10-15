package com.hotel.controlador;

import com.hotel.dao.BloqueoDAO;
import com.hotel.dao.HabitacionDAO;
import com.hotel.modelo.BloqueoHabitacion;
import com.hotel.modelo.Habitacion;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del bloqueo de habitaciones.
 *
 * Responde al requerimiento de "bloqueo de habitaciones": retirar una
 * habitacion de la venta durante un rango de fechas.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "BloqueoServlet", urlPatterns = {"/bloqueos"})
public class BloqueoServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final BloqueoDAO bloqueoDAO = new BloqueoDAO();
    private final HabitacionDAO habitacionDAO = new HabitacionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            switch (accion) {
                case "nuevo":
                    exigirPermisoDeOperacion(request);
                    request.setAttribute("habitaciones", habitacionDAO.listarTodas());
                    request.setAttribute("idHabitacionPreseleccionada",
                            parametroEntero(request, "idHabitacion", 0));
                    mostrar(request, response, "bloqueos/formulario.jsp");
                    break;

                case "levantar":
                    levantar(request, response);
                    break;

                case "listar":
                default:
                    boolean soloActivos = !"1".equals(request.getParameter("historial"));
                    request.setAttribute("bloqueos", bloqueoDAO.listar(soloActivos));
                    request.setAttribute("soloActivos", soloActivos);
                    mostrar(request, response, "bloqueos/lista.jsp");
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "bloqueos");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            exigirPermisoDeOperacion(request);
            guardar(request, response);
        } catch (Exception e) {
            manejarError(request, response, e, "bloqueos?accion=nuevo");
        }
    }

    /**
     * Registra un bloqueo nuevo.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int idHabitacion = Validador.entero(request.getParameter("idHabitacion"), "habitacion");
        LocalDate inicio = Validador.fecha(request.getParameter("fechaInicio"), "fecha de inicio");
        LocalDate fin = Validador.fecha(request.getParameter("fechaFin"), "fecha de fin");
        String motivo = Validador.textoRequerido(
                request.getParameter("motivo"), "motivo del bloqueo", 150);

        if (fin.isBefore(inicio)) {
            throw new ValidacionException(
                    "La fecha de fin no puede ser anterior a la de inicio.");
        }

        Habitacion habitacion = habitacionDAO.buscarPorId(idHabitacion);
        if (habitacion == null) {
            throw new ValidacionException("La habitacion indicada no existe.");
        }

        // No tiene sentido sacar de venta una habitacion que ya le fue
        // prometida a un huesped: al llegar no tendria donde dormir.
        int conflictos = bloqueoDAO.contarReservasEnConflicto(idHabitacion, inicio, fin);
        if (conflictos > 0) {
            throw new ValidacionException(
                    "La habitacion " + habitacion.getNumero() + " tiene " + conflictos
                    + " reserva(s) vigente(s) entre el " + inicio + " y el " + fin
                    + ". Reubique o cancele esas reservas antes de bloquearla.");
        }

        BloqueoHabitacion bloqueo = new BloqueoHabitacion();
        bloqueo.setIdHabitacion(idHabitacion);
        bloqueo.setFechaInicio(inicio);
        bloqueo.setFechaFin(fin);
        bloqueo.setMotivo(motivo);
        bloqueo.setIdUsuario(idUsuarioActual(request));

        bloqueoDAO.insertar(bloqueo);

        // Si el bloqueo empieza hoy, la habitacion se marca de inmediato
        // para que aparezca como bloqueada en el tablero.
        if (bloqueo.isVigente()) {
            habitacionDAO.cambiarEstado(idHabitacion, Habitacion.ESTADO_BLOQUEADA);
        }

        avisarExito(request, "La habitacion " + habitacion.getNumero()
                + " quedo bloqueada del " + inicio + " al " + fin + ".");
        redirigir(request, response, "bloqueos");
    }

    /**
     * Levanta un bloqueo antes de su fecha de fin.
     */
    private void levantar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirPermisoDeOperacion(request);

        int id = parametroEntero(request, "id", 0);
        BloqueoHabitacion bloqueo = bloqueoDAO.buscarPorId(id);

        if (bloqueo == null) {
            throw new ValidacionException("El bloqueo indicado no existe.");
        }
        if (!bloqueo.isActivo()) {
            throw new ValidacionException("Ese bloqueo ya habia sido levantado.");
        }

        bloqueoDAO.levantar(id);

        // La habitacion vuelve a estar disponible, pero solo si no le
        // queda otro bloqueo encima. Con dos bloqueos traslapados,
        // levantar uno no deberia liberarla.
        Habitacion habitacion = habitacionDAO.buscarPorId(bloqueo.getIdHabitacion());
        if (habitacion != null && Habitacion.ESTADO_BLOQUEADA.equals(habitacion.getEstado())) {

            boolean sigueBloqueada = bloqueoDAO
                    .listarPorHabitacion(bloqueo.getIdHabitacion())
                    .stream()
                    .anyMatch(BloqueoHabitacion::isVigente);

            if (!sigueBloqueada) {
                habitacionDAO.cambiarEstado(
                        bloqueo.getIdHabitacion(), Habitacion.ESTADO_DISPONIBLE);
            }
        }

        avisarExito(request, "Se levanto el bloqueo de la habitacion "
                + bloqueo.getNumeroHabitacion() + ".");
        redirigir(request, response, "bloqueos");
    }
}
