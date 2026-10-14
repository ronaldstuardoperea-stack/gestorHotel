<%--
    clientes/lista.jsp

    Directorio de clientes con buscador. El cuadro de busqueda consulta
    a la vez nombre, documento, NIT, telefono y correo, para que el
    recepcionista no tenga que elegir por cual campo buscar.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Cliente" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Clientes" />
    <jsp:param name="subtitulo" value="Directorio de huespedes del hotel" />
    <jsp:param name="activo" value="clientes" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    List<Cliente> clientes = (List<Cliente>) request.getAttribute("clientes");
    if (clientes == null) { clientes = new ArrayList<Cliente>(); }

    String busqueda = (String) request.getAttribute("busqueda");
    boolean hayBusqueda = (busqueda != null && !busqueda.isEmpty());

    Boolean banderaBajas = (Boolean) request.getAttribute("incluirBajas");
    boolean incluirBajas = (banderaBajas != null && banderaBajas.booleanValue());
%>

<div class="card mb-3">
    <div class="card-body py-3">
        <form method="get" action="<%= ctx %>/clientes" class="row g-2 align-items-center">

            <div class="col-md-6">
                <div class="input-group input-group-sm">
                    <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
                    <input type="text" class="form-control" name="q"
                           value="<%= Vista.esc(busqueda) %>"
                           placeholder="Buscar por nombre, documento, NIT, telefono o correo...">
                    <button type="submit" class="btn btn-hotel">Buscar</button>
                </div>
            </div>

            <div class="col-md-3">
                <div class="form-check">
                    <input class="form-check-input" type="checkbox" id="bajas"
                           name="bajas" value="1"<%= Vista.marcado(incluirBajas) %>
                           onchange="this.form.submit()">
                    <label class="form-check-label" for="bajas" style="font-size:.82rem;">
                        Incluir clientes dados de baja
                    </label>
                </div>
            </div>

            <div class="col-md-3 text-end">
<%
    if (puedeOperar) {
%>
                <a href="<%= ctx %>/clientes?accion=nuevo" class="btn btn-sm btn-oro">
                    <i class="bi bi-person-plus me-1"></i> Nuevo cliente
                </a>
<%
    }
%>
            </div>
        </form>
    </div>
</div>

<div class="card">
<%
    if (clientes.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-person-x fs-1 opacity-25 d-block mb-2"></i>
<%
        if (hayBusqueda) {
%>
        No se encontraron clientes que coincidan con
        "<%= Vista.esc(busqueda) %>".
<%
        } else {
%>
        Aun no hay clientes registrados.
<%
        }
%>
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Cliente</th>
                    <th>Documento</th>
                    <th>Contacto</th>
                    <th>Procedencia</th>
                    <th class="text-end">Acciones</th>
                </tr>
            </thead>
            <tbody>
<%
        for (Cliente cl : clientes) {
%>
                <tr class="<%= cl.isActivo() ? "" : "opacity-50" %>">

                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <span class="avatar chico"><%= Vista.esc(cl.getIniciales()) %></span>
                            <div>
                                <div class="fw-semibold">
                                    <%= Vista.esc(cl.getNombreCompleto()) %>
<%
            if (!cl.isActivo()) {
%>
                                    <span class="badge bg-secondary"
                                          style="font-size:.6rem;">Baja</span>
<%
            }
            if (cl.getNotas() != null && !cl.getNotas().isEmpty()) {
%>
                                </div>
                                <div class="text-muted" style="font-size:.72rem;">
                                    <i class="bi bi-sticky"></i>
                                    <%= Vista.esc(cl.getNotas()) %>
                                </div>
<%
            } else {
%>
                                </div>
<%
            }
%>
                            </div>
                        </div>
                    </td>

                    <td>
                        <div style="font-size:.82rem;">
                            <%= Vista.esc(cl.getDocumento()) %>
                        </div>
                        <div class="text-muted" style="font-size:.7rem;">
                            <%= Vista.esc(cl.getTipoDocumento()) %> &middot; NIT
                            <%= Vista.esc(cl.getNit()) %>
                        </div>
                    </td>

                    <td style="font-size:.82rem;">
<%
            if (cl.getTelefono() != null && !cl.getTelefono().isEmpty()) {
%>
                        <div><i class="bi bi-telephone text-muted me-1"></i>
                             <%= Vista.esc(cl.getTelefono()) %></div>
<%
            }
            if (cl.getCorreo() != null && !cl.getCorreo().isEmpty()) {
%>
                        <div class="text-muted" style="font-size:.74rem;">
                            <i class="bi bi-envelope me-1"></i>
                            <%= Vista.esc(cl.getCorreo()) %>
                        </div>
<%
            }
%>
                    </td>

                    <td style="font-size:.82rem;">
                        <%= Vista.esc(cl.getPais()) %>
<%
            if (cl.isExtranjero()) {
%>
                        <span class="badge bg-info-subtle text-info-emphasis"
                              style="font-size:.6rem;">Extranjero</span>
<%
            }
%>
                    </td>

                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/clientes?accion=historial&id=<%= cl.getIdCliente() %>"
                               title="Ver historial">
                                <i class="bi bi-clock-history"></i>
                            </a>
<%
            if (puedeOperar) {
%>
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/clientes?accion=editar&id=<%= cl.getIdCliente() %>"
                               title="Editar">
                                <i class="bi bi-pencil"></i>
                            </a>
<%
                if (cl.isActivo()) {
%>
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/reservas?accion=nueva&idCliente=<%= cl.getIdCliente() %>"
                               title="Nueva reserva">
                                <i class="bi bi-journal-plus"></i>
                            </a>
                            <a class="btn btn-outline-danger"
                               href="<%= ctx %>/clientes?accion=baja&id=<%= cl.getIdCliente() %>"
                               title="Dar de baja"
                               onclick="return confirmar('Se dara de baja a <%= Vista.esc(cl.getNombreCompleto()) %>. Su historial se conserva. Continuar?');">
                                <i class="bi bi-person-dash"></i>
                            </a>
<%
                } else {
%>
                            <a class="btn btn-outline-success"
                               href="<%= ctx %>/clientes?accion=alta&id=<%= cl.getIdCliente() %>"
                               title="Reactivar">
                                <i class="bi bi-person-check"></i>
                            </a>
<%
                }
            }
%>
                        </div>
                    </td>
                </tr>
<%
        }
%>
            </tbody>
        </table>
    </div>

    <div class="card-footer bg-white text-muted" style="font-size:.8rem;">
        <%= clientes.size() %> cliente(s) listado(s).
    </div>
<%
    }
%>
</div>

<jsp:include page="../comun/pie.jsp" />
