package com.hotel.controlador;

import com.hotel.dao.CargoDAO;
import com.hotel.dao.OcupanteDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Cargo;
import com.hotel.modelo.Reserva;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador de los consumos cargados a la cuenta de una reserva.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "CargoServlet", urlPatterns = {"/cargos"})
public class CargoServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final CargoDAO cargoDAO = new CargoDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final OcupanteDAO ocupanteDAO = new OcupanteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "nuevo");
        try {
            if ("eliminar".equals(accion)) {
                eliminar(request, response);
            } else {
                prepararFormulario(request, response);
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
            manejarError(request, response, e, "cargos?idReserva=" + idReserva);
        }
    }

    /**
     * Muestra el formulario para anotar un consumo.
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
        request.setAttribute("ocupantes", ocupanteDAO.listarPorReserva(idReserva));
        request.setAttribute("cargos", cargoDAO.listarPorReserva(idReserva));
        request.setAttribute("totalConsumos", cargoDAO.sumarTotal(idReserva));

        mostrar(request, response, "cargos/formulario.jsp");
    }

    /**
     * Registra un consumo nuevo.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int idReserva = Validador.entero(request.getParameter("idReserva"), "reserva");
        Reserva reserva = reservaDAO.buscarPorId(idReserva);

        if (reserva == null) {
            throw new ValidacionException("La reserva indicada no existe.");
        }

        // Solo se le pueden anotar consumos a un huesped que este dentro
        // del hotel. Cargarle algo a una reserva finalizada dejaria un
        // saldo que nadie va a cobrar, porque el cliente ya se fue.
        if (!reserva.isAdmiteConsumos()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible() + ". Solo se pueden registrar "
                    + "consumos mientras el huesped esta hospedado.");
        }

        Cargo cargo = new Cargo();
        cargo.setIdReserva(idReserva);
        cargo.setConcepto(Validador.textoRequerido(
                request.getParameter("concepto"), "concepto del consumo", 150));
        cargo.setCategoria(Validador.opcion(
                request.getParameter("categoria"), "categoria", Cargo.CATEGORIAS));

        BigDecimal cantidad = Validador.montoPositivo(
                request.getParameter("cantidad"), "cantidad");
        BigDecimal precio = Validador.montoNoNegativo(
                request.getParameter("precioUnitario"), "precio unitario");

        cargo.setCantidad(cantidad);
        cargo.setPrecioUnitario(precio);
        cargo.recalcularMonto();

        // El consumo puede quedar a nombre de un huesped concreto, para
        // poder facturarselo por separado, o a la habitacion en general.
        int idOcupante = parametroEntero(request, "idOcupante", 0);
        cargo.setIdOcupante(idOcupante > 0 ? idOcupante : null);

        cargo.setIdUsuario(idUsuarioActual(request));

        cargoDAO.insertar(cargo);
        avisarExito(request, "Se cargo " + cargo.getConcepto()
                + " por Q " + cargo.getMonto() + " a la cuenta.");
        redirigir(request, response, "cargos?idReserva=" + idReserva);
    }

    /**
     * Elimina un consumo que aun no ha sido facturado.
     */
    private void eliminar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirPermisoDeOperacion(request);

        int id = parametroEntero(request, "id", 0);
        Cargo cargo = cargoDAO.buscarPorId(id);

        if (cargo == null) {
            throw new ValidacionException("El consumo indicado no existe.");
        }

        int idReserva = cargo.getIdReserva();

        // El DAO vuelve a comprobar la condicion dentro del propio DELETE,
        // por si el cargo se facturo entre esta consulta y el borrado.
        if (!cargoDAO.eliminarSiNoFacturado(id)) {
            throw new ValidacionException(
                    "El consumo '" + cargo.getConcepto() + "' ya fue facturado "
                    + "y no se puede eliminar. Anule la factura si necesita corregirlo.");
        }

        avisarExito(request, "Se elimino el consumo '" + cargo.getConcepto() + "'.");
        redirigir(request, response, "cargos?idReserva=" + idReserva);
    }
}
