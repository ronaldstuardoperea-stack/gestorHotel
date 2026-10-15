package com.hotel.controlador;

import com.hotel.dao.TipoHabitacionDAO;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del catalogo de tipos de habitacion y sus tarifas base.
 *
 * Solo el administrador puede modificar este catalogo, porque de el
 * dependen los precios de todo el hotel.
 *
 * @author Ronald Perea
 */
@WebServlet(name = "TipoHabitacionServlet", urlPatterns = {"/tipos"})
public class TipoHabitacionServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final TipoHabitacionDAO tipoDAO = new TipoHabitacionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            switch (accion) {
                case "nuevo":
                    exigirAdministrador(request);
                    mostrar(request, response, "tipos/formulario.jsp");
                    break;

                case "editar":
                    exigirAdministrador(request);
                    int id = parametroEntero(request, "id", 0);
                    TipoHabitacion tipo = tipoDAO.buscarPorId(id);
                    if (tipo == null) {
                        throw new ValidacionException("El tipo de habitacion no existe.");
                    }
                    request.setAttribute("tipo", tipo);
                    mostrar(request, response, "tipos/formulario.jsp");
                    break;

                case "listar":
                default:
                    request.setAttribute("tipos", tipoDAO.listarTodos());
                    mostrar(request, response, "tipos/lista.jsp");
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "tipos");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            exigirAdministrador(request);
            guardar(request, response);
        } catch (Exception e) {
            manejarError(request, response, e, "tipos");
        }
    }

    /**
     * Guarda una categoria nueva o los cambios de una existente.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "idTipo", 0);

        TipoHabitacion tipo = new TipoHabitacion();
        tipo.setIdTipo(id);
        tipo.setNombre(Validador.textoRequerido(
                request.getParameter("nombre"), "nombre del tipo", 60));
        tipo.setDescripcion(Validador.textoOpcional(request.getParameter("descripcion"), 255));
        tipo.setCapacidadAdultos(Validador.enteroEnRango(
                request.getParameter("capacidadAdultos"), "capacidad de adultos", 1, 20));
        tipo.setCapacidadNinos(Validador.enteroEnRango(
                request.getParameter("capacidadNinos"), "capacidad de ninos", 0, 20));
        tipo.setTarifaBase(Validador.montoPositivo(
                request.getParameter("tarifaBase"), "tarifa base"));
        tipo.setActivo("1".equals(request.getParameter("activo")));

        if (id == 0) {
            tipoDAO.insertar(tipo);
            avisarExito(request, "Se creo el tipo de habitacion " + tipo.getNombre() + ".");
        } else {
            // Desactivar un tipo con habitaciones vivas las dejaria sin
            // categoria vendible, asi que se avisa en lugar de permitirlo
            // en silencio.
            if (!tipo.isActivo()) {
                int habitaciones = tipoDAO.contarHabitaciones(id);
                if (habitaciones > 0) {
                    throw new ValidacionException(
                            "No se puede desactivar el tipo " + tipo.getNombre()
                            + " porque hay " + habitaciones + " habitaciones asignadas a el. "
                            + "Reasignelas primero a otra categoria.");
                }
            }
            tipoDAO.actualizar(tipo);
            avisarExito(request, "Se actualizo el tipo " + tipo.getNombre()
                    + ". La nueva tarifa aplica solo a reservas futuras.");
        }
        redirigir(request, response, "tipos");
    }
}
