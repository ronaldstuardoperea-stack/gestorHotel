package com.hotel.controlador;

import com.hotel.dao.CargoDAO;
import com.hotel.dao.ClienteDAO;
import com.hotel.dao.FacturaDAO;
import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.PagoDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador de los reportes gerenciales.
 *
 * Reune los indicadores que le interesan a la administracion del hotel:
 * ingresos por periodo, de donde vienen esos ingresos, quienes son los
 * mejores clientes y que cuentas estan por cobrar.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "ReporteServlet", urlPatterns = {"/reportes"})
public class ReporteServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final CargoDAO cargoDAO = new CargoDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final HabitacionDAO habitacionDAO = new HabitacionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            LocalDate desde = Validador.fechaOpcional(request.getParameter("desde"));
            LocalDate hasta = Validador.fechaOpcional(request.getParameter("hasta"));

            // Por omision se reporta el mes en curso.
            if (desde == null) {
                desde = LocalDate.now().withDayOfMonth(1);
            }
            if (hasta == null) {
                hasta = LocalDate.now();
            }
            if (hasta.isBefore(desde)) {
                throw new ValidacionException(
                        "La fecha final no puede ser anterior a la inicial.");
            }

            // Resumen economico del periodo.
            request.setAttribute("ingresos", reservaDAO.sumarIngresos(desde, hasta));
            request.setAttribute("facturado", facturaDAO.sumarFacturado(desde, hasta));
            request.setAttribute("facturasEmitidas", facturaDAO.contarEmitidas(desde, hasta));

            // De donde viene el dinero.
            request.setAttribute("porCategoria", cargoDAO.totalizarPorCategoria(desde, hasta));
            request.setAttribute("porMetodo", pagoDAO.totalizarPorMetodo(desde, hasta));

            // Estado del hotel y de la cartera.
            request.setAttribute("ocupacion", reservaDAO.calcularOcupacionHoy());
            request.setAttribute("habitacionesPorEstado", habitacionDAO.contarPorEstado());
            request.setAttribute("reservasPorEstado", reservaDAO.contarPorEstado());
            request.setAttribute("cuentasPendientes", reservaDAO.listarConSaldoPendiente());

            // Ranking de clientes por consumo historico.
            request.setAttribute("mejoresClientes", clienteDAO.listarHistorial());

            request.setAttribute("desde", desde);
            request.setAttribute("hasta", hasta);

            mostrar(request, response, "reportes.jsp");

        } catch (Exception e) {
            request.setAttribute("error", traducirError(request, e));
            mostrar(request, response, "error.jsp");
        }
    }
}
