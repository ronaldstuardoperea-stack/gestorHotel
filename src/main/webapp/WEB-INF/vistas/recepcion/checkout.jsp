<%--
    recepcion/checkout.jsp

    Pantalla de salida del huesped. Lo importante aqui es el saldo: el
    recepcionista tiene que verlo antes de dejar salir a nadie, y el
    sistema se lo pone al frente con su desglose completo.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.EstadoCuenta" %>
<%@ page import="com.hotel.modelo.Cargo" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Check-out" />
    <jsp:param name="subtitulo" value="Liquidacion de la cuenta y salida" />
    <jsp:param name="activo" value="recepcion" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Reserva reserva = (Reserva) request.getAttribute("reserva");
    EstadoCuenta cuenta = (EstadoCuenta) request.getAttribute("cuenta");

    List<Cargo> cargos = (List<Cargo>) request.getAttribute("cargos");
    if (cargos == null) { cargos = new ArrayList<Cargo>(); }

    BigDecimal consumosPendientes = (BigDecimal) request.getAttribute("consumosPendientes");
    boolean hayPendientes = (consumosPendientes != null
                             && consumosPendientes.compareTo(BigDecimal.ZERO) > 0);

    String colorSaldo = cuenta.getColorSaldo();
%>

<div class="row justify-content-center">
    <div class="col-lg-10">

        <div class="row g-3 mb-3">

            <%-- ------------------------------- Datos de salida --%>
            <div class="col-lg-6">
                <div class="card h-100">
                    <div class="card-header">
                        <i class="bi bi-box-arrow-right text-primary me-2"></i>
                        Salida de <%= Vista.esc(reserva.getCodigo()) %>
                    </div>
                    <div class="card-body">
                        <table class="table table-sm mb-0">
                            <tr>
                                <th style="width:42%;">Huesped</th>
                                <td><%= Vista.esc(reserva.getNombreCliente()) %></td>
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
                            <tr>
                                <th>Estadia</th>
                                <td>
                                    <%= Vista.txt(reserva.getFechaEntrada()) %> &rarr;
                                    <%= Vista.txt(reserva.getFechaSalida()) %>
                                    <div class="text-muted" style="font-size:.76rem;">
                                        <%= reserva.getNoches() %> noche(s)
                                    </div>
                                </td>
                            </tr>
                            <tr>
                                <th>Check-in</th>
                                <td><%= Vista.txt(reserva.getFechaCheckin()) %></td>
                            </tr>
                        </table>

<%
    if (reserva.isVencida()) {
%>
                        <div class="alert alert-danger mt-3 mb-0 py-2" style="font-size:.84rem;">
                            <i class="bi bi-exclamation-triangle me-1"></i>
                            Este huesped debio salir el <%= Vista.txt(reserva.getFechaSalida()) %>.
                            Verifique si corresponde cobrar noches adicionales
                            antes de cerrar la cuenta.
                        </div>
<%
    }
%>
                    </div>
                </div>
            </div>

            <%-- ------------------------------ Liquidacion --%>
            <div class="col-lg-6">
                <div class="card h-100 border-<%= colorSaldo %>">
                    <div class="card-header bg-<%= colorSaldo %>-subtle">
                        <i class="bi bi-calculator me-2"></i> Liquidacion de la cuenta
                    </div>
                    <div class="card-body">

                        <table class="table table-sm">
                            <tr>
                                <td>Hospedaje (<%= cuenta.getNoches() %> x
                                    Q <%= Vista.moneda(cuenta.getTarifaNoche()) %>)</td>
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
                                <td class="fw-semibold">Total de la cuenta</td>
                                <td class="numero-tabla fw-semibold">
                                    <%= Vista.moneda(cuenta.getTotalCargado()) %>
                                </td>
                            </tr>
                            <tr>
                                <td class="text-success">Anticipos y pagos</td>
                                <td class="numero-tabla text-success">
                                    - <%= Vista.moneda(cuenta.getTotalPagado()) %>
                                </td>
                            </tr>
                        </table>

                        <div class="alert alert-<%= colorSaldo %> mb-3 text-center">
                            <div style="font-size:.78rem; text-transform:uppercase;
                                 letter-spacing:.08em;">
<%
    if (cuenta.isSaldada() && !cuenta.isConSaldoAFavor()) {
%>
                                Cuenta saldada
<%
    } else if (cuenta.isConSaldoAFavor()) {
%>
                                Saldo a favor del cliente
<%
    } else {
%>
                                Saldo por cobrar
<%
    }
%>
                            </div>
                            <div class="fs-3 fw-bold">
                                Q <%= cuenta.isConSaldoAFavor()
                                        ? Vista.moneda(cuenta.getSaldoAFavor())
                                        : Vista.moneda(cuenta.getSaldoPendiente()) %>
                            </div>
                        </div>

                        <div class="d-grid gap-2">
<%
    if (!cuenta.isSaldada()) {
%>
                            <a href="<%= ctx %>/pagos?accion=nuevo&idReserva=<%= reserva.getIdReserva() %>"
                               class="btn btn-sm btn-oro">
                                <i class="bi bi-cash-coin me-1"></i> Cobrar el saldo
                            </a>
<%
    }
    if (cuenta.isConSaldoAFavor()) {
%>
                            <a href="<%= ctx %>/pagos?accion=nuevo&idReserva=<%= reserva.getIdReserva() %>"
                               class="btn btn-sm btn-outline-danger">
                                <i class="bi bi-arrow-return-left me-1"></i>
                                Registrar reembolso
                            </a>
<%
    }
%>
                            <a href="<%= ctx %>/facturas?accion=nueva&idReserva=<%= reserva.getIdReserva() %>"
                               class="btn btn-sm btn-outline-dark">
                                <i class="bi bi-receipt me-1"></i> Emitir factura
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <%-- ------------------------------------- Consumos --%>
<%
    if (!cargos.isEmpty()) {
%>
        <div class="card mb-3">
            <div class="card-header">
                <i class="bi bi-cart me-2"></i> Detalle de consumos
            </div>
            <div class="table-responsive">
                <table class="table table-sm table-hover mb-0">
                    <thead>
                        <tr>
                            <th>Concepto</th>
                            <th>A nombre de</th>
                            <th>Fecha</th>
                            <th class="numero-tabla">Monto</th>
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
                            <td class="text-muted" style="font-size:.78rem;">
                                <%= Vista.txt(cg.getFecha()) %>
                            </td>
                            <td class="numero-tabla">
                                <%= Vista.moneda(cg.getMonto()) %>
                            </td>
                        </tr>
<%
        }
%>
                    </tbody>
                </table>
            </div>
        </div>
<%
    }

    if (hayPendientes) {
%>
        <div class="alert alert-info d-flex align-items-start gap-2">
            <i class="bi bi-receipt mt-1"></i>
            <div>
                Quedan <strong>Q <%= Vista.moneda(consumosPendientes) %></strong>
                en consumos sin facturar. Puede emitir la factura antes o
                despues del check-out; la cuenta queda igual en ambos casos.
            </div>
        </div>
<%
    }
%>

        <%-- El sistema no impide salir con saldo: hay empresas con
             credito. Solo lo advierte y deja la decision a recepcion. --%>
        <form method="post" action="<%= ctx %>/recepcion"
              onsubmit="return confirmar('Se registrara la salida y la habitacion <%= Vista.esc(reserva.getNumeroHabitacion()) %> pasara a limpieza. Continuar?');">
            <input type="hidden" name="accion" value="checkout">
            <input type="hidden" name="idReserva" value="<%= reserva.getIdReserva() %>">

            <div class="d-flex gap-2 justify-content-end">
                <a href="<%= ctx %>/recepcion" class="btn btn-outline-secondary">Cancelar</a>
                <button type="submit" class="btn btn-lg
                        <%= cuenta.isSaldada() ? "btn-oro" : "btn-outline-danger" %>">
                    <i class="bi bi-box-arrow-right me-1"></i>
                    <%= cuenta.isSaldada()
                          ? "Confirmar check-out"
                          : "Dar salida con saldo pendiente" %>
                </button>
            </div>
        </form>
    </div>
</div>

<jsp:include page="../comun/pie.jsp" />
