package com.hotel.controlador;

import com.hotel.dao.CargoDAO;
import com.hotel.dao.OcupanteDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Reserva;
import com.hotel.servicio.ServicioReservas;
import com.hotel.util.ValidacionException;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del panel de recepcion: entradas y salidas del dia.
 *
 * Es la pantalla que el recepcionista tiene abierta todo el turno.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "RecepcionServlet", urlPatterns = {"/recepcion"})
public class RecepcionServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final ServicioReservas servicio = new ServicioReservas();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final OcupanteDAO ocupanteDAO = new OcupanteDAO();
    private final CargoDAO cargoDAO = new CargoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "panel");
        try {
            switch (accion) {
                case "checkin":
                    prepararCheckin(request, response);
                    break;

                case "checkout":
                    prepararCheckout(request, response);
                    break;

                case "panel":
                default:
                    mostrarPanel(request, response);
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "recepcion");
        }
    }

    /**
     * Ejecuta el check-in o el check-out confirmado por el usuario.
     *
     * Estas dos operaciones van por POST y no por GET a proposito:
     * cambian el estado del hotel, y un GET podria dispararse solo si el
     * navegador precarga la direccion o el usuario refresca la pagina.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "");
        try {
            exigirPermisoDeOperacion(request);
            int idReserva = parametroEntero(request, "idReserva", 0);

            if ("checkin".equals(accion)) {
                servicio.registrarCheckin(idReserva);
                Reserva reserva = reservaDAO.buscarPorId(idReserva);
                avisarExito(request, "Check-in registrado. La habitacion "
                        + reserva.getNumeroHabitacion() + " quedo ocupada.");
                redirigir(request, response, "reservas?accion=detalle&id=" + idReserva);

            } else if ("checkout".equals(accion)) {
                servicio.registrarCheckout(idReserva);
                Reserva reserva = reservaDAO.buscarPorId(idReserva);
                avisarExito(request, "Check-out registrado. La habitacion "
                        + reserva.getNumeroHabitacion() + " paso a limpieza.");
                redirigir(request, response, "reservas?accion=detalle&id=" + idReserva);

            } else {
                throw new ValidacionException("Accion no reconocida.");
            }

        } catch (Exception e) {
            manejarError(request, response, e, "recepcion");
        }
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * Muestra las llegadas y salidas previstas para hoy.
     */
    private void mostrarPanel(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        request.setAttribute("llegadas", reservaDAO.listarLlegadasDeHoy());
        request.setAttribute("salidas", reservaDAO.listarSalidasDeHoy());
        request.setAttribute("enCasa", reservaDAO.listarEnCasa());
        request.setAttribute("ocupacion", reservaDAO.calcularOcupacionHoy());

        mostrar(request, response, "recepcion/panel.jsp");
    }

    /**
     * Muestra la pantalla de confirmacion del check-in.
     *
     * Antes de dar entrada se le muestra al recepcionista el resumen de
     * la reserva y se le avisa si faltan ocupantes por registrar.
     */
    private void prepararCheckin(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        Reserva reserva = obtenerReserva(request);

        if (!reserva.isCheckinHabilitado()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible() + " y no admite check-in.");
        }

        request.setAttribute("reserva", reserva);
        request.setAttribute("ocupantes", ocupanteDAO.listarPorReserva(reserva.getIdReserva()));
        request.setAttribute("faltanOcupantes", servicio.faltanOcupantes(reserva));
        request.setAttribute("cuenta", reservaDAO.obtenerEstadoCuenta(reserva.getIdReserva()));

        mostrar(request, response, "recepcion/checkin.jsp");
    }

    /**
     * Muestra la pantalla de confirmacion del check-out.
     *
     * Aqui es donde el recepcionista ve el saldo final: hospedaje mas
     * consumos menos lo ya pagado. Si queda saldo, la pantalla lo
     * advierte antes de dejar salir al huesped.
     */
    private void prepararCheckout(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        Reserva reserva = obtenerReserva(request);

        if (!reserva.isCheckoutHabilitado()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible()
                    + ". Solo se puede dar salida a un huesped que este en casa.");
        }

        request.setAttribute("reserva", reserva);
        request.setAttribute("cuenta", reservaDAO.obtenerEstadoCuenta(reserva.getIdReserva()));
        request.setAttribute("cargos", cargoDAO.listarPorReserva(reserva.getIdReserva()));
        request.setAttribute("consumosPendientes",
                cargoDAO.sumarPendientes(reserva.getIdReserva()));

        mostrar(request, response, "recepcion/checkout.jsp");
    }

    /**
     * Lee el identificador de la reserva y la recupera, fallando con un
     * mensaje claro si no existe.
     *
     * @param request peticion en curso
     * @return la reserva solicitada
     * @throws Exception si no existe o falla la consulta
     */
    private Reserva obtenerReserva(HttpServletRequest request) throws Exception {
        int id = parametroEntero(request, "id", 0);
        Reserva reserva = reservaDAO.buscarPorId(id);
        if (reserva == null) {
            throw new ValidacionException("La reserva solicitada no existe.");
        }
        return reserva;
    }
}
