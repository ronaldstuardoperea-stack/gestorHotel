package com.hotel.controlador;

import com.hotel.dao.CargoDAO;
import com.hotel.dao.ClienteDAO;
import com.hotel.dao.FacturaDAO;
import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.OcupanteDAO;
import com.hotel.dao.PagoDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.dao.TipoHabitacionDAO;
import com.hotel.modelo.Reserva;
import com.hotel.servicio.ServicioReservas;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del modulo de reservaciones.
 *
 * Es el modulo central del sistema. Delega todas las reglas del hotel en
 * {@link ServicioReservas} y se queda unicamente con lo que le
 * corresponde a un controlador: leer el formulario, validar el formato
 * de los datos, llamar al servicio y decidir que pantalla mostrar.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "ReservaServlet", urlPatterns = {"/reservas"})
public class ReservaServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final ServicioReservas servicio = new ServicioReservas();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final HabitacionDAO habitacionDAO = new HabitacionDAO();
    private final TipoHabitacionDAO tipoDAO = new TipoHabitacionDAO();
    private final OcupanteDAO ocupanteDAO = new OcupanteDAO();
    private final CargoDAO cargoDAO = new CargoDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final FacturaDAO facturaDAO = new FacturaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            switch (accion) {
                case "nueva":
                    exigirPermisoDeOperacion(request);
                    prepararFormulario(request, response);
                    break;

                case "editar":
                    exigirPermisoDeOperacion(request);
                    prepararEdicion(request, response);
                    break;

                case "detalle":
                    mostrarDetalle(request, response);
                    break;

                case "confirmar":
                    exigirPermisoDeOperacion(request);
                    servicio.confirmar(parametroEntero(request, "id", 0));
                    avisarExito(request, "La reserva quedo confirmada.");
                    volverAlDetalle(request, response);
                    break;

                case "cancelar":
                    exigirPermisoDeOperacion(request);
                    servicio.cancelar(parametroEntero(request, "id", 0));
                    avisarExito(request, "La reserva se cancelo. Si habia anticipo, "
                            + "registre el reembolso desde el modulo de pagos.");
                    volverAlDetalle(request, response);
                    break;

                case "noshow":
                    exigirPermisoDeOperacion(request);
                    servicio.marcarNoShow(parametroEntero(request, "id", 0));
                    avisarExito(request, "La reserva se marco como no presentada.");
                    volverAlDetalle(request, response);
                    break;

                case "listar":
                default:
                    listar(request, response);
                    break;
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
            manejarError(request, response, e, "reservas");
        }
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * Muestra el buscador de reservas con sus filtros.
     */
    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String busqueda = Validador.limpiar(request.getParameter("q"));
        String estado = Validador.limpiar(request.getParameter("estado"));
        LocalDate desde = Validador.fechaOpcional(request.getParameter("desde"));
        LocalDate hasta = Validador.fechaOpcional(request.getParameter("hasta"));

        request.setAttribute("reservas", reservaDAO.buscar(busqueda, estado, desde, hasta));
        request.setAttribute("conteo", reservaDAO.contarPorEstado());

        request.setAttribute("busqueda", busqueda);
        request.setAttribute("filtroEstado", estado);
        request.setAttribute("filtroDesde", desde);
        request.setAttribute("filtroHasta", hasta);

        mostrar(request, response, "reservas/lista.jsp");
    }

    /**
     * Prepara el formulario de reserva nueva.
     *
     * Si vienen fechas en la direccion (porque el usuario llego desde la
     * consulta de disponibilidad), se muestran ya las habitaciones libres
     * en ese rango. Si no, se muestra el formulario vacio para que
     * primero elija las fechas.
     */
    private void prepararFormulario(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        LocalDate entrada = Validador.fechaOpcional(request.getParameter("entrada"));
        LocalDate salida = Validador.fechaOpcional(request.getParameter("salida"));
        int idHabitacion = parametroEntero(request, "idHabitacion", 0);

        request.setAttribute("clientes", clienteDAO.listarActivos());
        request.setAttribute("tipos", tipoDAO.listarActivos());

        if (entrada != null && salida != null && salida.isAfter(entrada)) {
            request.setAttribute("disponibles",
                    habitacionDAO.listarDisponibles(entrada, salida, 0));
            request.setAttribute("temporada", servicio.obtenerTemporada(entrada));

            if (idHabitacion > 0) {
                request.setAttribute("tarifaSugerida",
                        servicio.calcularTarifaSugerida(idHabitacion, entrada));
            }
        }

        request.setAttribute("entrada", entrada);
        request.setAttribute("salida", salida);
        request.setAttribute("idHabitacionPreseleccionada", idHabitacion);
        request.setAttribute("idClientePreseleccionado",
                parametroEntero(request, "idCliente", 0));

        mostrar(request, response, "reservas/formulario.jsp");
    }

    /**
     * Carga una reserva existente en el formulario de edicion.
     */
    private void prepararEdicion(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "id", 0);
        Reserva reserva = reservaDAO.buscarPorId(id);

        if (reserva == null) {
            throw new ValidacionException("La reserva solicitada no existe.");
        }
        if (!reserva.isCancelable()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible() + " y ya no se puede modificar.");
        }

        request.setAttribute("reserva", reserva);
        request.setAttribute("clientes", clienteDAO.listarActivos());
        request.setAttribute("tipos", tipoDAO.listarActivos());

        // Al editar se listan las habitaciones libres en el rango, mas la
        // que la reserva ya tiene asignada: de lo contrario su propia
        // habitacion no apareceria en el combo y el usuario no podria
        // guardar sin cambiarla.
        var disponibles = habitacionDAO.listarDisponibles(
                reserva.getFechaEntrada(), reserva.getFechaSalida(), 0);
        var actual = habitacionDAO.buscarPorId(reserva.getIdHabitacion());
        if (actual != null && disponibles.stream()
                .noneMatch(h -> h.getIdHabitacion() == actual.getIdHabitacion())) {
            disponibles.add(0, actual);
        }
        request.setAttribute("disponibles", disponibles);

        mostrar(request, response, "reservas/formulario.jsp");
    }

    /**
     * Muestra la ficha completa de una reserva.
     *
     * Reune en una sola pantalla todo lo que cuelga de ella: ocupantes,
     * consumos, pagos, estado de cuenta y facturas emitidas.
     */
    private void mostrarDetalle(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "id", 0);
        Reserva reserva = reservaDAO.buscarPorId(id);

        if (reserva == null) {
            throw new ValidacionException("La reserva solicitada no existe.");
        }

        request.setAttribute("reserva", reserva);
        request.setAttribute("ocupantes", ocupanteDAO.listarPorReserva(id));
        request.setAttribute("cargos", cargoDAO.listarPorReserva(id));
        request.setAttribute("pagos", pagoDAO.listarPorReserva(id));
        request.setAttribute("cuenta", reservaDAO.obtenerEstadoCuenta(id));
        request.setAttribute("facturas", facturaDAO.listarPorReserva(id));
        request.setAttribute("consumosPendientes", cargoDAO.sumarPendientes(id));

        mostrar(request, response, "reservas/detalle.jsp");
    }

    /**
     * Guarda una reserva nueva o los cambios de una existente.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "idReserva", 0);

        Reserva reserva = new Reserva();
        reserva.setIdReserva(id);
        reserva.setIdCliente(Validador.entero(request.getParameter("idCliente"), "cliente"));
        reserva.setIdHabitacion(
                Validador.entero(request.getParameter("idHabitacion"), "habitacion"));
        reserva.setFechaEntrada(
                Validador.fecha(request.getParameter("fechaEntrada"), "fecha de entrada"));
        reserva.setFechaSalida(
                Validador.fecha(request.getParameter("fechaSalida"), "fecha de salida"));
        reserva.setAdultos(
                Validador.enteroEnRango(request.getParameter("adultos"), "adultos", 1, 20));
        reserva.setNinos(
                Validador.enteroEnRango(request.getParameter("ninos"), "ninos", 0, 20));
        reserva.setTarifaNoche(
                Validador.montoPositivo(request.getParameter("tarifaNoche"), "tarifa por noche"));
        reserva.setObservaciones(
                Validador.textoOpcional(request.getParameter("observaciones"), 255));
        reserva.setIdUsuario(idUsuarioActual(request));

        if (id == 0) {
            reserva.setEstado(Reserva.ESTADO_PENDIENTE);
            int nuevoId = servicio.crearReserva(reserva);
            avisarExito(request, "Se creo la reserva " + reserva.getCodigo()
                    + ". Registre el anticipo para confirmarla.");
            redirigir(request, response, "reservas?accion=detalle&id=" + nuevoId);
        } else {
            servicio.modificarReserva(reserva);
            avisarExito(request, "Se actualizo la reserva.");
            redirigir(request, response, "reservas?accion=detalle&id=" + id);
        }
    }

    /**
     * Vuelve a la ficha de la reserva sobre la que se acaba de operar.
     */
    private void volverAlDetalle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = parametroEntero(request, "id", 0);
        redirigir(request, response, "reservas?accion=detalle&id=" + id);
    }
}
