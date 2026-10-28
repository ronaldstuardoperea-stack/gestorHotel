package com.hotel.controlador;

import com.hotel.dao.PagoDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Pago;
import com.hotel.modelo.Reserva;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador de pagos, anticipos y reembolsos.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "PagoServlet", urlPatterns = {"/pagos"})
public class PagoServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final PagoDAO pagoDAO = new PagoDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "caja");
        try {
            switch (accion) {
                case "nuevo":
                    prepararFormulario(request, response);
                    break;

                case "eliminar":
                    eliminar(request, response);
                    break;

                case "caja":
                default:
                    mostrarCaja(request, response);
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "pagos");
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
            manejarError(request, response, e, "pagos?accion=nuevo&idReserva=" + idReserva);
        }
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * Muestra el corte de caja de un periodo.
     */
    private void mostrarCaja(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        LocalDate desde = Validador.fechaOpcional(request.getParameter("desde"));
        LocalDate hasta = Validador.fechaOpcional(request.getParameter("hasta"));

        // Por omision se muestra el movimiento del dia, que es lo que se
        // consulta al cerrar el turno.
        if (desde == null) {
            desde = LocalDate.now();
        }
        if (hasta == null) {
            hasta = LocalDate.now();
        }
        if (hasta.isBefore(desde)) {
            throw new ValidacionException(
                    "La fecha final no puede ser anterior a la inicial.");
        }

        request.setAttribute("pagos", pagoDAO.listarPorFechas(desde, hasta));
        request.setAttribute("porMetodo", pagoDAO.totalizarPorMetodo(desde, hasta));
        request.setAttribute("totalPeriodo", reservaDAO.sumarIngresos(desde, hasta));
        request.setAttribute("cuentasPendientes", reservaDAO.listarConSaldoPendiente());
        request.setAttribute("desde", desde);
        request.setAttribute("hasta", hasta);

        mostrar(request, response, "pagos/caja.jsp");
    }

    /**
     * Muestra el formulario para registrar un movimiento de dinero.
     */
    private void prepararFormulario(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirPermisoDeOperacion(request);

        int idReserva = parametroEntero(request, "idReserva", 0);
        Reserva reserva = reservaDAO.buscarPorId(idReserva);

        if (reserva == null) {
            throw new ValidacionException("La reserva indicada no existe.");
        }

        request.setAttribute("reserva", reserva);
        request.setAttribute("cuenta", reservaDAO.obtenerEstadoCuenta(idReserva));
        request.setAttribute("pagos", pagoDAO.listarPorReserva(idReserva));

        mostrar(request, response, "pagos/formulario.jsp");
    }

    /**
     * Registra un pago, anticipo o reembolso.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int idReserva = Validador.entero(request.getParameter("idReserva"), "reserva");
        Reserva reserva = reservaDAO.buscarPorId(idReserva);

        if (reserva == null) {
            throw new ValidacionException("La reserva indicada no existe.");
        }

        Pago pago = new Pago();
        pago.setIdReserva(idReserva);
        pago.setTipo(Validador.opcion(request.getParameter("tipo"), "tipo de pago", Pago.TIPOS));
        pago.setMetodo(Validador.opcion(
                request.getParameter("metodo"), "metodo de pago", Pago.METODOS));
        pago.setMonto(Validador.montoPositivo(request.getParameter("monto"), "monto"));
        pago.setReferencia(Validador.textoOpcional(request.getParameter("referencia"), 80));
        pago.setIdUsuario(idUsuarioActual(request));

        // Un reembolso no puede superar lo que el cliente realmente
        // entrego: se estaria devolviendo dinero que nunca ingreso.
        if (Pago.TIPO_REEMBOLSO.equals(pago.getTipo())) {
            BigDecimal pagadoHastaAhora = pagoDAO.sumarPorReserva(idReserva);
            if (pago.getMonto().compareTo(pagadoHastaAhora) > 0) {
                throw new ValidacionException(
                        "No se puede reembolsar Q " + pago.getMonto()
                        + " porque el cliente solo ha pagado Q " + pagadoHastaAhora
                        + " en esta reserva.");
            }
        }

        pagoDAO.insertar(pago);

        // Un anticipo sobre una reserva pendiente la confirma de forma
        // automatica: es justamente lo que el anticipo garantiza.
        if (Pago.TIPO_ANTICIPO.equals(pago.getTipo())
                && Reserva.ESTADO_PENDIENTE.equals(reserva.getEstado())) {
            reservaDAO.cambiarEstado(idReserva, Reserva.ESTADO_CONFIRMADA);
            avisarExito(request, "Se registro el anticipo de Q " + pago.getMonto()
                    + " y la reserva " + reserva.getCodigo() + " quedo confirmada.");
        } else {
            avisarExito(request, "Se registro el movimiento de Q " + pago.getMonto() + ".");
        }

        redirigir(request, response, "reservas?accion=detalle&id=" + idReserva);
    }

    /**
     * Elimina un movimiento registrado por error.
     */
    private void eliminar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirAdministrador(request);

        int id = parametroEntero(request, "id", 0);
        Pago pago = pagoDAO.buscarPorId(id);

        if (pago == null) {
            throw new ValidacionException("El movimiento indicado no existe.");
        }

        int idReserva = pago.getIdReserva();
        pagoDAO.eliminar(id);

        avisarExito(request, "Se elimino el movimiento de Q " + pago.getMonto() + ".");
        redirigir(request, response, "reservas?accion=detalle&id=" + idReserva);
    }
}
