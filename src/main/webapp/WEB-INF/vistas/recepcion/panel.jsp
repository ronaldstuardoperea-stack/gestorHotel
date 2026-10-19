<%--
    recepcion/panel.jsp

    Panel de trabajo del recepcionista: las entradas y salidas del dia,
    y quien esta hospedado en este momento.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Recepcion" />
    <jsp:param name="subtitulo" value="Entradas y salidas del dia" />
    <jsp:param name="activo" value="recepcion" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    List<Reserva> llegadas = (List<Reserva>) request.getAttribute("llegadas");
    if (llegadas == null) { llegadas = new ArrayList<Reserva>(); }

    List<Reserva> salidas = (List<Reserva>) request.getAttribute("salidas");
    if (salidas == null) { salidas = new ArrayList<Reserva>(); }

    List<Reserva> enCasa = (List<Reserva>) request.getAttribute("enCasa");
    if (enCasa == null) { enCasa = new ArrayList<Reserva>(); }

    Integer ocupacionObj = (Integer) request.getAttribute("ocupacion");
    int ocupacion = (ocupacionObj == null) ? 0 : ocupacionObj.intValue();
%>

<div class="row g-3 mb-3">
    <div class="col-md-4">
        <div class="indicador">
            <i class="bi bi-box-arrow-in-right icono"></i>
            <div class="etiqueta">Llegadas pendientes</div>
            <div class="valor text-success"><%= llegadas.size() %></div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="indicador">
            <i class="bi bi-box-arrow-right icono"></i>
            <div class="etiqueta">Salidas por atender</div>
            <div class="valor text-primary"><%= salidas.size() %></div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="indicador">
            <i class="bi bi-people-fill icono"></i>
            <div class="etiqueta">Huespedes en casa</div>
            <div class="valor"><%= enCasa.size() %></div>
            <div class="nota"><%= ocupacion %>% de ocupacion</div>
        </div>
    </div>
</div>

<div class="row g-3 mb-3">

    <%-- ------------------------------------------ Llegadas --%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-box-arrow-in-right text-success me-2"></i>
                Llegadas de hoy
            </div>

<%
    if (llegadas.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-calendar-check fs-1 opacity-25 d-block mb-2"></i>
                No hay llegadas pendientes para hoy.
            </div>
<%
    } else {
%>
            <div class="list-group list-group-flush">
<%
        for (Reserva r : llegadas) {
%>
                <div class="list-group-item d-flex justify-content-between
                            align-items-center gap-2">
                    <div>
                        <div class="fw-semibold">
                            <%= Vista.esc(r.getNombreCliente()) %>
                        </div>
                        <div class="text-muted" style="font-size:.76rem;">
                            <%= Vista.esc(r.getCodigo()) %> &middot;
                            habitacion <%= Vista.esc(r.getNumeroHabitacion()) %>
                            &middot; <%= r.getNoches() %> noche(s)
                            &middot; <%= r.getTotalHuespedes() %> huesped(es)
                        </div>
                        <span class="badge bg-<%= r.getColorEstado() %>"
                              style="font-size:.62rem;">
                            <%= Vista.esc(r.getEstadoLegible()) %>
                        </span>
                    </div>

<%
            if (puedeOperar) {
%>
                    <a href="<%= ctx %>/recepcion?accion=checkin&id=<%= r.getIdReserva() %>"
                       class="btn btn-sm btn-hotel flex-shrink-0">
                        Check-in
                    </a>
<%
            }
%>
                </div>
<%
        }
%>
            </div>
<%
    }
%>
        </div>
    </div>

    <%-- ------------------------------------------- Salidas --%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-box-arrow-right text-primary me-2"></i>
                Salidas por atender
            </div>

<%
    if (salidas.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-suitcase fs-1 opacity-25 d-block mb-2"></i>
                No hay salidas pendientes.
            </div>
<%
    } else {
%>
            <div class="list-group list-group-flush">
<%
        for (Reserva r : salidas) {
%>
                <div class="list-group-item d-flex justify-content-between
                            align-items-center gap-2">
                    <div>
                        <div class="fw-semibold">
                            <%= Vista.esc(r.getNombreCliente()) %>
                        </div>
                        <div class="text-muted" style="font-size:.76rem;">
                            <%= Vista.esc(r.getCodigo()) %> &middot;
                            habitacion <%= Vista.esc(r.getNumeroHabitacion()) %>
                            &middot; salida <%= Vista.txt(r.getFechaSalida()) %>
                        </div>
<%
            if (r.isVencida()) {
%>
                        <span class="badge bg-danger" style="font-size:.62rem;">
                            <i class="bi bi-exclamation-triangle me-1"></i>
                            Se paso de la fecha
                        </span>
<%
            }
%>
                    </div>

<%
            if (puedeOperar) {
%>
                    <a href="<%= ctx %>/recepcion?accion=checkout&id=<%= r.getIdReserva() %>"
                       class="btn btn-sm btn-hotel flex-shrink-0">
                        Check-out
                    </a>
<%
            }
%>
                </div>
<%
        }
%>
            </div>
<%
    }
%>
        </div>
    </div>
</div>

<%-- --------------------------------------- Huespedes en casa --%>
<div class="card">
    <div class="card-header">
        <i class="bi bi-people-fill me-2"></i> Huespedes hospedados en este momento
    </div>

<%
    if (enCasa.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-house fs-1 opacity-25 d-block mb-2"></i>
        No hay huespedes hospedados en este momento.
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Hab.</th>
                    <th>Huesped</th>
                    <th>Estadia</th>
                    <th>Check-in</th>
                    <th class="text-end">Acciones</th>
                </tr>
            </thead>
            <tbody>
<%
        for (Reserva r : enCasa) {
%>
                <tr>
                    <td>
                        <span class="badge-suave fs-6"><%= Vista.esc(r.getNumeroHabitacion()) %></span>
                    </td>
                    <td>
                        <div class="fw-semibold">
                            <%= Vista.esc(r.getNombreCliente()) %>
                        </div>
                        <div class="text-muted" style="font-size:.72rem;">
                            <%= Vista.esc(r.getCodigo()) %>
                        </div>
                    </td>
                    <td style="font-size:.82rem;">
                        <%= Vista.txt(r.getFechaEntrada()) %> &rarr; <%= Vista.txt(r.getFechaSalida()) %>
                    </td>
                    <td class="text-muted" style="font-size:.78rem;">
                        <%= Vista.txt(r.getFechaCheckin()) %>
                    </td>
                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/reservas?accion=detalle&id=<%= r.getIdReserva() %>"
                               title="Ver ficha">
                                <i class="bi bi-eye"></i>
                            </a>
<%
            if (puedeOperar) {
%>
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/cargos?idReserva=<%= r.getIdReserva() %>"
                               title="Anotar consumo">
                                <i class="bi bi-cart-plus"></i>
                            </a>
                            <a class="btn btn-outline-primary"
                               href="<%= ctx %>/recepcion?accion=checkout&id=<%= r.getIdReserva() %>"
                               title="Check-out">
                                <i class="bi bi-box-arrow-right"></i>
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
<%
    }
%>
</div>

<jsp:include page="../comun/pie.jsp" />
