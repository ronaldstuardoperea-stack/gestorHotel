<%--
    clientes/historial.jsp

    Ficha completa del cliente. Responde al requerimiento de "historial
    de clientes con datos economicos, visitas y temporadas": muestra
    cuanto ha gastado, cuantas veces ha venido y todas sus reservas.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Cliente" %>
<%@ page import="com.hotel.modelo.HistorialCliente" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Historial del cliente" />
    <jsp:param name="subtitulo" value="Perfil economico, visitas y reservas" />
    <jsp:param name="activo" value="clientes" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    Cliente cliente = (Cliente) request.getAttribute("cliente");
    HistorialCliente historial = (HistorialCliente) request.getAttribute("historial");

    List<Reserva> reservas = (List<Reserva>) request.getAttribute("reservas");
    if (reservas == null) { reservas = new ArrayList<Reserva>(); }
%>

<div class="mb-3 d-flex gap-2">
    <a href="<%= ctx %>/clientes" class="btn btn-sm btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i> Volver al directorio
    </a>
<%
    if (puedeOperar) {
%>
    <a href="<%= ctx %>/reservas?accion=nueva&idCliente=<%= cliente.getIdCliente() %>"
       class="btn btn-sm btn-oro">
        <i class="bi bi-journal-plus me-1"></i> Nueva reserva para este cliente
    </a>
<%
    }
%>
</div>

<div class="row g-3 mb-3">

    <%-- ------------------------------------ Datos del cliente --%>
    <div class="col-lg-4">
        <div class="card h-100">
            <div class="card-body">

                <div class="text-center mb-3">
                    <span class="avatar oro mx-auto mb-2"
                          style="width:64px; height:64px; font-size:1.3rem;">
                        <%= Vista.esc(cliente.getIniciales()) %>
                    </span>
                    <h5 class="mb-1"><%= Vista.esc(cliente.getNombreCompleto()) %></h5>

<%
    if (historial != null) {
%>
                    <span class="badge bg-<%= historial.getColorCategoria() %>">
                        <i class="bi bi-award me-1"></i>
                        Cliente <%= Vista.esc(historial.getCategoria()) %>
                    </span>
<%
    }
%>
                </div>

                <table class="table table-sm mb-0">
                    <tr>
                        <th style="width:40%;">Documento</th>
                        <td>
                            <%= Vista.esc(cliente.getDocumento()) %>
                            <div class="text-muted" style="font-size:.7rem;">
                                <%= Vista.esc(cliente.getTipoDocumento()) %>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <th>NIT</th>
                        <td><%= Vista.esc(cliente.getNit()) %></td>
                    </tr>
                    <tr>
                        <th>Telefono</th>
                        <td><%= Vista.esc(cliente.getTelefono()) %></td>
                    </tr>
                    <tr>
                        <th>Correo</th>
                        <td style="word-break:break-all;">
                            <%= Vista.esc(cliente.getCorreo()) %>
                        </td>
                    </tr>
                    <tr>
                        <th>Direccion</th>
                        <td><%= Vista.esc(cliente.getDireccion()) %></td>
                    </tr>
                    <tr>
                        <th>Pais</th>
                        <td><%= Vista.esc(cliente.getPais()) %></td>
                    </tr>
<%
    if (cliente.getNotas() != null && !cliente.getNotas().isEmpty()) {
%>
                    <tr>
                        <th>Notas</th>
                        <td class="text-warning-emphasis">
                            <%= Vista.esc(cliente.getNotas()) %>
                        </td>
                    </tr>
<%
    }
%>
                </table>
            </div>
        </div>
    </div>

    <%-- ------------------------------------ Perfil economico --%>
    <div class="col-lg-8">
<%
    if (historial == null) {
%>
        <div class="card h-100">
            <div class="card-body text-center text-muted py-5">
                Este cliente aun no tiene movimientos registrados.
            </div>
        </div>
<%
    } else {
%>
        <div class="row g-2 mb-2">
            <div class="col-6 col-md-3">
                <div class="indicador py-2">
                    <div class="etiqueta">Consumo total</div>
                    <div class="valor" style="font-size:1.35rem;">
                        <span class="fs-6 text-muted">Q</span>
                        <%= Vista.moneda(historial.getConsumoHistorico()) %>
                    </div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="indicador py-2">
                    <div class="etiqueta">Visitas</div>
                    <div class="valor" style="font-size:1.35rem;">
                        <%= historial.getVisitasCompletadas() %>
                    </div>
                    <div class="nota"><%= historial.getTotalReservas() %> reservas en total</div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="indicador py-2">
                    <div class="etiqueta">Noches</div>
                    <div class="valor" style="font-size:1.35rem;">
                        <%= historial.getNochesTotales() %>
                    </div>
                    <div class="nota">
                        Q <%= Vista.moneda(historial.getPromedioPorNoche()) %> promedio
                    </div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="indicador py-2">
                    <div class="etiqueta">Saldo actual</div>
                    <div class="valor <%= historial.isConDeuda() ? "text-danger" : "text-success" %>"
                         style="font-size:1.35rem;">
                        <span class="fs-6 text-muted">Q</span>
                        <%= Vista.moneda(historial.getSaldoActual()) %>
                    </div>
                </div>
            </div>
        </div>

        <div class="card">
            <div class="card-body py-3">
                <div class="row" style="font-size:.84rem;">
                    <div class="col-md-4">
                        <span class="text-muted">Ultima visita:</span>
                        <strong>
                            <%= historial.getUltimaVisita() == null
                                  ? "Nunca"
                                  : Vista.txt(historial.getUltimaVisita()) %>
                        </strong>
                    </div>
                    <div class="col-md-4">
                        <span class="text-muted">Cancelaciones:</span>
                        <strong><%= historial.getCancelaciones() %></strong>
                        (<%= historial.getPorcentajeCancelacion() %>%)
                    </div>
                    <div class="col-md-4">
<%
        if (historial.isFrecuente()) {
%>
                        <span class="badge bg-success-subtle text-success-emphasis">
                            <i class="bi bi-star-fill me-1"></i> Huesped frecuente
                        </span>
<%
        }
        if (historial.getPorcentajeCancelacion() >= 40) {
%>
                        <span class="badge bg-warning-subtle text-warning-emphasis">
                            <i class="bi bi-exclamation-triangle me-1"></i>
                            Pedir anticipo
                        </span>
<%
        }
%>
                    </div>
                </div>
            </div>
        </div>
<%
    }
%>
    </div>
</div>

<%-- ---------------------------------------- Reservas --%>
<div class="card">
    <div class="card-header">
        <i class="bi bi-journal-bookmark me-2"></i> Reservas de este cliente
    </div>

<%
    if (reservas.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-journal fs-1 opacity-25 d-block mb-2"></i>
        Este cliente aun no tiene reservas.
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Codigo</th>
                    <th>Hab.</th>
                    <th>Estadia</th>
                    <th>Noches</th>
                    <th class="numero-tabla">Tarifa</th>
                    <th class="numero-tabla">Hospedaje</th>
                    <th>Estado</th>
                </tr>
            </thead>
            <tbody>
<%
        for (Reserva r : reservas) {
%>
                <tr onclick="location.href='<%= ctx %>/reservas?accion=detalle&id=<%= r.getIdReserva() %>'"
                    style="cursor:pointer;">
                    <td><span class="badge-suave"><%= Vista.esc(r.getCodigo()) %></span></td>
                    <td><%= Vista.esc(r.getNumeroHabitacion()) %></td>
                    <td style="font-size:.82rem;">
                        <%= Vista.txt(r.getFechaEntrada()) %> &rarr; <%= Vista.txt(r.getFechaSalida()) %>
                    </td>
                    <td><%= r.getNoches() %></td>
                    <td class="numero-tabla">
                        <%= Vista.moneda(r.getTarifaNoche()) %>
                    </td>
                    <td class="numero-tabla fw-semibold">
                        <%= Vista.moneda(r.getTotalHospedaje()) %>
                    </td>
                    <td>
                        <span class="badge bg-<%= r.getColorEstado() %>">
                            <%= Vista.esc(r.getEstadoLegible()) %>
                        </span>
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
