package com.hotel.controlador;

import com.hotel.dao.ClienteDAO;
import com.hotel.dao.FacturaDAO;
import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.ReservaDAO;

import java.io.IOException;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del tablero principal.
 *
 * Reune en una sola pantalla lo que el personal necesita ver al llegar:
 * cuantas habitaciones hay libres, quien llega hoy, quien sale hoy,
 * cuanto se cobro y que cuentas estan pendientes.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/inicio"})
public class DashboardServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final HabitacionDAO habitacionDAO = new HabitacionDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final FacturaDAO facturaDAO = new FacturaDAO();

    /**
     * Carga todos los indicadores y muestra el tablero.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            LocalDate hoy = LocalDate.now();
            LocalDate inicioMes = hoy.withDayOfMonth(1);

            // Estado actual del inventario de habitaciones.
            request.setAttribute("habitacionesPorEstado", habitacionDAO.contarPorEstado());
            request.setAttribute("ocupacion", reservaDAO.calcularOcupacionHoy());

            // Movimiento del dia en recepcion.
            request.setAttribute("llegadasHoy", reservaDAO.listarLlegadasDeHoy());
            request.setAttribute("salidasHoy", reservaDAO.listarSalidasDeHoy());
            request.setAttribute("enCasa", reservaDAO.listarEnCasa());

            // Indicadores economicos.
            request.setAttribute("ingresosHoy", reservaDAO.sumarIngresos(hoy, hoy));
            request.setAttribute("ingresosMes", reservaDAO.sumarIngresos(inicioMes, hoy));
            request.setAttribute("facturadoMes", facturaDAO.sumarFacturado(inicioMes, hoy));
            request.setAttribute("facturasMes", facturaDAO.contarEmitidas(inicioMes, hoy));

            // Cuentas por cobrar: se muestran solo las cinco mayores,
            // porque el tablero es un resumen, no un reporte completo.
            var pendientes = reservaDAO.listarConSaldoPendiente();
            request.setAttribute("totalCuentasPendientes", pendientes.size());
            request.setAttribute("cuentasPendientes",
                    pendientes.size() > 5 ? pendientes.subList(0, 5) : pendientes);

            request.setAttribute("reservasPorEstado", reservaDAO.contarPorEstado());
            request.setAttribute("totalClientes", clienteDAO.contarActivos());

            mostrar(request, response, "dashboard.jsp");

        } catch (Exception e) {
            request.setAttribute("error", traducirError(request, e));
            mostrar(request, response, "error.jsp");
        }
    }
}
