<%--
    usuarios/lista.jsp

    Administracion del personal que puede usar el sistema.
    Modulo exclusivo del rol ADMINISTRADOR.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Usuarios" />
    <jsp:param name="subtitulo" value="Personal autorizado para usar el sistema" />
    <jsp:param name="activo" value="usuarios" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario yo = (Usuario) session.getAttribute("usuario");
    int idYo = (yo == null) ? 0 : yo.getIdUsuario();

    List<Usuario> usuarios = (List<Usuario>) request.getAttribute("usuarios");
    if (usuarios == null) { usuarios = new ArrayList<Usuario>(); }
%>

<div class="row g-3 mb-3">
    <div class="col-md-8">
        <div class="alert alert-light border mb-0 h-100" style="font-size:.84rem;">
            <div class="fw-semibold mb-1">Que puede hacer cada rol</div>
            <div><strong>Administrador:</strong> todo, incluidos tarifas,
                 temporadas, usuarios y anulacion de facturas.</div>
            <div><strong>Recepcion:</strong> reservas, check-in, check-out,
                 consumos, cobros y emision de facturas.</div>
            <div><strong>Auditor:</strong> solo consulta y reportes; no puede
                 registrar ni modificar nada.</div>
        </div>
    </div>

    <div class="col-md-4 d-flex align-items-end justify-content-end">
        <a href="<%= ctx %>/usuarios?accion=nuevo" class="btn btn-sm btn-oro">
            <i class="bi bi-person-plus me-1"></i> Nuevo usuario
        </a>
    </div>
</div>

<div class="card">
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Usuario</th>
                    <th>Rol</th>
                    <th>Ultimo acceso</th>
                    <th>Estado</th>
                    <th class="text-end">Acciones</th>
                </tr>
            </thead>
            <tbody>
<%
    for (Usuario us : usuarios) {
        boolean esYo = (us.getIdUsuario() == idYo);
        String tonoRol = us.isAdministrador() ? "dark"
                       : (us.isSoloLectura() ? "secondary" : "primary");
%>
                <tr class="<%= us.isActivo() ? "" : "opacity-50" %>">

                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <span class="avatar chico <%= us.isAdministrador() ? "oro" : "" %>">
                                <%= Vista.esc(us.getIniciales()) %>
                            </span>
                            <div>
                                <div class="fw-semibold">
                                    <%= Vista.esc(us.getNombreCompleto()) %>
<%
        if (esYo) {
%>
                                    <span class="badge bg-info-subtle text-info-emphasis"
                                          style="font-size:.6rem;">Usted</span>
<%
        }
%>
                                </div>
                                <div class="text-muted" style="font-size:.74rem;">
                                    <%= Vista.esc(us.getUsuario()) %>
                                </div>
                            </div>
                        </div>
                    </td>

                    <td>
                        <span class="badge bg-<%= tonoRol %>">
                            <%= Vista.esc(us.getRol()) %>
                        </span>
                    </td>

                    <td class="text-muted" style="font-size:.8rem;">
                        <%= us.getUltimoAcceso() == null
                              ? "Nunca ha ingresado"
                              : Vista.esc(us.getUltimoAcceso()) %>
                    </td>

                    <td>
                        <span class="badge bg-<%= us.isActivo() ? "success" : "secondary" %>">
                            <%= us.isActivo() ? "Activo" : "Inactivo" %>
                        </span>
                    </td>

                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/usuarios?accion=editar&id=<%= us.getIdUsuario() %>"
                               title="Editar">
                                <i class="bi bi-pencil"></i>
                            </a>

                            <%-- Nadie puede desactivarse a si mismo: si fuera
                                 el unico administrador, el sistema quedaria
                                 sin quien lo administre. --%>
<%
        if (!esYo && us.isActivo()) {
%>
                            <a class="btn btn-outline-danger"
                               href="<%= ctx %>/usuarios?accion=desactivar&id=<%= us.getIdUsuario() %>"
                               title="Desactivar"
                               onclick="return confirmar('El usuario <%= Vista.esc(us.getUsuario()) %> ya no podra iniciar sesion. Continuar?');">
                                <i class="bi bi-person-dash"></i>
                            </a>
<%
        } else if (!esYo) {
%>
                            <a class="btn btn-outline-success"
                               href="<%= ctx %>/usuarios?accion=activar&id=<%= us.getIdUsuario() %>"
                               title="Activar">
                                <i class="bi bi-person-check"></i>
                            </a>
<%
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
</div>

<jsp:include page="../comun/pie.jsp" />
