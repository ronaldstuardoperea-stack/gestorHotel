<%--
    dashboard.jsp

    Tablero principal. Es la primera pantalla que ve el personal al
    entrar, asi que reune de un vistazo lo que necesita para empezar el
    turno: ocupacion, llegadas, salidas, dinero del dia y cuentas por
    cobrar.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.EstadoCuenta" %>
<%@ page import="com.hotel.util.Vista" %>

<%!
    /* Conteo de un estado, o cero si no viene en el mapa. */
    private int cuantas(Map<String, Integer> conteo, String estado) {
        Integer valor = conteo.get(estado);
        return (valor == null) ? 0 : valor.intValue();
    }
%>

<jsp:include page="comun/cabecera.jsp">
    <jsp:param name="titulo" value="Tablero" />
    <jsp:param name="subtitulo" value="Resumen de la operacion del hotel" />
    <jsp:param name="activo" value="inicio" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Map<String, Integer> habitacionesPorEstado =
            (Map<String, Integer>) request.getAttribute("habitacionesPorEstado");
    if (habitacionesPorEstado == null) {
        habitacionesPorEstado = new HashMap<String, Integer>();
    }

    Map<String, Integer> reservasPorEstado =
            (Map<String, Integer>) request.getAttribute("reservasPorEstado");
    if (reservasPorEstado == null) {
        reservasPorEstado = new HashMap<String, Integer>();
    }

    Integer ocupacionObj = (Integer) request.getAttribute("ocupacion");
    int ocupacion = (ocupacionObj == null) ? 0 : ocupacionObj.intValue();

    List<Reserva> llegadasHoy = (List<Reserva>) request.getAttribute("llegadasHoy");
    if (llegadasHoy == null) { llegadasHoy = new ArrayList<Reserva>(); }

    List<Reserva> salidasHoy = (List<Reserva>) request.getAttribute("salidasHoy");
    if (salidasHoy == null) { salidasHoy = new ArrayList<Reserva>(); }

    List<Reserva> enCasa = (List<Reserva>) request.getAttribute("enCasa");
    if (enCasa == null) { enCasa = new ArrayList<Reserva>(); }

    List<EstadoCuenta> cuentasPendientes =
            (List<EstadoCuenta>) request.getAttribute("cuentasPendientes");
    if (cuentasPendientes == null) { cuentasPendientes = new ArrayList<EstadoCuenta>(); }

    BigDecimal ingresosHoy  = (BigDecimal) request.getAttribute("ingresosHoy");
    BigDecimal ingresosMes  = (BigDecimal) request.getAttribute("ingresosMes");
    BigDecimal facturadoMes = (BigDecimal) request.getAttribute("facturadoMes");

    Integer facturasObj = (Integer) request.getAttribute("facturasMes");
    int facturasMes = (facturasObj == null) ? 0 : facturasObj.intValue();

    Integer totalCtasObj = (Integer) request.getAttribute("totalCuentasPendientes");
    int totalCuentasPendientes = (totalCtasObj == null) ? 0 : totalCtasObj.intValue();

    Integer totalCliObj = (Integer) request.getAttribute("totalClientes");
    int totalClientes = (totalCliObj == null) ? 0 : totalCliObj.intValue();
%>

<%-- =============================================== Indicadores --%>
<div class="row g-3 mb-4">

    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-pie-chart-fill icono"></i>
            <div class="etiqueta">Ocupacion de hoy</div>
            <div class="valor"><%= ocupacion %><span class="fs-5">%</span></div>
            <div class="progress mt-2" style="height:6px;">
                <div class="progress-bar bg-success" style="width:<%= ocupacion %>%"></div>
            </div>
            <div class="nota mt-1">
                <%= cuantas(habitacionesPorEstado, "OCUPADA") %> habitaciones con huesped
            </div>
        </div>
    </div>

    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-door-open-fill icono"></i>
            <div class="etiqueta">Habitaciones libres</div>
            <div class="valor"><%= cuantas(habitacionesPorEstado, "DISPONIBLE") %></div>
            <div class="nota mt-2">
                <span class="badge bg-danger-subtle text-danger-emphasis">
                    <%= cuantas(habitacionesPorEstado, "OCUPADA") %> ocupadas
                </span>
                <span class="badge bg-warning-subtle text-warning-emphasis">
                    <%= cuantas(habitacionesPorEstado, "LIMPIEZA") %> en limpieza
                </span>
            </div>
        </div>
    </div>

    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-cash-stack icono"></i>
            <div class="etiqueta">Cobrado hoy</div>
            <div class="valor">
                <span class="fs-6 text-muted">Q</span>
                <%= Vista.moneda(ingresosHoy) %>
            </div>
            <div class="nota mt-1">
                Q <%= Vista.moneda(ingresosMes) %> en el mes
            </div>
        </div>
    </div>

    <div class="col-6 col-xl-3">
        <div class="indicador">
            <i class="bi bi-receipt icono"></i>
            <div class="etiqueta">Facturado este mes</div>
            <div class="valor">
                <span class="fs-6 text-muted">Q</span>
                <%= Vista.moneda(facturadoMes) %>
            </div>
            <div class="nota mt-1"><%= facturasMes %> documentos emitidos</div>
        </div>
    </div>
</div>

<%-- =========================================== Llegadas y salidas --%>
<div class="row g-3 mb-4">

    <%-- ---------------------------------------------- Llegadas --%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header d-flex align-items-center justify-content-between">
                <span><i class="bi bi-box-arrow-in-right text-success me-2"></i>
                      Llegadas de hoy</span>
                <span class="badge bg-success-subtle text-success-emphasis">
                    <%= llegadasHoy.size() %>
                </span>
            </div>

<%
    if (llegadasHoy.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-calendar-check fs-1 opacity-25 d-block mb-2"></i>
                No hay llegadas pendientes para hoy.
            </div>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead>
                        <tr>
                            <th>Huesped</th>
                            <th>Hab.</th>
                            <th>Noches</th>
                            <th class="text-end">Accion</th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Reserva r : llegadasHoy) {
%>
                        <tr>
                            <td>
                                <div class="fw-semibold">
                                    <%= Vista.esc(r.getNombreCliente()) %>
                                </div>
                                <div class="text-muted" style="font-size:.74rem;">
                                    <%= Vista.esc(r.getCodigo()) %> &middot;
                                    <%= r.getTotalHuespedes() %> huesped(es)
                                </div>
                            </td>
                            <td>
                                <span class="badge-suave">
                                    <%= Vista.esc(r.getNumeroHabitacion()) %>
                                </span>
                            </td>
                            <td><%= r.getNoches() %></td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-hotel"
                                   href="<%= ctx %>/recepcion?accion=checkin&id=<%= r.getIdReserva() %>">
                                    Check-in
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

    <%-- ----------------------------------------------- Salidas --%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header d-flex align-items-center justify-content-between">
                <span><i class="bi bi-box-arrow-right text-primary me-2"></i>
                      Salidas de hoy</span>
                <span class="badge bg-primary-subtle text-primary-emphasis">
                    <%= salidasHoy.size() %>
                </span>
            </div>

<%
    if (salidasHoy.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-suitcase fs-1 opacity-25 d-block mb-2"></i>
                No hay salidas programadas para hoy.
            </div>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead>
                        <tr>
                            <th>Huesped</th>
                            <th>Hab.</th>
                            <th>Salida</th>
                            <th class="text-end">Accion</th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Reserva r : salidasHoy) {
%>
                        <tr>
                            <td>
                                <div class="fw-semibold">
                                    <%= Vista.esc(r.getNombreCliente()) %>
                                </div>
                                <div class="text-muted" style="font-size:.74rem;">
                                    <%= Vista.esc(r.getCodigo()) %>
                                </div>
                            </td>
                            <td>
                                <span class="badge-suave">
                                    <%= Vista.esc(r.getNumeroHabitacion()) %>
                                </span>
                            </td>
                            <td>
<%
            if (r.isVencida()) {
%>
                                <span class="badge bg-danger">Vencida</span>
<%
            } else {
%>
                                <%= Vista.txt(r.getFechaSalida()) %>
<%
            }
%>
                            </td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-outline-primary"
                                   href="<%= ctx %>/recepcion?accion=checkout&id=<%= r.getIdReserva() %>">
                                    Check-out
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

<%-- ====================================== Cuentas y estado general --%>
<div class="row g-3">

    <%-- --------------------------------- Cuentas por cobrar --%>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header d-flex align-items-center justify-content-between">
                <span><i class="bi bi-exclamation-circle text-warning me-2"></i>
                      Cuentas con saldo pendiente</span>
                <a href="<%= ctx %>/pagos" class="btn btn-sm btn-outline-secondary">
                    Ver todas (<%= totalCuentasPendientes %>)
                </a>
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
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead>
                        <tr>
                            <th>Reserva</th>
                            <th>Cliente</th>
                            <th class="numero-tabla">Cargado</th>
                            <th class="numero-tabla">Pagado</th>
                            <th class="numero-tabla">Saldo</th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (EstadoCuenta cta : cuentasPendientes) {
%>
                        <tr onclick="location.href='<%= ctx %>/reservas?accion=detalle&id=<%= cta.getIdReserva() %>'"
                            style="cursor:pointer;">
                            <td>
                                <span class="badge-suave">
                                    <%= Vista.esc(cta.getCodigo()) %>
                                </span>
                            </td>
                            <td><%= Vista.esc(cta.getCliente()) %></td>
                            <td class="numero-tabla">
                                <%= Vista.moneda(cta.getTotalCargado()) %>
                            </td>
                            <td class="numero-tabla text-success">
                                <%= Vista.moneda(cta.getTotalPagado()) %>
                            </td>
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

    <%-- ------------------------------------- Estado general --%>
    <div class="col-lg-5">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-clipboard-data me-2"></i> Estado general
            </div>
            <div class="card-body">

                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-1">
                        <span class="text-muted" style="font-size:.8rem;">
                            Huespedes en casa
                        </span>
                        <span class="fw-bold"><%= enCasa.size() %></span>
                    </div>
                    <div class="d-flex justify-content-between mb-1">
                        <span class="text-muted" style="font-size:.8rem;">
                            Clientes registrados
                        </span>
                        <span class="fw-bold"><%= totalClientes %></span>
                    </div>
                </div>

                <hr>

                <div class="text-muted mb-2" style="font-size:.7rem; font-weight:700;
                     letter-spacing:.1em; text-transform:uppercase;">
                    Reservas por estado
                </div>

                <div class="d-flex flex-wrap gap-2 mb-3">
                    <span class="badge bg-warning-subtle text-warning-emphasis">
                        Pendientes: <%= cuantas(reservasPorEstado, "PENDIENTE") %>
                    </span>
                    <span class="badge bg-info-subtle text-info-emphasis">
                        Confirmadas: <%= cuantas(reservasPorEstado, "CONFIRMADA") %>
                    </span>
                    <span class="badge bg-success-subtle text-success-emphasis">
                        En casa: <%= cuantas(reservasPorEstado, "EN_CASA") %>
                    </span>
                    <span class="badge bg-secondary-subtle text-secondary-emphasis">
                        Finalizadas: <%= cuantas(reservasPorEstado, "FINALIZADA") %>
                    </span>
                    <span class="badge bg-danger-subtle text-danger-emphasis">
                        Canceladas: <%= cuantas(reservasPorEstado, "CANCELADA") %>
                    </span>
                </div>

                <hr>

                <div class="text-muted mb-2" style="font-size:.7rem; font-weight:700;
                     letter-spacing:.1em; text-transform:uppercase;">
                    Accesos rapidos
                </div>

                <div class="d-grid gap-2">
                    <a href="<%= ctx %>/disponibilidad" class="btn btn-sm btn-hotel">
                        <i class="bi bi-search me-1"></i> Consultar disponibilidad
                    </a>
                    <a href="<%= ctx %>/reservas?accion=nueva" class="btn btn-sm btn-outline-secondary">
                        <i class="bi bi-plus-circle me-1"></i> Nueva reserva
                    </a>
                    <a href="<%= ctx %>/recepcion" class="btn btn-sm btn-outline-secondary">
                        <i class="bi bi-door-open me-1"></i> Panel de recepcion
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="comun/pie.jsp" />
