package com.hotel.controlador;

import com.hotel.dao.CargoDAO;
import com.hotel.dao.ClienteDAO;
import com.hotel.dao.FacturaDAO;
import com.hotel.dao.OcupanteDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Factura;
import com.hotel.modelo.Reserva;
import com.hotel.servicio.ServicioFacturacion;
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
 * Controlador del modulo de facturacion.
 *
 * Permite la facturacion flexible que pide el enunciado: por reserva
 * completa, solo hospedaje, solo consumos, o solo los consumos de un
 * huesped en particular.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "FacturaServlet", urlPatterns = {"/facturas"})
public class FacturaServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final ServicioFacturacion servicio = new ServicioFacturacion();
    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final OcupanteDAO ocupanteDAO = new OcupanteDAO();
    private final CargoDAO cargoDAO = new CargoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            switch (accion) {
                case "nueva":
                    prepararEmision(request, response);
                    break;

                case "ver":
                    mostrarFactura(request, response, "facturas/detalle.jsp");
                    break;

                case "imprimir":
                    mostrarFactura(request, response, "facturas/impresion.jsp");
                    break;

                case "listar":
                default:
                    listar(request, response);
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "facturas");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "emitir");
        try {
            exigirPermisoDeOperacion(request);

            if ("anular".equals(accion)) {
                anular(request, response);
            } else {
                emitir(request, response);
            }
        } catch (Exception e) {
            manejarError(request, response, e, "facturas");
        }
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * Muestra el buscador de facturas emitidas.
     */
    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String busqueda = Validador.limpiar(request.getParameter("q"));
        String estado = Validador.limpiar(request.getParameter("estado"));
        LocalDate desde = Validador.fechaOpcional(request.getParameter("desde"));
        LocalDate hasta = Validador.fechaOpcional(request.getParameter("hasta"));

        request.setAttribute("facturas", facturaDAO.buscar(busqueda, estado, desde, hasta));

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        request.setAttribute("facturadoMes",
                facturaDAO.sumarFacturado(inicioMes, LocalDate.now()));
        request.setAttribute("emitidasMes",
                facturaDAO.contarEmitidas(inicioMes, LocalDate.now()));

        request.setAttribute("busqueda", busqueda);
        request.setAttribute("filtroEstado", estado);
        request.setAttribute("filtroDesde", desde);
        request.setAttribute("filtroHasta", hasta);

        mostrar(request, response, "facturas/lista.jsp");
    }

    /**
     * Muestra el formulario de emision con todo lo que se puede facturar.
     */
    private void prepararEmision(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirPermisoDeOperacion(request);

        int idReserva = parametroEntero(request, "idReserva", 0);
        Reserva reserva = reservaDAO.buscarPorId(idReserva);

        if (reserva == null) {
            throw new ValidacionException("La reserva indicada no existe.");
        }

        request.setAttribute("reserva", reserva);
        request.setAttribute("cliente", clienteDAO.buscarPorId(reserva.getIdCliente()));
        request.setAttribute("ocupantes", ocupanteDAO.listarPorReserva(idReserva));
        request.setAttribute("pendientes", cargoDAO.listarPendientesDeFacturar(idReserva));
        request.setAttribute("totalPendiente", cargoDAO.sumarPendientes(idReserva));
        request.setAttribute("cuenta", reservaDAO.obtenerEstadoCuenta(idReserva));
        request.setAttribute("emitidas", facturaDAO.listarPorReserva(idReserva));

        mostrar(request, response, "facturas/formulario.jsp");
    }

    /**
     * Emite la factura con el tipo y las opciones elegidas.
     */
    private void emitir(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int idReserva = Validador.entero(request.getParameter("idReserva"), "reserva");
        String tipo = Validador.opcion(
                request.getParameter("tipo"), "tipo de factura", Factura.TIPOS);

        // El huesped solo se toma en cuenta cuando se factura por
        // ocupante; en los demas tipos se ignora.
        Integer idOcupante = null;
        if (Factura.TIPO_OCUPANTE.equals(tipo)) {
            int leido = parametroEntero(request, "idOcupante", 0);
            idOcupante = leido > 0 ? leido : null;
        }

        String descuentoTexto = Validador.limpiar(request.getParameter("descuento"));
        BigDecimal descuento = (descuentoTexto == null)
                ? BigDecimal.ZERO
                : Validador.montoNoNegativo(descuentoTexto, "descuento");

        String nit = Validador.textoOpcional(request.getParameter("nitReceptor"), 20);
        String nombre = Validador.textoOpcional(request.getParameter("nombreReceptor"), 150);

        int idFactura = servicio.emitirFactura(idReserva, tipo, idOcupante,
                descuento, nit, nombre, idUsuarioActual(request));

        avisarExito(request, "Factura emitida correctamente.");
        redirigir(request, response, "facturas?accion=ver&id=" + idFactura);
    }

    /**
     * Anula una factura y libera sus consumos.
     */
    private void anular(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirAdministrador(request);

        int id = Validador.entero(request.getParameter("idFactura"), "factura");
        String motivo = Validador.textoRequerido(
                request.getParameter("motivo"), "motivo de la anulacion", 150);

        servicio.anularFactura(id, motivo);

        avisarExito(request, "La factura quedo anulada y sus consumos volvieron "
                + "a quedar pendientes de facturar.");
        redirigir(request, response, "facturas?accion=ver&id=" + id);
    }

    /**
     * Carga una factura con su detalle y la muestra en la vista indicada.
     *
     * @param vista pantalla de consulta o formato para imprimir
     */
    private void mostrarFactura(HttpServletRequest request, HttpServletResponse response,
                                String vista) throws Exception {

        int id = parametroEntero(request, "id", 0);
        Factura factura = facturaDAO.buscarPorId(id);

        if (factura == null) {
            throw new ValidacionException("La factura solicitada no existe.");
        }

        request.setAttribute("factura", factura);
        request.setAttribute("cliente", clienteDAO.buscarPorId(factura.getIdCliente()));
        request.setAttribute("reserva", reservaDAO.buscarPorId(factura.getIdReserva()));

        mostrar(request, response, vista);
    }
}
