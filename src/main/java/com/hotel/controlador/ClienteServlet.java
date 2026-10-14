package com.hotel.controlador;

import com.hotel.dao.ClienteDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Cliente;
import com.hotel.util.ValidacionException;
import com.hotel.util.Validador;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controlador del modulo de clientes.
 *
 * Incluye la ficha de historial, que responde al requerimiento de
 * "historial de clientes con datos economicos, visitas y temporadas".
 *
 * @author Ronald Perea
 */
@WebServlet(name = "ClienteServlet", urlPatterns = {"/clientes"})
public class ClienteServlet extends ServletBase {

    private static final long serialVersionUID = 1L;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = accion(request, "listar");
        try {
            switch (accion) {
                case "nuevo":
                    exigirPermisoDeOperacion(request);
                    mostrar(request, response, "clientes/formulario.jsp");
                    break;

                case "editar":
                    exigirPermisoDeOperacion(request);
                    mostrarFormularioEdicion(request, response);
                    break;

                case "historial":
                    mostrarHistorial(request, response);
                    break;

                case "baja":
                    cambiarVigencia(request, response, false);
                    break;

                case "alta":
                    cambiarVigencia(request, response, true);
                    break;

                case "listar":
                default:
                    listar(request, response);
                    break;
            }
        } catch (Exception e) {
            manejarError(request, response, e, "clientes");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            exigirPermisoDeOperacion(request);
            guardar(request, response);
        } catch (Exception e) {
            manejarError(request, response, e, "clientes");
        }
    }

    // -----------------------------------------------------------------
    // Acciones
    // -----------------------------------------------------------------

    /**
     * Muestra el listado con el buscador.
     */
    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String busqueda = Validador.limpiar(request.getParameter("q"));
        boolean incluirBajas = "1".equals(request.getParameter("bajas"));

        request.setAttribute("clientes", clienteDAO.buscar(busqueda, incluirBajas));
        request.setAttribute("busqueda", busqueda);
        request.setAttribute("incluirBajas", incluirBajas);

        mostrar(request, response, "clientes/lista.jsp");
    }

    /**
     * Carga un cliente en el formulario de edicion.
     */
    private void mostrarFormularioEdicion(HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {
        int id = parametroEntero(request, "id", 0);
        Cliente cliente = clienteDAO.buscarPorId(id);

        if (cliente == null) {
            throw new ValidacionException("El cliente solicitado no existe.");
        }
        request.setAttribute("cliente", cliente);
        mostrar(request, response, "clientes/formulario.jsp");
    }

    /**
     * Muestra la ficha completa del cliente: sus datos, su perfil
     * economico acumulado y todas sus reservas.
     */
    private void mostrarHistorial(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        int id = parametroEntero(request, "id", 0);
        Cliente cliente = clienteDAO.buscarPorId(id);

        if (cliente == null) {
            throw new ValidacionException("El cliente solicitado no existe.");
        }
        request.setAttribute("cliente", cliente);
        request.setAttribute("historial", clienteDAO.obtenerHistorial(id));
        request.setAttribute("reservas", reservaDAO.listarPorCliente(id));

        mostrar(request, response, "clientes/historial.jsp");
    }

    /**
     * Guarda un cliente nuevo o los cambios de uno existente.
     */
    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        int id = parametroEntero(request, "idCliente", 0);

        String nombre = Validador.textoRequerido(request.getParameter("nombre"), "nombre", 80);
        String apellido = Validador.textoRequerido(
                request.getParameter("apellido"), "apellido", 80);
        String tipoDocumento = Validador.opcion(
                request.getParameter("tipoDocumento"), "tipo de documento",
                Cliente.DOC_DPI, Cliente.DOC_PASAPORTE, Cliente.DOC_NIT, Cliente.DOC_OTRO);
        String documento = Validador.textoRequerido(
                request.getParameter("documento"), "numero de documento", 25);

        if (clienteDAO.existeDocumento(tipoDocumento, documento, id)) {
            throw new ValidacionException(
                    "Ya hay un cliente registrado con el " + tipoDocumento
                    + " numero " + documento + ".");
        }

        Cliente cliente = new Cliente();
        cliente.setIdCliente(id);
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTipoDocumento(tipoDocumento);
        cliente.setDocumento(documento);

        String nit = Validador.textoOpcional(request.getParameter("nit"), 20);
        cliente.setNit(nit == null ? "C/F" : nit);

        cliente.setTelefono(Validador.textoOpcional(request.getParameter("telefono"), 25));
        cliente.setCorreo(Validador.correoOpcional(request.getParameter("correo")));
        cliente.setDireccion(Validador.textoOpcional(request.getParameter("direccion"), 180));

        String pais = Validador.textoOpcional(request.getParameter("pais"), 60);
        cliente.setPais(pais == null ? "Guatemala" : pais);

        cliente.setNotas(Validador.textoOpcional(request.getParameter("notas"), 255));
        cliente.setActivo(true);

        if (id == 0) {
            clienteDAO.insertar(cliente);
            avisarExito(request, "Se registro al cliente " + cliente.getNombreCompleto() + ".");
        } else {
            clienteDAO.actualizar(cliente);
            avisarExito(request, "Se actualizaron los datos de "
                    + cliente.getNombreCompleto() + ".");
        }
        redirigir(request, response, "clientes");
    }

    /**
     * Da de baja o vuelve a activar a un cliente.
     *
     * @param activar true para reactivarlo, false para darlo de baja
     */
    private void cambiarVigencia(HttpServletRequest request, HttpServletResponse response,
                                 boolean activar) throws Exception {
        exigirPermisoDeOperacion(request);

        int id = parametroEntero(request, "id", 0);
        Cliente cliente = clienteDAO.buscarPorId(id);

        if (cliente == null) {
            throw new ValidacionException("El cliente solicitado no existe.");
        }

        if (activar) {
            clienteDAO.activar(id);
            avisarExito(request, "Se reactivo a " + cliente.getNombreCompleto() + ".");
        } else {
            clienteDAO.desactivar(id);
            avisarExito(request, "Se dio de baja a " + cliente.getNombreCompleto()
                    + ". Su historial se conserva.");
        }
        redirigir(request, response, "clientes");
    }
}
