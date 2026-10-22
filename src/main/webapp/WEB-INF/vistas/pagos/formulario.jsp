<%--
    pagos/formulario.jsp

    Registro de anticipos, abonos, liquidaciones y reembolsos de una
    reserva. A la izquierda se captura el movimiento y a la derecha se ve
    el saldo y los movimientos anteriores, para no cobrar de mas.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.EstadoCuenta" %>
<%@ page import="com.hotel.modelo.Pago" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Registrar pago" />
    <jsp:param name="subtitulo" value="Anticipos, abonos y liquidacion de la cuenta" />
    <jsp:param name="activo" value="pagos" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Reserva reserva = (Reserva) request.getAttribute("reserva");
    EstadoCuenta cuenta = (EstadoCuenta) request.getAttribute("cuenta");

    List<Pago> pagos = (List<Pago>) request.getAttribute("pagos");
    if (pagos == null) { pagos = new ArrayList<Pago>(); }

    BigDecimal saldoPendiente = (cuenta == null) ? BigDecimal.ZERO : cuenta.getSaldoPendiente();
    boolean haySaldo = (saldoPendiente != null
                        && saldoPendiente.compareTo(BigDecimal.ZERO) > 0);
%>

<div class="mb-3">
    <a href="<%= ctx %>/reservas?accion=detalle&id=<%= reserva.getIdReserva() %>"
       class="btn btn-sm btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i>
        Volver a la reserva <%= Vista.esc(reserva.getCodigo()) %>
    </a>
</div>

<div class="row g-3">

    <%-- ------------------------------ Captura del movimiento --%>
    <div class="col-lg-5">
        <div class="card">
            <div class="card-header">
                <i class="bi bi-cash-coin me-2"></i> Nuevo movimiento
            </div>

            <form method="post" action="<%= ctx %>/pagos">
                <input type="hidden" name="idReserva" value="<%= reserva.getIdReserva() %>">

                <div class="card-body">

                    <div class="alert alert-light border py-2 mb-3" style="font-size:.84rem;">
                        <strong><%= Vista.esc(reserva.getNombreCliente()) %></strong><br>
                        Habitacion <%= Vista.esc(reserva.getNumeroHabitacion()) %> &middot;
                        <%= Vista.txt(reserva.getFechaEntrada()) %>
                        al <%= Vista.txt(reserva.getFechaSalida()) %>
                    </div>

                    <div class="mb-3">
                        <label for="tipo" class="form-label obligatorio">Tipo de movimiento</label>
                        <select class="form-select" id="tipo" name="tipo"
                                onchange="ajustarAyuda()">
                            <option value="ANTICIPO">Anticipo (antes de la llegada)</option>
                            <option value="ABONO" selected>Abono (pago parcial)</option>
                            <option value="LIQUIDACION">Liquidacion (salda la cuenta)</option>
                            <option value="REEMBOLSO">Reembolso (devolucion al cliente)</option>
                        </select>
                        <div class="form-text" id="ayudaTipo">
                            Pago parcial durante la estadia.
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="metodo" class="form-label obligatorio">Metodo</label>
                        <select class="form-select" id="metodo" name="metodo">
                            <option value="EFECTIVO" selected>Efectivo</option>
                            <option value="TARJETA">Tarjeta</option>
                            <option value="TRANSFERENCIA">Transferencia</option>
                            <option value="DEPOSITO">Deposito bancario</option>
                        </select>
                    </div>

                    <div class="mb-3">
                        <label for="monto" class="form-label obligatorio">Monto (Q)</label>
                        <input type="number" class="form-control form-control-lg" id="monto"
                               name="monto" step="0.01" min="0.01" required
                               placeholder="0.00">

<%
    if (haySaldo) {
%>
                        <div class="form-text">
                            <a href="javascript:void(0)"
                               onclick="document.getElementById('monto').value='<%= saldoPendiente %>'">
                                Usar el saldo pendiente
                                (Q <%= Vista.moneda(saldoPendiente) %>)
                            </a>
                        </div>
<%
    }
%>
                    </div>

                    <div class="mb-0">
                        <label for="referencia" class="form-label">Referencia</label>
                        <input type="text" class="form-control" id="referencia"
                               name="referencia" maxlength="80"
                               placeholder="Numero de boleta, autorizacion, ultimos 4 digitos...">
                    </div>
                </div>

                <div class="card-footer bg-white text-end">
                    <button type="submit" class="btn btn-oro">
                        <i class="bi bi-check-lg me-1"></i> Registrar movimiento
                    </button>
                </div>
            </form>
        </div>
    </div>

    <%-- --------------------------- Saldo y movimientos previos --%>
    <div class="col-lg-7">

        <div class="card mb-3">
            <div class="card-header">
                <i class="bi bi-calculator me-2"></i> Estado de cuenta
            </div>
            <div class="card-body">
                <div class="row g-2 text-center">
                    <div class="col-4">
                        <div class="text-muted" style="font-size:.72rem;">Total cargado</div>
                        <div class="fs-5 fw-bold">
                            Q <%= Vista.moneda(cuenta.getTotalCargado()) %>
                        </div>
                    </div>
                    <div class="col-4">
                        <div class="text-muted" style="font-size:.72rem;">Pagado</div>
                        <div class="fs-5 fw-bold text-success">
                            Q <%= Vista.moneda(cuenta.getTotalPagado()) %>
                        </div>
                    </div>
                    <div class="col-4">
                        <div class="text-muted" style="font-size:.72rem;">
                            <%= cuenta.isConSaldoAFavor() ? "A favor" : "Pendiente" %>
                        </div>
                        <div class="fs-5 fw-bold text-<%= cuenta.getColorSaldo() %>">
                            Q <%= cuenta.isConSaldoAFavor()
                                    ? Vista.moneda(cuenta.getSaldoAFavor())
                                    : Vista.moneda(cuenta.getSaldoPendiente()) %>
                        </div>
                    </div>
                </div>

                <div class="progress mt-3" style="height:8px;">
                    <div class="progress-bar bg-<%= cuenta.getColorSaldo() %>"
                         style="width:<%= cuenta.getPorcentajePagado() %>%"></div>
                </div>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <i class="bi bi-clock-history me-2"></i> Movimientos registrados
            </div>

<%
    if (pagos.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-wallet2 fs-1 opacity-25 d-block mb-2"></i>
                Esta reserva aun no tiene movimientos de dinero.
            </div>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead>
                        <tr>
                            <th>Tipo</th>
                            <th>Metodo</th>
                            <th>Referencia</th>
                            <th>Fecha</th>
                            <th class="numero-tabla">Monto</th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Pago p : pagos) {
%>
                        <tr>
                            <td>
                                <span class="badge bg-<%= p.getColorTipo() %>">
                                    <%= Vista.esc(p.getTipo()) %>
                                </span>
                            </td>
                            <td style="font-size:.84rem;">
                                <i class="bi bi-<%= p.getIconoMetodo() %> text-muted me-1"></i>
                                <%= Vista.esc(p.getMetodo()) %>
                            </td>
                            <td class="text-muted" style="font-size:.78rem;">
                                <%= Vista.esc(p.getReferencia()) %>
                            </td>
                            <td class="text-muted" style="font-size:.78rem;">
                                <%= Vista.txt(p.getFecha()) %>
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
</div>

<script>
    /* Explica en una linea que significa cada tipo de movimiento. */
    function ajustarAyuda() {
        var textos = {
            'ANTICIPO':    'Pago adelantado. Si la reserva esta pendiente, quedara confirmada.',
            'ABONO':       'Pago parcial durante la estadia.',
            'LIQUIDACION': 'Pago final que salda la cuenta al hacer check-out.',
            'REEMBOLSO':   'Devolucion de dinero al cliente. Resta del total pagado.'
        };
        var tipo = document.getElementById('tipo').value;
        document.getElementById('ayudaTipo').textContent = textos[tipo];
    }
</script>

<jsp:include page="../comun/pie.jsp" />
