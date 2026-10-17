package com.hotel.controlador;

import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.TipoHabitacionDAO;
import com.hotel.servicio.ServicioReservas;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador de la consulta de disponibilidad.
 *
 * Responde a la pregunta que mas veces se hace en la recepcion de un
 * hotel: que habitaciones tengo libres entre tal y tal fecha.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "DisponibilidadServlet", urlPatterns = {"/disponibilidad"})
public class DisponibilidadServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final HabitacionDAO habitacionDAO = new HabitacionDAO();
    private final TipoHabitacionDAO tipoDAO = new TipoHabitacionDAO();
    private final ServicioReservas servicio = new ServicioReservas();

    /**
     * Muestra el buscador y, si ya hay fechas, el resultado.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            LocalDate entrada = Validador.fechaOpcional(request.getParameter("entrada"));
            LocalDate salida = Validador.fechaOpcional(request.getParameter("salida"));
            int idTipo = parametroEntero(request, "idTipo", 0);

            request.setAttribute("tipos", tipoDAO.listarActivos());
            request.setAttribute("filtroTipo", idTipo);

            // La primera vez que se entra no hay fechas: se proponen hoy
            // y manana, que es la consulta mas frecuente.
            if (entrada == null && salida == null) {
                entrada = LocalDate.now();
                salida = entrada.plusDays(1);
            }

            if (entrada != null && salida != null) {
                if (!salida.isAfter(entrada)) {
                    throw new ValidacionException(
                            "La fecha de salida debe ser posterior a la de entrada.");
                }

                request.setAttribute("disponibles",
                        habitacionDAO.listarDisponibles(entrada, salida, idTipo));
                request.setAttribute("noches", ChronoUnit.DAYS.between(entrada, salida));
                request.setAttribute("temporada", servicio.obtenerTemporada(entrada));
            }

            request.setAttribute("entrada", entrada);
            request.setAttribute("salida", salida);

            mostrar(request, response, "disponibilidad.jsp");

        } catch (Exception e) {
            request.setAttribute("error", traducirError(request, e));
            try {
                request.setAttribute("tipos", tipoDAO.listarActivos());
            } catch (Exception ignorada) {
                // Si tampoco se puede leer el catalogo, la vista se
                // muestra sin combos y solo con el mensaje de error.
            }
            mostrar(request, response, "disponibilidad.jsp");
        }
    }
}
