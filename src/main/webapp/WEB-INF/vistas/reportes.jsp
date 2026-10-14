<%--
    reportes.jsp

    Reportes gerenciales del hotel: ingresos del periodo, de donde
    vienen, estado del inventario, cuentas por cobrar y ranking de
    clientes.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.hotel.modelo.EstadoCuenta" %>
<%@ page import="com.hotel.modelo.HistorialCliente" %>
<%@ page import="com.hotel.util.Vista" %>

<%!
    /* Ancho de la barra en porcentaje, tomando el mayor valor como
       referencia. Devuelve 0 cuando no hay nada que comparar. */
    private String barra(BigDecimal valor, BigDecimal maximo) {
        if (valor == null || maximo == null
                || maximo.compareTo(BigDecimal.ZERO) <= 0) {
            return "0";
        }
        return String.valueOf(
                valor.multiply(new BigDecimal(100))
                     .divide(maximo, 2, java.math.RoundingMode.HALF_UP));
    }

    /* Mayor valor de un mapa de totales. */
    private BigDecimal mayor(Map<String, BigDecimal> totales) {
        BigDecimal maximo = BigDecimal.ZERO;
        for (BigDecimal v : totales.values()) {
            if (v != null && v.compareTo(maximo) > 0) { maximo = v; }
        }
        return maximo;
    }
%>

<jsp:include page="comun/cabecera.jsp">
    <jsp:param name="titulo" value="Reportes" />
    <jsp:param name="subtitulo" value="Indicadores de operacion y ventas" />
    <jsp:param name="activo" value="reportes" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    LocalDate desde = (LocalDate) request.getAttribute("desde");
    LocalDate hasta = (LocalDate) request.getAttribute("hasta");

    BigDecimal ingresos  = (BigDecimal) request.getAttribute("ingresos");
    BigDecimal facturado = (BigDecimal) request.getAttribute("facturado");

    Integer facturasObj = (Integer) request.getAttribute("facturasEmitidas");
    int facturasEmitidas = (facturasObj == null) ? 0 : facturasObj.intValue();

    Integer ocupacionObj = (Integer) request.getAttribute("ocupacion");
    int ocupacion = (ocupacionObj == null) ? 0 : ocupacionObj.intValue();

    Map<String, BigDecimal> porCategoria =
            (Map<String, BigDecimal>) request.getAttribute("porCategoria");
    if (porCategoria == null) { porCategoria = new LinkedHashMap<String, BigDecimal>(); }

    Map<String, BigDecimal> porMetodo =
            (Map<String, BigDecimal>) request.getAttribute("porMetodo");
    if (porMetodo == null) { porMetodo = new LinkedHashMap<String, BigDecimal>(); }

    Map<String, Integer> habitacionesPorEstado =
            (Map<String, Integer>) request.getAttribute("habitacionesPorEstado");
    if (habitacionesPorEstado == null) {
        habitacionesPorEstado = new LinkedHashMap<String, Integer>();
    }

    Map<String, Integer> reservasPorEstado =
            (Map<String, Integer>) request.getAttribute("reservasPorEstado");
    if (reservasPorEstado == null) {
        reservasPorEstado = new LinkedHashMap<String, Integer>();
    }

    List<EstadoCuenta> cuentasPendientes =
            (List<EstadoCuenta>) request.getAttribute("cuentasPendientes");
    if (cuentasPendientes == null) { cuentasPendientes = new ArrayList<EstadoCuenta>(); }

    List<HistorialCliente> mejoresClientes =
            (List<HistorialCliente>) request.getAttribute("mejoresClientes");
    if (mejoresClientes == null) { mejoresClientes = new ArrayList<HistorialCliente>(); }

    BigDecimal maxCategoria = mayor(porCategoria);
    BigDecimal maxMetodo    = mayor(porMetodo);
%>

<%-- ========================================== Periodo --%>
<div class="card mb-3 sin-imprimir">
    <div class="card-body py-3">
        <form method="get" action="<%= ctx %>/reportes" class="row g-2 align-items-end">
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
            <div class="col-md-6 d-flex gap-2">
                <button type="submit" class="btn btn-sm btn-hotel">
                    <i class="bi bi-funnel me-1"></i> Generar reporte
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary"
                        onclick="window.print()">
                    <i class="bi bi-printer me-1"></i> Imprimir
                </button>
            </div>
        </form>
    </div>
</div>

<div class="alert alert-light border" style="font-size:.85rem;">
    Periodo analizado: <strong><%= Vista.txt(desde) %></strong>
    al <strong><%= Vista.txt(hasta) %></strong>
</div>

<%-- ======================================== Indicadores --%>
<div class="row g-3 mb-4">
    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-cash-stack icono"></i>
            <div class="etiqueta">Dinero cobrado</div>
            <div class="valor">
                <span class="fs-6 text-muted">Q</span>
                <%= Vista.moneda(ingresos) %>
            </div>
            <div class="nota mt-1">Pagos recibidos en el periodo</div>
        </div>
    </div>

    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-receipt icono"></i>
            <div class="etiqueta">Facturado</div>
            <div class="valor">
                <span class="fs-6 text-muted">Q</span>
                <%= Vista.moneda(facturado) %>
            </div>
            <div class="nota mt-1"><%= facturasEmitidas %> documentos emitidos</div>
        </div>
    </div>

    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-pie-chart-fill icono"></i>
            <div class="etiqueta">Ocupacion actual</div>
            <div class="valor"><%= ocupacion %><span class="fs-5">%</span></div>
            <div class="progress mt-2" style="height:6px;">
                <div class="progress-bar bg-success" style="width:<%= ocupacion %>%"></div>
            </div>
        </div>
    </div>

    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-exclamation-circle icono"></i>
            <div class="etiqueta">Cuentas por cobrar</div>
            <div class="valor text-danger"><%= cuentasPendientes.size() %></div>
            <div class="nota mt-1">Reservas con saldo pendiente</div>
        </div>
    </div>
</div>

<div class="row g-3 mb-3">

    <%-- --------------------------- Ingresos por categoria --%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-bar-chart me-2"></i>
                De donde vienen los consumos
            </div>
            <div class="card-body">
<%
    if (porCategoria.isEmpty()) {
%>
                <p class="text-muted text-center mb-0 py-4">
                    No hubo consumos registrados en este periodo.
                </p>
<%
    } else {
        for (Map.Entry<String, BigDecimal> cat : porCategoria.entrySet()) {
%>
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-1">
                        <span style="font-size:.84rem;"><%= Vista.esc(cat.getKey()) %></span>
                        <strong style="font-size:.84rem;">
                            Q <%= Vista.moneda(cat.getValue()) %>
                        </strong>
                    </div>
                    <div class="progress" style="height:8px;">
                        <div class="progress-bar"
                             style="width:<%= barra(cat.getValue(), maxCategoria) %>%;
                                    background-color:var(--hotel-azul);"></div>
                    </div>
                </div>
<%
        }
    }
%>
            </div>
        </div>
    </div>

    <%-- ----------------------------- Ingresos por metodo --%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-wallet2 me-2"></i>
                Como pagaron los huespedes
            </div>
            <div class="card-body">
<%
    if (porMetodo.isEmpty()) {
%>
                <p class="text-muted text-center mb-0 py-4">
                    No hubo movimientos de dinero en este periodo.
                </p>
<%
    } else {
        for (Map.Entry<String, BigDecimal> m : porMetodo.entrySet()) {
%>
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-1">
                        <span style="font-size:.84rem;"><%= Vista.esc(m.getKey()) %></span>
                        <strong style="font-size:.84rem;">
                            Q <%= Vista.moneda(m.getValue()) %>
                        </strong>
                    </div>
                    <div class="progress" style="height:8px;">
                        <div class="progress-bar bg-success"
                             style="width:<%= barra(m.getValue(), maxMetodo) %>%"></div>
                    </div>
                </div>
<%
        }
    }
%>
            </div>
        </div>
    </div>
</div>

<div class="row g-3 mb-3">

    <%-- ------------------------------ Estado del inventario --%>
    <div class="col-lg-5">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-house-door me-2"></i> Estado del inventario
            </div>
            <div class="card-body">
                <table class="table table-sm mb-3">
<%
    for (Map.Entry<String, Integer> e : habitacionesPorEstado.entrySet()) {
%>
                    <tr>
                        <td style="font-size:.84rem;"><%= Vista.esc(e.getKey()) %></td>
                        <td class="numero-tabla fw-semibold"><%= e.getValue() %></td>
                    </tr>
<%
    }
%>
                </table>

                <div class="text-muted mb-2" style="font-size:.7rem; font-weight:700;
                     letter-spacing:.1em; text-transform:uppercase;">
                    Reservas por estado
                </div>
                <table class="table table-sm mb-0">
<%
    for (Map.Entry<String, Integer> e : reservasPorEstado.entrySet()) {
%>
                    <tr>
                        <td style="font-size:.84rem;"><%= Vista.esc(e.getKey()) %></td>
                        <td class="numero-tabla fw-semibold"><%= e.getValue() %></td>
                    </tr>
<%
    }
%>
                </table>
            </div>
        </div>
    </div>

    <%-- ---------------------------------- Cuentas por cobrar --%>
    <div class="col-lg-7">
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
            <div class="table-responsive" style="max-height:420px; overflow-y:auto;">
                <table class="table table-hover align-middle mb-0">
                    <thead>
                        <tr>
                            <th>Reserva</th>
                            <th>Cliente</th>
                            <th>Estado</th>
                            <th class="numero-tabla">Saldo</th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (EstadoCuenta cta : cuentasPendientes) {
%>
                        <tr onclick="location.href='<%= ctx %>/reservas?accion=detalle&id=<%= cta.getIdReserva() %>'"
                            style="cursor:pointer;">
                            <td><span class="badge-suave"><%= Vista.esc(cta.getCodigo()) %></span></td>
                            <td style="font-size:.84rem;">
                                <%= Vista.esc(cta.getCliente()) %>
                            </td>
                            <td style="font-size:.78rem;"><%= Vista.esc(cta.getEstado()) %></td>
                            <td class="numero-tabla fw-bold text-danger">
                                <%= Vista.moneda(cta.getSaldoPendiente()) %>
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

<%-- ==================================== Ranking de clientes --%>
<div class="card">
    <div class="card-header">
        <i class="bi bi-trophy me-2"></i> Clientes por consumo historico
    </div>

<%
    if (mejoresClientes.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        Aun no hay clientes con movimientos registrados.
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>#</th>
                    <th>Cliente</th>
                    <th>Categoria</th>
                    <th class="numero-tabla">Visitas</th>
                    <th class="numero-tabla">Noches</th>
                    <th class="numero-tabla">Consumo total</th>
                    <th class="numero-tabla">Promedio/noche</th>
                    <th class="numero-tabla">Saldo</th>
                </tr>
            </thead>
            <tbody>
<%
        int fila = 0;
        for (HistorialCliente h : mejoresClientes) {
            fila++;
%>
                <tr onclick="location.href='<%= ctx %>/clientes?accion=historial&id=<%= h.getIdCliente() %>'"
                    style="cursor:pointer;">
                    <td class="text-muted"><%= fila %></td>
                    <td>
                        <div class="fw-semibold" style="font-size:.85rem;">
                            <%= Vista.esc(h.getCliente()) %>
                        </div>
                        <div class="text-muted" style="font-size:.7rem;">
                            <%= Vista.esc(h.getDocumento()) %>
                        </div>
                    </td>
                    <td>
                        <span class="badge bg-<%= h.getColorCategoria() %>">
                            <%= Vista.esc(h.getCategoria()) %>
                        </span>
                    </td>
                    <td class="numero-tabla"><%= h.getVisitasCompletadas() %></td>
                    <td class="numero-tabla"><%= h.getNochesTotales() %></td>
                    <td class="numero-tabla fw-semibold">
                        <%= Vista.moneda(h.getConsumoHistorico()) %>
                    </td>
                    <td class="numero-tabla">
                        <%= Vista.moneda(h.getPromedioPorNoche()) %>
                    </td>
                    <td class="numero-tabla <%= h.isConDeuda()
                            ? "text-danger fw-semibold" : "text-muted" %>">
                        <%= Vista.moneda(h.getSaldoActual()) %>
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

<jsp:include page="comun/pie.jsp" />
