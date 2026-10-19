<%--
    reservas/lista.jsp

    Buscador y listado de reservaciones. Los filtros se acumulan: se
    puede pedir solo las canceladas, solo las de un cliente, o las
    canceladas de ese cliente dentro de un rango de fechas.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.util.Vista" %>

<%!
    /* Conteo de un estado, o cero si ese estado no viene en el mapa. */
    private int cuantas(Map<String, Integer> conteo, String estado) {
        Integer valor = conteo.get(estado);
        return (valor == null) ? 0 : valor.intValue();
    }
%>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Reservaciones" />
    <jsp:param name="subtitulo" value="Busqueda y control de reservas" />
    <jsp:param name="activo" value="reservas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    List<Reserva> reservas = (List<Reserva>) request.getAttribute("reservas");
    if (reservas == null) { reservas = new ArrayList<Reserva>(); }

    Map<String, Integer> conteo = (Map<String, Integer>) request.getAttribute("conteo");
    if (conteo == null) { conteo = new HashMap<String, Integer>(); }

    String busqueda = (String) request.getAttribute("busqueda");

    String filtroEstado = (String) request.getAttribute("filtroEstado");
    if (filtroEstado == null) { filtroEstado = ""; }

    LocalDate filtroDesde = (LocalDate) request.getAttribute("filtroDesde");
    LocalDate filtroHasta = (LocalDate) request.getAttribute("filtroHasta");
%>

<%-- ================================= Resumen por estado --%>
<div class="row g-2 mb-3">
    <div class="col-6 col-md-2">
        <div class="indicador py-2">
            <div class="etiqueta">Pendientes</div>
            <div class="valor text-warning"><%= cuantas(conteo, "PENDIENTE") %></div>
        </div>
    </div>
    <div class="col-6 col-md-2">
        <div class="indicador py-2">
            <div class="etiqueta">Confirmadas</div>
            <div class="valor text-info"><%= cuantas(conteo, "CONFIRMADA") %></div>
        </div>
    </div>
    <div class="col-6 col-md-2">
        <div class="indicador py-2">
            <div class="etiqueta">En casa</div>
            <div class="valor text-success"><%= cuantas(conteo, "EN_CASA") %></div>
        </div>
    </div>
    <div class="col-6 col-md-2">
        <div class="indicador py-2">
            <div class="etiqueta">Finalizadas</div>
            <div class="valor text-secondary"><%= cuantas(conteo, "FINALIZADA") %></div>
        </div>
    </div>
    <div class="col-6 col-md-2">
        <div class="indicador py-2">
            <div class="etiqueta">Canceladas</div>
            <div class="valor text-danger"><%= cuantas(conteo, "CANCELADA") %></div>
        </div>
    </div>
    <div class="col-6 col-md-2">
        <div class="indicador py-2">
            <div class="etiqueta">No show</div>
            <div class="valor text-dark"><%= cuantas(conteo, "NO_SHOW") %></div>
        </div>
    </div>
</div>

<%-- ============================================ Filtros --%>
<div class="card mb-3">
    <div class="card-body py-3">
        <form method="get" action="<%= ctx %>/reservas" class="row g-2 align-items-end">

            <div class="col-md-4">
                <label class="form-label">Buscar</label>
                <input type="text" class="form-control form-control-sm" name="q"
                       value="<%= Vista.esc(busqueda) %>"
                       placeholder="Codigo, cliente, documento o habitacion">
            </div>

            <div class="col-md-2">
                <label class="form-label">Estado</label>
                <select name="estado" class="form-select form-select-sm">
                    <option value="">Todos</option>
                    <option value="PENDIENTE"<%=
                        Vista.selecto("PENDIENTE".equals(filtroEstado)) %>>Pendiente</option>
                    <option value="CONFIRMADA"<%=
                        Vista.selecto("CONFIRMADA".equals(filtroEstado)) %>>Confirmada</option>
                    <option value="EN_CASA"<%=
                        Vista.selecto("EN_CASA".equals(filtroEstado)) %>>En casa</option>
                    <option value="FINALIZADA"<%=
                        Vista.selecto("FINALIZADA".equals(filtroEstado)) %>>Finalizada</option>
                    <option value="CANCELADA"<%=
                        Vista.selecto("CANCELADA".equals(filtroEstado)) %>>Cancelada</option>
                    <option value="NO_SHOW"<%=
                        Vista.selecto("NO_SHOW".equals(filtroEstado)) %>>No se presento</option>
                </select>
            </div>

            <div class="col-md-2">
                <label class="form-label">Entrada desde</label>
                <input type="date" class="form-control form-control-sm" name="desde"
                       value="<%= Vista.txt(filtroDesde) %>">
            </div>

            <div class="col-md-2">
                <label class="form-label">Entrada hasta</label>
                <input type="date" class="form-control form-control-sm" name="hasta"
                       value="<%= Vista.txt(filtroHasta) %>">
            </div>

            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-sm btn-hotel">
                    <i class="bi bi-search"></i>
                </button>
                <a href="<%= ctx %>/reservas" class="btn btn-sm btn-outline-secondary">Limpiar</a>
            </div>
        </form>
    </div>
</div>

<div class="d-flex justify-content-between align-items-center mb-3">
    <span class="text-muted" style="font-size:.85rem;">
        <%= reservas.size() %> reserva(s) encontrada(s)
    </span>
<%
    if (puedeOperar) {
%>
    <a href="<%= ctx %>/reservas?accion=nueva" class="btn btn-sm btn-oro">
        <i class="bi bi-plus-lg me-1"></i> Nueva reserva
    </a>
<%
    }
%>
</div>

<%-- ============================================ Listado --%>
<div class="card">
<%
    if (reservas.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-journal-x fs-1 opacity-25 d-block mb-2"></i>
        No hay reservas que coincidan con los filtros aplicados.
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Codigo</th>
                    <th>Cliente</th>
                    <th>Habitacion</th>
                    <th>Estadia</th>
                    <th>Huespedes</th>
                    <th class="numero-tabla">Hospedaje</th>
                    <th>Estado</th>
                    <th class="text-end">Acciones</th>
                </tr>
            </thead>
            <tbody>
<%
        for (Reserva r : reservas) {
%>
                <tr>
                    <td>
                        <a href="<%= ctx %>/reservas?accion=detalle&id=<%= r.getIdReserva() %>"
                           class="badge-suave text-decoration-none">
                            <%= Vista.esc(r.getCodigo()) %>
                        </a>
                    </td>

                    <td>
                        <div class="fw-semibold" style="font-size:.85rem;">
                            <%= Vista.esc(r.getNombreCliente()) %>
                        </div>
                        <div class="text-muted" style="font-size:.72rem;">
                            <%= Vista.esc(r.getDocumentoCliente()) %>
                        </div>
                    </td>

                    <td>
                        <div class="fw-semibold"><%= Vista.esc(r.getNumeroHabitacion()) %></div>
                        <div class="text-muted" style="font-size:.7rem;">
                            <%= Vista.esc(r.getNombreTipo()) %>
                        </div>
                    </td>

                    <td style="font-size:.82rem;">
                        <%= Vista.txt(r.getFechaEntrada()) %> &rarr; <%= Vista.txt(r.getFechaSalida()) %>
                        <div class="text-muted" style="font-size:.72rem;">
                            <%= r.getNoches() %> noche(s)
<%
            if (r.isVencida()) {
%>
                            <span class="badge bg-danger"
                                  style="font-size:.6rem;">Salida vencida</span>
<%
            }
%>
                        </div>
                    </td>

                    <td style="font-size:.82rem;">
                        <%= r.getAdultos() %> adulto(s)
<%
            if (r.getNinos() > 0) {
%>
                        <div class="text-muted" style="font-size:.72rem;">
                            + <%= r.getNinos() %> nino(s)
                        </div>
<%
            }
%>
                    </td>

                    <td class="numero-tabla fw-semibold">
                        <%= Vista.moneda(r.getTotalHospedaje()) %>
                    </td>

                    <td>
                        <span class="badge bg-<%= r.getColorEstado() %>">
                            <%= Vista.esc(r.getEstadoLegible()) %>
                        </span>
                    </td>

                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/reservas?accion=detalle&id=<%= r.getIdReserva() %>"
                               title="Ver ficha">
                                <i class="bi bi-eye"></i>
                            </a>

<%
            if (puedeOperar && r.isCheckinHabilitado()) {
%>
                            <a class="btn btn-outline-success"
                               href="<%= ctx %>/recepcion?accion=checkin&id=<%= r.getIdReserva() %>"
                               title="Check-in">
                                <i class="bi bi-box-arrow-in-right"></i>
                            </a>
<%
            }
            if (puedeOperar && r.isCheckoutHabilitado()) {
%>
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
