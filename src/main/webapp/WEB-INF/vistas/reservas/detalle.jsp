<%--
    reservas/detalle.jsp

    Ficha completa de una reserva. Es la pantalla central del sistema:
    desde aqui se hace el check-in, se anotan consumos, se registran
    pagos y se emiten facturas, sin tener que ir buscando la reserva en
    cada modulo.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.Ocupante" %>
<%@ page import="com.hotel.modelo.Cargo" %>
<%@ page import="com.hotel.modelo.Pago" %>
<%@ page import="com.hotel.modelo.Factura" %>
<%@ page import="com.hotel.modelo.EstadoCuenta" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Ficha de reserva" />
    <jsp:param name="subtitulo" value="Estadia, ocupantes, consumos y cuenta" />
    <jsp:param name="activo" value="reservas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());
    boolean esAdmin     = (u != null && u.isAdministrador());

    Reserva reserva = (Reserva) request.getAttribute("reserva");
    EstadoCuenta cuenta = (EstadoCuenta) request.getAttribute("cuenta");

    List<Ocupante> ocupantes = (List<Ocupante>) request.getAttribute("ocupantes");
    if (ocupantes == null) { ocupantes = new ArrayList<Ocupante>(); }

    List<Cargo> cargos = (List<Cargo>) request.getAttribute("cargos");
    if (cargos == null) { cargos = new ArrayList<Cargo>(); }

    List<Pago> pagos = (List<Pago>) request.getAttribute("pagos");
    if (pagos == null) { pagos = new ArrayList<Pago>(); }

    List<Factura> facturas = (List<Factura>) request.getAttribute("facturas");
    if (facturas == null) { facturas = new ArrayList<Factura>(); }

    BigDecimal consumosPendientes = (BigDecimal) request.getAttribute("consumosPendientes");
    boolean hayPendientes = (consumosPendientes != null
                             && consumosPendientes.compareTo(BigDecimal.ZERO) > 0);

    String estado = reserva.getEstado();
    boolean cancelada  = "CANCELADA".equals(estado);
    boolean finalizada = "FINALIZADA".equals(estado);
    boolean pendiente  = "PENDIENTE".equals(estado);
%>

<%-- ============================ Encabezado y acciones --%>
<div class="card mb-3">
    <div class="card-body">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">

            <div>
                <div class="d-flex align-items-center gap-2 mb-1">
                    <h4 class="mb-0"><%= Vista.esc(reserva.getCodigo()) %></h4>
                    <span class="badge bg-<%= reserva.getColorEstado() %>">
                        <%= Vista.esc(reserva.getEstadoLegible()) %>
                    </span>
<%
    if (reserva.isVencida()) {
%>
                    <span class="badge bg-danger">
                        <i class="bi bi-exclamation-triangle me-1"></i> Salida vencida
                    </span>
<%
    }
%>
                </div>
                <div class="text-muted" style="font-size:.85rem;">
                    Registrada por <%= Vista.esc(reserva.getNombreUsuario()) %>
                    el <%= Vista.txt(reserva.getFechaCreacion()) %>
                </div>
            </div>

            <div class="d-flex flex-wrap gap-2">
                <a href="<%= ctx %>/reservas" class="btn btn-sm btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Volver
                </a>

<%
    if (puedeOperar) {
        if (pendiente) {
%>
                <a href="<%= ctx %>/reservas?accion=confirmar&id=<%= reserva.getIdReserva() %>"
                   class="btn btn-sm btn-outline-info">
                    <i class="bi bi-check2 me-1"></i> Confirmar
                </a>
<%
        }
        if (reserva.isCancelable()) {
%>
                <a href="<%= ctx %>/reservas?accion=editar&id=<%= reserva.getIdReserva() %>"
                   class="btn btn-sm btn-outline-secondary">
                    <i class="bi bi-pencil me-1"></i> Editar
                </a>
<%
        }
        if (reserva.isCheckinHabilitado()) {
%>
                <a href="<%= ctx %>/recepcion?accion=checkin&id=<%= reserva.getIdReserva() %>"
                   class="btn btn-sm btn-hotel">
                    <i class="bi bi-box-arrow-in-right me-1"></i> Check-in
                </a>
<%
        }
        if (reserva.isCheckoutHabilitado()) {
%>
                <a href="<%= ctx %>/recepcion?accion=checkout&id=<%= reserva.getIdReserva() %>"
                   class="btn btn-sm btn-hotel">
                    <i class="bi bi-box-arrow-right me-1"></i> Check-out
                </a>
<%
        }
%>
                <a href="<%= ctx %>/pagos?accion=nuevo&idReserva=<%= reserva.getIdReserva() %>"
                   class="btn btn-sm btn-oro">
                    <i class="bi bi-cash-coin me-1"></i> Registrar pago
                </a>
<%
        if (!cancelada) {
%>
                <a href="<%= ctx %>/facturas?accion=nueva&idReserva=<%= reserva.getIdReserva() %>"
                   class="btn btn-sm btn-outline-dark">
                    <i class="bi bi-receipt me-1"></i> Facturar
                </a>
<%
        }
        if (reserva.isCancelable()) {
%>
                <a href="<%= ctx %>/reservas?accion=cancelar&id=<%= reserva.getIdReserva() %>"
                   class="btn btn-sm btn-outline-danger"
                   onclick="return confirmar('Se cancelara la reserva <%= Vista.esc(reserva.getCodigo()) %>. Continuar?');">
                    <i class="bi bi-x-lg me-1"></i> Cancelar
                </a>
<%
        }
    }
%>
            </div>
        </div>
    </div>
</div>

<div class="row g-3 mb-3">

    <%-- ------------------------------- Datos de la estadia --%>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-info-circle me-2"></i> Datos de la estadia
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-sm-6">
                        <table class="table table-sm mb-0">
                            <tr>
                                <th style="width:45%;">Cliente</th>
                                <td>
                                    <a href="<%= ctx %>/clientes?accion=historial&id=<%= reserva.getIdCliente() %>">
                                        <%= Vista.esc(reserva.getNombreCliente()) %>
                                    </a>
                                </td>
                            </tr>
                            <tr>
                                <th>Documento</th>
                                <td><%= Vista.esc(reserva.getDocumentoCliente()) %></td>
                            </tr>
                            <tr>
                                <th>Telefono</th>
                                <td><%= Vista.esc(reserva.getTelefonoCliente()) %></td>
                            </tr>
                            <tr>
                                <th>Habitacion</th>
                                <td>
                                    <strong><%= Vista.esc(reserva.getNumeroHabitacion()) %></strong>
                                    <span class="text-muted">
                                        (<%= Vista.esc(reserva.getNombreTipo()) %>)
                                    </span>
                                </td>
                            </tr>
                        </table>
                    </div>

                    <div class="col-sm-6">
                        <table class="table table-sm mb-0">
                            <tr>
                                <th style="width:45%;">Entrada</th>
                                <td><%= Vista.txt(reserva.getFechaEntrada()) %></td>
                            </tr>
                            <tr>
                                <th>Salida</th>
                                <td><%= Vista.txt(reserva.getFechaSalida()) %></td>
                            </tr>
                            <tr>
                                <th>Noches</th>
                                <td>
                                    <%= reserva.getNoches() %> x
                                    Q <%= Vista.moneda(reserva.getTarifaNoche()) %>
                                </td>
                            </tr>
                            <tr>
                                <th>Huespedes</th>
                                <td>
                                    <%= reserva.getAdultos() %> adulto(s)
<%
    if (reserva.getNinos() > 0) {
%>
                                    + <%= reserva.getNinos() %> nino(s)
<%
    }
%>
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>

<%
    if (reserva.getFechaCheckin() != null) {
%>
                <div class="alert alert-light border mt-3 mb-0 py-2" style="font-size:.82rem;">
                    <i class="bi bi-clock-history me-1"></i>
                    Check-in: <strong><%= Vista.txt(reserva.getFechaCheckin()) %></strong>
<%
        if (reserva.getFechaCheckout() != null) {
%>
                    &nbsp;&middot;&nbsp;
                    Check-out: <strong><%= Vista.txt(reserva.getFechaCheckout()) %></strong>
<%
        }
%>
                </div>
<%
    }

    if (reserva.getObservaciones() != null && !reserva.getObservaciones().isEmpty()) {
%>
                <div class="alert alert-warning mt-3 mb-0 py-2" style="font-size:.82rem;">
                    <i class="bi bi-sticky me-1"></i>
                    <%= Vista.esc(reserva.getObservaciones()) %>
                </div>
<%
    }
%>
            </div>
        </div>
    </div>

    <%-- ------------------------------------ Estado de cuenta --%>
    <div class="col-lg-5">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-calculator me-2"></i> Estado de cuenta
            </div>
            <div class="card-body">
<%
    if (cuenta != null) {
%>
                <table class="table table-sm">
                    <tr>
                        <td>Hospedaje (<%= cuenta.getNoches() %> noches)</td>
                        <td class="numero-tabla">
                            <%= Vista.moneda(cuenta.getTotalHospedaje()) %>
                        </td>
                    </tr>
                    <tr>
                        <td>Consumos y gastos</td>
                        <td class="numero-tabla">
                            <%= Vista.moneda(cuenta.getTotalConsumos()) %>
                        </td>
                    </tr>
                    <tr class="border-top">
                        <td class="fw-semibold">Total cargado</td>
                        <td class="numero-tabla fw-semibold">
                            <%= Vista.moneda(cuenta.getTotalCargado()) %>
                        </td>
                    </tr>
                    <tr>
                        <td class="text-success">Pagado</td>
                        <td class="numero-tabla text-success">
                            - <%= Vista.moneda(cuenta.getTotalPagado()) %>
                        </td>
                    </tr>
                    <tr class="border-top">
                        <td class="fw-bold">
                            <%= cuenta.isConSaldoAFavor() ? "Saldo a favor" : "Saldo pendiente" %>
                        </td>
                        <td class="numero-tabla fw-bold fs-5 text-<%= cuenta.getColorSaldo() %>">
                            Q <%= cuenta.isConSaldoAFavor()
                                    ? Vista.moneda(cuenta.getSaldoAFavor())
                                    : Vista.moneda(cuenta.getSaldoPendiente()) %>
                        </td>
                    </tr>
                </table>

                <div class="progress mb-2" style="height:8px;">
                    <div class="progress-bar bg-<%= cuenta.getColorSaldo() %>"
                         style="width:<%= cuenta.getPorcentajePagado() %>%"></div>
                </div>
                <div class="text-muted text-center" style="font-size:.76rem;">
                    <%= cuenta.getPorcentajePagado() %>% de la cuenta cubierto
                </div>

<%
        if (hayPendientes) {
%>
                <div class="alert alert-info mt-3 mb-0 py-2" style="font-size:.8rem;">
                    <i class="bi bi-receipt me-1"></i>
                    Hay Q <%= Vista.moneda(consumosPendientes) %>
                    en consumos pendientes de facturar.
                </div>
<%
        }
    }
%>
            </div>
        </div>
    </div>
</div>

<%-- ==================================== Pestanas de detalle --%>
<div class="card">
    <div class="card-header p-0">
        <ul class="nav nav-tabs card-header-tabs m-0 px-2 pt-2" role="tablist">
            <li class="nav-item">
                <button class="nav-link active" data-bs-toggle="tab"
                        data-bs-target="#tabOcupantes" type="button">
                    <i class="bi bi-people me-1"></i> Ocupantes
                    <span class="badge bg-secondary"><%= ocupantes.size() %></span>
                </button>
            </li>
            <li class="nav-item">
                <button class="nav-link" data-bs-toggle="tab"
                        data-bs-target="#tabCargos" type="button">
                    <i class="bi bi-cart me-1"></i> Consumos
                    <span class="badge bg-secondary"><%= cargos.size() %></span>
                </button>
            </li>
            <li class="nav-item">
                <button class="nav-link" data-bs-toggle="tab"
                        data-bs-target="#tabPagos" type="button">
                    <i class="bi bi-cash-coin me-1"></i> Pagos
                    <span class="badge bg-secondary"><%= pagos.size() %></span>
                </button>
            </li>
            <li class="nav-item">
                <button class="nav-link" data-bs-toggle="tab"
                        data-bs-target="#tabFacturas" type="button">
                    <i class="bi bi-receipt me-1"></i> Facturas
                    <span class="badge bg-secondary"><%= facturas.size() %></span>
                </button>
            </li>
        </ul>
    </div>

    <div class="tab-content">

        <%-- ------------------------------------- Ocupantes --%>
        <div class="tab-pane fade show active p-3" id="tabOcupantes">

<%
    if (puedeOperar && !cancelada && !finalizada) {
%>
            <form method="post" action="<%= ctx %>/ocupantes" class="row g-2 align-items-end mb-3">
                <input type="hidden" name="idReserva" value="<%= reserva.getIdReserva() %>">

                <div class="col-md-5">
                    <label class="form-label">Nombre completo del ocupante</label>
                    <input type="text" class="form-control form-control-sm"
                           name="nombreCompleto" maxlength="150" required>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Documento</label>
                    <input type="text" class="form-control form-control-sm"
                           name="documento" maxlength="25">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Edad</label>
                    <input type="number" class="form-control form-control-sm"
                           name="edad" min="0" max="120">
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-sm btn-hotel w-100">
                        <i class="bi bi-person-plus me-1"></i> Agregar
                    </button>
                </div>
            </form>
<%
    }

    if (ocupantes.isEmpty()) {
%>
            <p class="text-muted text-center py-4 mb-0">
                Aun no se han registrado los ocupantes de esta habitacion.
            </p>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead>
                        <tr>
                            <th>Ocupante</th>
                            <th>Documento</th>
                            <th>Edad</th>
                            <th class="text-end"></th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Ocupante o : ocupantes) {
%>
                        <tr>
                            <td>
                                <div class="d-flex align-items-center gap-2">
                                    <span class="avatar chico"><%= Vista.esc(o.getIniciales()) %></span>
                                    <div>
                                        <%= Vista.esc(o.getNombreCompleto()) %>
<%
            if (o.isEsTitular()) {
%>
                                        <span class="badge bg-oro"
                                              style="background-color:var(--hotel-oro);
                                                     color:var(--hotel-noche);
                                                     font-size:.6rem;">
                                            Titular
                                        </span>
<%
            }
%>
                                    </div>
                                </div>
                            </td>
                            <td style="font-size:.84rem;">
                                <%= Vista.esc(o.getDocumento()) %>
                            </td>
                            <td style="font-size:.84rem;">
                                <%= Vista.esc(o.getEdad()) %>
<%
            if (o.isMenorDeEdad()) {
%>
                                <span class="badge bg-info-subtle text-info-emphasis"
                                      style="font-size:.6rem;">Menor</span>
<%
            }
%>
                            </td>
                            <td class="text-end">
<%
            if (puedeOperar) {
%>
                                <a class="btn btn-sm btn-outline-danger"
                                   href="<%= ctx %>/ocupantes?accion=eliminar&id=<%= o.getIdOcupante() %>"
                                   onclick="return confirmar('Quitar a <%= Vista.esc(o.getNombreCompleto()) %> de la lista de ocupantes?');">
                                    <i class="bi bi-trash"></i>
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

        <%-- --------------------------------------- Consumos --%>
        <div class="tab-pane fade p-3" id="tabCargos">

<%
    if (puedeOperar && reserva.isAdmiteConsumos()) {
%>
            <a href="<%= ctx %>/cargos?idReserva=<%= reserva.getIdReserva() %>"
               class="btn btn-sm btn-hotel mb-3">
                <i class="bi bi-plus-lg me-1"></i> Anotar consumo
            </a>
<%
    }

    if (cargos.isEmpty()) {
%>
            <p class="text-muted text-center py-4 mb-0">
                Esta reserva no tiene consumos registrados.
            </p>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead>
                        <tr>
                            <th>Concepto</th>
                            <th>Categoria</th>
                            <th>A nombre de</th>
                            <th class="numero-tabla">Cant.</th>
                            <th class="numero-tabla">P. unit.</th>
                            <th class="numero-tabla">Monto</th>
                            <th>Estado</th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Cargo cg : cargos) {
%>
                        <tr>
                            <td>
                                <i class="bi bi-<%= cg.getIconoCategoria() %> text-muted me-1"></i>
                                <%= Vista.esc(cg.getConcepto()) %>
                                <div class="text-muted" style="font-size:.7rem;">
                                    <%= Vista.txt(cg.getFecha()) %>
                                </div>
                            </td>
                            <td>
                                <span class="badge-suave"><%= Vista.esc(cg.getCategoria()) %></span>
                            </td>
                            <td style="font-size:.82rem;">
<%
            if (cg.getNombreOcupante() == null || cg.getNombreOcupante().isEmpty()) {
%>
                                <span class="text-muted">La habitacion</span>
<%
            } else {
%>
                                <%= Vista.esc(cg.getNombreOcupante()) %>
<%
            }
%>
                            </td>
                            <td class="numero-tabla">
                                <%= Vista.cantidad(cg.getCantidad()) %>
                            </td>
                            <td class="numero-tabla">
                                <%= Vista.moneda(cg.getPrecioUnitario()) %>
                            </td>
                            <td class="numero-tabla fw-semibold">
                                <%= Vista.moneda(cg.getMonto()) %>
                            </td>
                            <td>
<%
            if (cg.isPendienteDeFacturar()) {
%>
                                <span class="badge bg-warning-subtle text-warning-emphasis">
                                    Por facturar
                                </span>
<%
            } else {
%>
                                <span class="badge bg-success-subtle text-success-emphasis">
                                    Facturado
                                </span>
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

        <%-- ------------------------------------------ Pagos --%>
        <div class="tab-pane fade p-3" id="tabPagos">

<%
    if (puedeOperar) {
%>
            <a href="<%= ctx %>/pagos?accion=nuevo&idReserva=<%= reserva.getIdReserva() %>"
               class="btn btn-sm btn-hotel mb-3">
                <i class="bi bi-plus-lg me-1"></i> Registrar movimiento
            </a>
<%
    }

    if (pagos.isEmpty()) {
%>
            <p class="text-muted text-center py-4 mb-0">
                Esta reserva no tiene pagos registrados.
            </p>
<%
    } else {
%>
            <ul class="linea-tiempo">
<%
        for (Pago p : pagos) {
%>
                <li>
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <span class="badge bg-<%= p.getColorTipo() %>">
                                <%= Vista.esc(p.getTipo()) %>
                            </span>
                            <span class="ms-1" style="font-size:.84rem;">
                                <i class="bi bi-<%= p.getIconoMetodo() %> text-muted me-1"></i>
                                <%= Vista.esc(p.getMetodo()) %>
                            </span>
<%
            if (p.getReferencia() != null && !p.getReferencia().isEmpty()) {
%>
                            <span class="text-muted" style="font-size:.76rem;">
                                &middot; ref. <%= Vista.esc(p.getReferencia()) %>
                            </span>
<%
            }
%>
                            <div class="text-muted" style="font-size:.72rem;">
                                <%= Vista.txt(p.getFecha()) %> &middot;
                                <%= Vista.esc(p.getNombreUsuario()) %>
                            </div>
                        </div>

                        <div class="text-end">
                            <div class="fw-bold <%= p.isSalida() ? "text-danger" : "text-success" %>">
                                <%= p.isSalida() ? "-" : "+" %>
                                Q <%= Vista.moneda(p.getMonto()) %>
                            </div>
<%
            if (esAdmin) {
%>
                            <a class="text-danger" style="font-size:.7rem;"
                               href="<%= ctx %>/pagos?accion=eliminar&id=<%= p.getIdPago() %>"
                               onclick="return confirmar('Se eliminara este movimiento de la caja. Continuar?');">
                                Eliminar
                            </a>
<%
            }
%>
                        </div>
                    </div>
                </li>
<%
        }
%>
            </ul>
<%
    }
%>
        </div>

        <%-- --------------------------------------- Facturas --%>
        <div class="tab-pane fade p-3" id="tabFacturas">

<%
    if (puedeOperar && !cancelada) {
%>
            <a href="<%= ctx %>/facturas?accion=nueva&idReserva=<%= reserva.getIdReserva() %>"
               class="btn btn-sm btn-hotel mb-3">
                <i class="bi bi-plus-lg me-1"></i> Emitir factura
            </a>
<%
    }

    if (facturas.isEmpty()) {
%>
            <p class="text-muted text-center py-4 mb-0">
                Esta reserva aun no tiene facturas emitidas.
            </p>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead>
                        <tr>
                            <th>Documento</th>
                            <th>Tipo</th>
                            <th>Receptor</th>
                            <th class="numero-tabla">Total</th>
                            <th>Estado</th>
                            <th class="text-end"></th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Factura f : facturas) {
%>
                        <tr>
                            <td>
                                <span class="badge-suave"><%= Vista.esc(f.getNumeroCompleto()) %></span>
                                <div class="text-muted" style="font-size:.7rem;">
                                    <%= Vista.txt(f.getFechaEmision()) %>
                                </div>
                            </td>
                            <td>
                                <div style="font-size:.84rem;"><%= Vista.esc(f.getTipo()) %></div>
                                <div class="text-muted" style="font-size:.68rem;">
                                    <%= Vista.esc(f.getDescripcionTipo()) %>
                                </div>
                            </td>
                            <td style="font-size:.84rem;">
                                <%= Vista.esc(f.getNombreReceptor()) %>
                                <div class="text-muted" style="font-size:.7rem;">
                                    NIT <%= Vista.esc(f.getNitReceptor()) %>
                                </div>
                            </td>
                            <td class="numero-tabla fw-semibold">
                                <%= Vista.moneda(f.getTotal()) %>
                            </td>
                            <td>
                                <span class="badge bg-<%= f.isVigente() ? "success" : "danger" %>">
                                    <%= Vista.esc(f.getEstado()) %>
                                </span>
                            </td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-outline-secondary"
                                   href="<%= ctx %>/facturas?accion=ver&id=<%= f.getIdFactura() %>">
                                    <i class="bi bi-eye"></i>
                                </a>
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
