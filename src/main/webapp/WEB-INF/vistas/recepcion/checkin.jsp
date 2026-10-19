<%--
    recepcion/checkin.jsp

    Pantalla de confirmacion del check-in. Antes de darle entrada al
    huesped se le muestra al recepcionista el resumen de la reserva y se
    le advierte si faltan ocupantes por registrar.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.Ocupante" %>
<%@ page import="com.hotel.modelo.EstadoCuenta" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Check-in" />
    <jsp:param name="subtitulo" value="Registro de entrada del huesped" />
    <jsp:param name="activo" value="recepcion" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Reserva reserva = (Reserva) request.getAttribute("reserva");
    EstadoCuenta cuenta = (EstadoCuenta) request.getAttribute("cuenta");

    List<Ocupante> ocupantes = (List<Ocupante>) request.getAttribute("ocupantes");
    if (ocupantes == null) { ocupantes = new ArrayList<Ocupante>(); }

    Boolean bandera = (Boolean) request.getAttribute("faltanOcupantes");
    boolean faltanOcupantes = (bandera != null && bandera.booleanValue());
%>

<div class="row justify-content-center">
    <div class="col-lg-9">

        <div class="card mb-3">
            <div class="card-header">
                <i class="bi bi-box-arrow-in-right text-success me-2"></i>
                Confirmar entrada de <%= Vista.esc(reserva.getCodigo()) %>
            </div>
            <div class="card-body">

                <div class="row g-4">
                    <div class="col-md-6">
                        <h6 class="text-muted" style="font-size:.72rem; font-weight:700;
                            letter-spacing:.1em; text-transform:uppercase;">
                            Huesped titular
                        </h6>
                        <div class="fs-5 fw-semibold">
                            <%= Vista.esc(reserva.getNombreCliente()) %>
                        </div>
                        <div class="text-muted" style="font-size:.85rem;">
                            <%= Vista.esc(reserva.getDocumentoCliente()) %>
<%
    if (reserva.getTelefonoCliente() != null && !reserva.getTelefonoCliente().isEmpty()) {
%>
                            &middot; <%= Vista.esc(reserva.getTelefonoCliente()) %>
<%
    }
%>
                        </div>
                    </div>

                    <div class="col-md-6">
                        <h6 class="text-muted" style="font-size:.72rem; font-weight:700;
                            letter-spacing:.1em; text-transform:uppercase;">
                            Habitacion asignada
                        </h6>
                        <div class="fs-5 fw-semibold">
                            <%= Vista.esc(reserva.getNumeroHabitacion()) %>
                            <span class="text-muted fs-6">
                                (<%= Vista.esc(reserva.getNombreTipo()) %>)
                            </span>
                        </div>
                        <div class="text-muted" style="font-size:.85rem;">
                            <%= Vista.txt(reserva.getFechaEntrada()) %> &rarr;
                            <%= Vista.txt(reserva.getFechaSalida()) %>
                            &middot; <%= reserva.getNoches() %> noche(s)
                        </div>
                    </div>
                </div>

                <hr>

                <div class="row g-3">
                    <div class="col-6 col-md-3">
                        <div class="text-muted" style="font-size:.74rem;">Huespedes</div>
                        <div class="fw-semibold">
                            <%= reserva.getAdultos() %> adulto(s)
<%
    if (reserva.getNinos() > 0) {
%>
                            + <%= reserva.getNinos() %> nino(s)
<%
    }
%>
                        </div>
                    </div>
                    <div class="col-6 col-md-3">
                        <div class="text-muted" style="font-size:.74rem;">Tarifa por noche</div>
                        <div class="fw-semibold">
                            Q <%= Vista.moneda(reserva.getTarifaNoche()) %>
                        </div>
                    </div>
                    <div class="col-6 col-md-3">
                        <div class="text-muted" style="font-size:.74rem;">Total hospedaje</div>
                        <div class="fw-semibold">
                            Q <%= Vista.moneda(reserva.getTotalHospedaje()) %>
                        </div>
                    </div>
                    <div class="col-6 col-md-3">
                        <div class="text-muted" style="font-size:.74rem;">Anticipo recibido</div>
                        <div class="fw-semibold text-success">
                            Q <%= Vista.moneda(cuenta == null ? null : cuenta.getTotalPagado()) %>
                        </div>
                    </div>
                </div>

<%
    if (reserva.getObservaciones() != null && !reserva.getObservaciones().isEmpty()) {
%>
                <div class="alert alert-warning mt-3 mb-0 py-2" style="font-size:.85rem;">
                    <i class="bi bi-sticky me-1"></i>
                    <strong>Peticion del huesped:</strong>
                    <%= Vista.esc(reserva.getObservaciones()) %>
                </div>
<%
    }
%>
            </div>
        </div>

        <%-- Aviso de ocupantes: es una advertencia, no un impedimento.
             El hotel puede dar entrada y completar los datos despues. --%>
<%
    if (faltanOcupantes) {
%>
        <div class="alert alert-warning d-flex align-items-start gap-2">
            <i class="bi bi-person-exclamation mt-1"></i>
            <div>
                <strong>Faltan ocupantes por registrar.</strong>
                La reserva es para <%= reserva.getTotalHuespedes() %> huesped(es) y hay
                menos registrados. Puede continuar con el check-in y completar
                los datos despues desde la ficha de la reserva.
            </div>
        </div>
<%
    }
%>

        <div class="card mb-3">
            <div class="card-header">
                <i class="bi bi-people me-2"></i> Ocupantes registrados
            </div>
<%
    if (ocupantes.isEmpty()) {
%>
            <div class="card-body text-muted text-center py-4">
                Aun no se ha registrado ningun ocupante.
                <div class="mt-2">
                    <a href="<%= ctx %>/reservas?accion=detalle&id=<%= reserva.getIdReserva() %>"
                       class="btn btn-sm btn-outline-secondary">
                        Registrarlos ahora
                    </a>
                </div>
            </div>
<%
    } else {
%>
            <ul class="list-group list-group-flush">
<%
        for (Ocupante o : ocupantes) {
%>
                <li class="list-group-item d-flex align-items-center gap-2">
                    <span class="avatar chico"><%= Vista.esc(o.getIniciales()) %></span>
                    <span><%= Vista.esc(o.getNombreCompleto()) %></span>
<%
            if (o.isEsTitular()) {
%>
                    <span class="badge bg-secondary"
                          style="font-size:.62rem;">Titular</span>
<%
            }
            if (o.getDocumento() != null && !o.getDocumento().isEmpty()) {
%>
                    <span class="text-muted ms-auto" style="font-size:.76rem;">
                        <%= Vista.esc(o.getDocumento()) %>
                    </span>
<%
            }
%>
                </li>
<%
        }
%>
            </ul>
<%
    }
%>
        </div>

        <%-- El check-in se envia por POST porque cambia el estado del
             hotel. Un enlace GET podria dispararse solo si el navegador
             precarga la direccion o el usuario refresca la pagina. --%>
        <form method="post" action="<%= ctx %>/recepcion"
              onsubmit="return confirmar('Se registrara la entrada y la habitacion <%= Vista.esc(reserva.getNumeroHabitacion()) %> quedara ocupada. Continuar?');">
            <input type="hidden" name="accion" value="checkin">
            <input type="hidden" name="idReserva" value="<%= reserva.getIdReserva() %>">

            <div class="d-flex gap-2 justify-content-end">
                <a href="<%= ctx %>/recepcion" class="btn btn-outline-secondary">Cancelar</a>
                <button type="submit" class="btn btn-oro btn-lg">
                    <i class="bi bi-box-arrow-in-right me-1"></i>
                    Confirmar check-in
                </button>
            </div>
        </form>
    </div>
</div>

<jsp:include page="../comun/pie.jsp" />
