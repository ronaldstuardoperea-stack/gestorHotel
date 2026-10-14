<%--
    habitaciones/detalle.jsp

    Ficha de una habitacion: sus datos y los bloqueos que tiene
    registrados.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Habitacion" %>
<%@ page import="com.hotel.modelo.BloqueoHabitacion" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Ficha de habitacion" />
    <jsp:param name="subtitulo" value="Detalle e historial de bloqueos" />
    <jsp:param name="activo" value="habitaciones" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    Habitacion habitacion = (Habitacion) request.getAttribute("habitacion");

    List<BloqueoHabitacion> bloqueos =
            (List<BloqueoHabitacion>) request.getAttribute("bloqueos");
    if (bloqueos == null) { bloqueos = new ArrayList<BloqueoHabitacion>(); }
%>

<div class="mb-3">
    <a href="<%= ctx %>/habitaciones" class="btn btn-sm btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i> Volver al inventario
    </a>
</div>

<div class="row g-3">

    <%-- ------------------------------------ Datos generales --%>
    <div class="col-lg-5">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-info-circle me-2"></i> Datos de la habitacion
            </div>
            <div class="card-body">

                <div class="d-flex align-items-center gap-3 mb-4">
                    <div class="text-center">
                        <div style="font-size:2.6rem; font-weight:700; line-height:1;
                             color:var(--hotel-noche);">
                            <%= Vista.esc(habitacion.getNumero()) %>
                        </div>
                        <span class="badge bg-<%= habitacion.getColorEstado() %>">
                            <%= Vista.esc(habitacion.getEstado()) %>
                        </span>
                    </div>
                    <div>
                        <div class="fw-semibold fs-5">
                            <%= Vista.esc(habitacion.getNombreTipo()) %>
                        </div>
                        <div class="text-muted" style="font-size:.85rem;">
                            Piso <%= habitacion.getPiso() %>
                        </div>
                        <div class="mt-1" style="color:var(--hotel-azul); font-weight:600;">
                            Q <%= Vista.moneda(habitacion.getTarifaBase()) %> por noche
                        </div>
                    </div>
                </div>

                <table class="table table-sm">
                    <tr>
                        <th style="width:45%;">Capacidad</th>
                        <td>
                            <%= habitacion.getCapacidadAdultos() %> adulto(s)
<%
    if (habitacion.getCapacidadNinos() > 0) {
%>
                            + <%= habitacion.getCapacidadNinos() %> nino(s)
<%
    }
%>
                        </td>
                    </tr>
                    <tr>
                        <th>Descripcion</th>
                        <td>
<%
    if (habitacion.getDescripcion() == null || habitacion.getDescripcion().isEmpty()) {
%>
                            <span class="text-muted">Sin descripcion</span>
<%
    } else {
%>
                            <%= Vista.esc(habitacion.getDescripcion()) %>
<%
    }
%>
                        </td>
                    </tr>
                </table>

<%
    if (puedeOperar) {
%>
                <div class="d-grid gap-2 mt-3">
                    <a href="<%= ctx %>/habitaciones?accion=editar&id=<%= habitacion.getIdHabitacion() %>"
                       class="btn btn-sm btn-hotel">
                        <i class="bi bi-pencil me-1"></i> Editar habitacion
                    </a>
                    <a href="<%= ctx %>/bloqueos?accion=nuevo&idHabitacion=<%= habitacion.getIdHabitacion() %>"
                       class="btn btn-sm btn-outline-secondary">
                        <i class="bi bi-lock me-1"></i> Bloquear por fechas
                    </a>
                </div>
<%
    }
%>
            </div>
        </div>
    </div>

    <%-- ------------------------------------------ Bloqueos --%>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-lock me-2"></i> Bloqueos activos
            </div>

<%
    if (bloqueos.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-unlock fs-1 opacity-25 d-block mb-2"></i>
                Esta habitacion no tiene bloqueos registrados.
            </div>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Periodo</th>
                            <th>Motivo</th>
                            <th>Registro</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (BloqueoHabitacion b : bloqueos) {
%>
                        <tr>
                            <td>
                                <div class="fw-semibold" style="font-size:.84rem;">
                                    <%= Vista.txt(b.getFechaInicio()) %> &rarr; <%= Vista.txt(b.getFechaFin()) %>
                                </div>
<%
            if (b.isVigente()) {
%>
                                <span class="badge bg-danger"
                                      style="font-size:.62rem;">Vigente hoy</span>
<%
            }
            if (b.isVencido()) {
%>
                                <span class="badge bg-secondary"
                                      style="font-size:.62rem;">Ya vencio</span>
<%
            }
%>
                            </td>
                            <td style="font-size:.84rem;">
                                <%= Vista.esc(b.getMotivo()) %>
                            </td>
                            <td class="text-muted" style="font-size:.76rem;">
                                <%= Vista.esc(b.getNombreUsuario()) %>
                            </td>
                            <td class="text-end">
<%
            if (puedeOperar) {
%>
                                <a class="btn btn-sm btn-outline-danger"
                                   href="<%= ctx %>/bloqueos?accion=levantar&id=<%= b.getIdBloqueo() %>"
                                   onclick="return confirmar('Se levantara el bloqueo. Continuar?');">
                                    Levantar
                                </a>
<%
            }
%>
                            </td>
                        </tr>
<%
        }
%>
                    </tbody>
                </table>
            </div>
<%
    }
%>
        </div>
    </div>
</div>

<jsp:include page="../comun/pie.jsp" />
