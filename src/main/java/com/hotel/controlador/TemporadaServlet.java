package com.hotel.controlador;

import com.hotel.dao.TemporadaDAO;
import com.hotel.modelo.Temporada;
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
 * Controlador de las temporadas tarifarias.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "TemporadaServlet", urlPatterns = {"/temporadas"})
public class TemporadaServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    /** Limite razonable del multiplicador de temporada: de -90% a +200%. */
    private static final BigDecimal FACTOR_MINIMO = new BigDecimal("0.10");
    private static final BigDecimal FACTOR_MAXIMO = new BigDecimal("3.00");

    private final TemporadaDAO temporadaDAO = new TemporadaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            switch (accion) {
                case "nueva":
                    exigirAdministrador(request);
                    mostrar(request, response, "temporadas/formulario.jsp");
                    break;

                case "editar":
                    exigirAdministrador(request);
                    int id = parametroEntero(request, "id", 0);
                    Temporada temporada = temporadaDAO.buscarPorId(id);
                    if (temporada == null) {
                        throw new ValidacionException("La temporada indicada no existe.");
                    }
                    request.setAttribute("temporada", temporada);
                    mostrar(request, response, "temporadas/formulario.jsp");
                    break;

                case "eliminar":
                    eliminar(request, response);
                    break;

                case "listar":
                default:
                    request.setAttribute("temporadas", temporadaDAO.listarTodas());
                    request.setAttribute("vigente",
                            temporadaDAO.buscarPorFecha(LocalDate.now()));
                    mostrar(request, response, "temporadas/lista.jsp");
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "temporadas");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            exigirAdministrador(request);
            guardar(request, response);
        } catch (Exception e) {
            manejarError(request, response, e, "temporadas");
        }
    }

    /**
     * Guarda una temporada nueva o los cambios de una existente.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "idTemporada", 0);

        LocalDate inicio = Validador.fecha(request.getParameter("fechaInicio"), "fecha de inicio");
        LocalDate fin = Validador.fecha(request.getParameter("fechaFin"), "fecha de fin");

        if (fin.isBefore(inicio)) {
            throw new ValidacionException(
                    "La fecha de fin no puede ser anterior a la de inicio.");
        }

        BigDecimal factor = Validador.montoPositivo(
                request.getParameter("factor"), "factor de temporada");

        if (factor.compareTo(FACTOR_MINIMO) < 0 || factor.compareTo(FACTOR_MAXIMO) > 0) {
            throw new ValidacionException(
                    "El factor debe estar entre " + FACTOR_MINIMO + " y " + FACTOR_MAXIMO
                    + ". Recuerde que 1.00 es el precio normal, 1.40 un recargo del "
                    + "40% y 0.85 un descuento del 15%.");
        }

        Temporada temporada = new Temporada();
        temporada.setIdTemporada(id);
        temporada.setNombre(Validador.textoRequerido(
                request.getParameter("nombre"), "nombre de la temporada", 60));
        temporada.setFechaInicio(inicio);
        temporada.setFechaFin(fin);
        temporada.setFactor(factor);
        temporada.setActivo("1".equals(request.getParameter("activo")));

        if (id == 0) {
            temporadaDAO.insertar(temporada);
            avisarExito(request, "Se creo la temporada " + temporada.getNombre() + ".");
        } else {
            temporadaDAO.actualizar(temporada);
            avisarExito(request, "Se actualizo la temporada " + temporada.getNombre()
                    + ". Las reservas ya creadas conservan su tarifa original.");
        }
        redirigir(request, response, "temporadas");
    }

    /**
     * Elimina una temporada del calendario tarifario.
     */
    private void eliminar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        exigirAdministrador(request);

        int id = parametroEntero(request, "id", 0);
        Temporada temporada = temporadaDAO.buscarPorId(id);

        if (temporada == null) {
            throw new ValidacionException("La temporada indicada no existe.");
        }

        temporadaDAO.eliminar(id);
        avisarExito(request, "Se elimino la temporada " + temporada.getNombre() + ".");
        redirigir(request, response, "temporadas");
    }
}
