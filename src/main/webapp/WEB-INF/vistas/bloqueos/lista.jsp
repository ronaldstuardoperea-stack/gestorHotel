<%--
    bloqueos/lista.jsp

    Habitaciones retiradas de la venta por mantenimiento, remodelacion o
    uso interno. Responde al requerimiento de "bloqueo de habitaciones".
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.BloqueoHabitacion" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Bloqueos" />
    <jsp:param name="subtitulo" value="Habitaciones retiradas de la venta" />
    <jsp:param name="activo" value="bloqueos" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    List<BloqueoHabitacion> bloqueos =
            (List<BloqueoHabitacion>) request.getAttribute("bloqueos");
    if (bloqueos == null) { bloqueos = new ArrayList<BloqueoHabitacion>(); }

    Boolean banderaActivos = (Boolean) request.getAttribute("soloActivos");
    boolean soloActivos = (banderaActivos != null && banderaActivos.booleanValue());
%>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div class="btn-group btn-group-sm">
        <a href="<%= ctx %>/bloqueos"
           class="btn <%= soloActivos ? "btn-hotel" : "btn-outline-secondary" %>">
            Activos
        </a>
        <a href="<%= ctx %>/bloqueos?historial=1"
           class="btn <%= soloActivos ? "btn-outline-secondary" : "btn-hotel" %>">
            Historial completo
        </a>
    </div>

<%
    if (puedeOperar) {
%>
    <a href="<%= ctx %>/bloqueos?accion=nuevo" class="btn btn-sm btn-oro">
        <i class="bi bi-lock me-1"></i> Bloquear habitacion
    </a>
<%
    }
%>
</div>

<div class="alert alert-light border" style="font-size:.85rem;">
    <i class="bi bi-info-circle me-1" style="color:var(--hotel-azul);"></i>
    Un bloqueo activo impide reservar la habitacion en ese rango de fechas.
    El sistema no deja bloquear una habitacion que ya tenga reservas
    vigentes en el periodo.
</div>

<div class="card">
<%
    if (bloqueos.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-unlock fs-1 opacity-25 d-block mb-2"></i>
<%
        if (soloActivos) {
%>
        No hay bloqueos activos. Todas las habitaciones estan
        disponibles para la venta.
<%
        } else {
%>
        No hay bloqueos registrados.
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
                    <th>Habitacion</th>
                    <th>Periodo</th>
                    <th>Motivo</th>
                    <th>Registrado por</th>
                    <th>Estado</th>
                    <th class="text-end"></th>
                </tr>
            </thead>
            <tbody>
<%
        for (BloqueoHabitacion b : bloqueos) {
%>
                <tr class="<%= b.isActivo() ? "" : "opacity-50" %>">
                    <td>
                        <span class="badge-suave fs-6"><%= Vista.esc(b.getNumeroHabitacion()) %></span>
                        <div class="text-muted" style="font-size:.7rem;">
                            <%= Vista.esc(b.getNombreTipo()) %>
                        </div>
                    </td>

                    <td style="font-size:.84rem;">
                        <%= Vista.txt(b.getFechaInicio()) %> &rarr; <%= Vista.txt(b.getFechaFin()) %>
                    </td>

                    <td style="font-size:.84rem;">
                        <%= Vista.esc(b.getMotivo()) %>
                    </td>

                    <td class="text-muted" style="font-size:.78rem;">
                        <%= Vista.esc(b.getNombreUsuario()) %>
                        <div style="font-size:.7rem;"><%= Vista.txt(b.getFechaRegistro()) %></div>
                    </td>

                    <td>
<%
            if (!b.isActivo()) {
%>
                        <span class="badge bg-secondary">Levantado</span>
<%
            } else if (b.isVigente()) {
%>
                        <span class="badge bg-danger">Vigente hoy</span>
<%
            } else if (b.isVencido()) {
%>
                        <span class="badge bg-secondary">Ya vencio</span>
<%
            } else {
%>
                        <span class="badge bg-info">Programado</span>
<%
            }
%>
                    </td>

                    <td class="text-end">
<%
            if (puedeOperar && b.isActivo()) {
%>
                        <a class="btn btn-sm btn-outline-danger"
                           href="<%= ctx %>/bloqueos?accion=levantar&id=<%= b.getIdBloqueo() %>"
                           onclick="return confirmar('Se levantara el bloqueo de la habitacion <%= Vista.esc(b.getNumeroHabitacion()) %>. Continuar?');">
                            <i class="bi bi-unlock me-1"></i> Levantar
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

    <div class="card-footer bg-white text-muted" style="font-size:.8rem;">
        <%= bloqueos.size() %> bloqueo(s) listado(s).
    </div>
<%
    }
%>
</div>

<jsp:include page="../comun/pie.jsp" />
