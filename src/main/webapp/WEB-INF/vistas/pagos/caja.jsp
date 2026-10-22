<%--
    pagos/caja.jsp

    Corte de caja de un periodo y listado de cuentas por cobrar.
    Es la pantalla que se usa al cerrar el turno: dice cuanto debe haber
    en efectivo, cuanto entro por tarjeta y a quien falta cobrarle.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.hotel.modelo.Pago" %>
<%@ page import="com.hotel.modelo.EstadoCuenta" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Caja y pagos" />
    <jsp:param name="subtitulo" value="Corte de caja y cuentas por cobrar" />
    <jsp:param name="activo" value="pagos" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    List<Pago> pagos = (List<Pago>) request.getAttribute("pagos");
    if (pagos == null) { pagos = new ArrayList<Pago>(); }

    Map<String, BigDecimal> porMetodo =
            (Map<String, BigDecimal>) request.getAttribute("porMetodo");
    if (porMetodo == null) { porMetodo = new LinkedHashMap<String, BigDecimal>(); }

    BigDecimal totalPeriodo = (BigDecimal) request.getAttribute("totalPeriodo");

    List<EstadoCuenta> cuentasPendientes =
            (List<EstadoCuenta>) request.getAttribute("cuentasPendientes");
    if (cuentasPendientes == null) { cuentasPendientes = new ArrayList<EstadoCuenta>(); }

    LocalDate desde = (LocalDate) request.getAttribute("desde");
    LocalDate hasta = (LocalDate) request.getAttribute("hasta");
%>

<%-- ========================================= Periodo --%>
<div class="card mb-3">
    <div class="card-body py-3">
        <form method="get" action="<%= ctx %>/pagos" class="row g-2 align-items-end">
            <input type="hidden" name="accion" value="caja">

            <div class="col-md-3">
                <label class="form-label">Desde</label>
                <input type="date" class="form-control form-control-sm" name="desde"
                       value="<%= Vista.txt(desde) %>">
            </div>
            <div class="col-md-3">
                <label class="form-label">Hasta</label>
                <input type="date" class="form-control form-control-sm" name="hasta"
                       value="<%= Vista.txt(hasta) %>">
            </div>
            <div class="col-md-6">
                <button type="submit" class="btn btn-sm btn-hotel">
                    <i class="bi bi-funnel me-1"></i> Ver periodo
                </button>
                <a href="<%= ctx %>/pagos" class="btn btn-sm btn-outline-secondary">
                    Solo hoy
                </a>
            </div>
        </form>
    </div>
</div>

<div class="row g-3 mb-3">

    <%-- ------------------------------- Total del periodo --%>
    <div class="col-lg-4">
        <div class="indicador h-100">
            <i class="bi bi-cash-stack icono"></i>
            <div class="etiqueta">Total cobrado en el periodo</div>
            <div class="valor">
                <span class="fs-6 text-muted">Q</span>
                <%= Vista.moneda(totalPeriodo) %>
            </div>
            <div class="nota mt-1">
                <%= pagos.size() %> movimiento(s) &middot;
                del <%= Vista.txt(desde) %> al <%= Vista.txt(hasta) %>
            </div>
        </div>
    </div>

    <%-- ---------------------------------- Desglose por metodo --%>
    <div class="col-lg-8">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-wallet2 me-2"></i> Desglose por metodo de pago
            </div>
            <div class="card-body">
<%
    if (porMetodo.isEmpty()) {
%>
                <p class="text-muted text-center mb-0 py-3">
                    No hubo movimientos de dinero en este periodo.
                </p>
<%
    } else {
%>
                <div class="row g-2">
<%
        for (Map.Entry<String, BigDecimal> m : porMetodo.entrySet()) {
%>
                    <div class="col-6 col-md-3">
                        <div class="border rounded-3 p-2 text-center h-100">
                            <div class="text-muted"
                                 style="font-size:.7rem; text-transform:uppercase;
                                        letter-spacing:.06em;">
                                <%= Vista.esc(m.getKey()) %>
                            </div>
                            <div class="fw-bold" style="color:var(--hotel-azul);">
                                Q <%= Vista.moneda(m.getValue()) %>
                            </div>
                        </div>
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
</div>

<div class="row g-3">

    <%-- ------------------------------ Movimientos del periodo --%>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-list-ul me-2"></i> Movimientos del periodo
            </div>

<%
    if (pagos.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-wallet2 fs-1 opacity-25 d-block mb-2"></i>
                No hay movimientos registrados en estas fechas.
            </div>
<%
    } else {
%>
            <div class="table-responsive" style="max-height:520px; overflow-y:auto;">
                <table class="table table-hover align-middle mb-0">
                    <thead>
                        <tr>
                            <th>Reserva</th>
                            <th>Cliente</th>
                            <th>Tipo</th>
                            <th>Metodo</th>
                            <th class="numero-tabla">Monto</th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Pago p : pagos) {
%>
                        <tr onclick="location.href='<%= ctx %>/reservas?accion=detalle&id=<%= p.getIdReserva() %>'"
                            style="cursor:pointer;">
                            <td>
                                <span class="badge-suave"><%= Vista.esc(p.getCodigoReserva()) %></span>
                                <div class="text-muted" style="font-size:.68rem;">
                                    <%= Vista.txt(p.getFecha()) %>
                                </div>
                            </td>
                            <td style="font-size:.82rem;">
                                <%= Vista.esc(p.getNombreCliente()) %>
                            </td>
                            <td>
                                <span class="badge bg-<%= p.getColorTipo() %>"
                                      style="font-size:.62rem;">
                                    <%= Vista.esc(p.getTipo()) %>
                                </span>
                            </td>
                            <td style="font-size:.8rem;">
                                <i class="bi bi-<%= p.getIconoMetodo() %> text-muted me-1"></i>
                                <%= Vista.esc(p.getMetodo()) %>
                            </td>
                            <td class="numero-tabla fw-semibold
                                       <%= p.isSalida() ? "text-danger" : "text-success" %>">
                                <%= p.isSalida() ? "-" : "" %>
                                <%= Vista.moneda(p.getMonto()) %>
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

    <%-- --------------------------------- Cuentas por cobrar --%>
    <div class="col-lg-5">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-exclamation-circle text-warning me-2"></i>
                Cuentas por cobrar
            </div>

<%
    if (cuentasPendientes.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-check2-circle fs-1 opacity-25 d-block mb-2"></i>
                No hay cuentas pendientes de cobro.
            </div>
<%
    } else {
%>
            <div class="table-responsive" style="max-height:520px; overflow-y:auto;">
                <table class="table table-hover align-middle mb-0">
                    <thead>
                        <tr>
                            <th>Reserva</th>
                            <th class="numero-tabla">Saldo</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (EstadoCuenta cta : cuentasPendientes) {
%>
                        <tr>
                            <td>
                                <div class="fw-semibold" style="font-size:.84rem;">
                                    <%= Vista.esc(cta.getCliente()) %>
                                </div>
                                <div class="text-muted" style="font-size:.7rem;">
                                    <%= Vista.esc(cta.getCodigo()) %> &middot;
                                    hab. <%= Vista.esc(cta.getHabitacion()) %>
                                    &middot; <%= Vista.esc(cta.getEstado()) %>
                                </div>
                            </td>
                            <td class="numero-tabla fw-bold text-danger">
                                <%= Vista.moneda(cta.getSaldoPendiente()) %>
                            </td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-outline-secondary"
                                   href="<%= ctx %>/pagos?accion=nuevo&idReserva=<%= cta.getIdReserva() %>"
                                   title="Registrar cobro">
                                    <i class="bi bi-cash-coin"></i>
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
