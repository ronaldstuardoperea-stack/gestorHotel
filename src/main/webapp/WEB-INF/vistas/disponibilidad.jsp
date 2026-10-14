<%--
    disponibilidad.jsp

    Consulta de habitaciones libres en un rango de fechas. Es la
    pregunta que mas veces se hace en la recepcion de un hotel, asi que
    tiene pantalla propia y da paso directo a crear la reserva.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Habitacion" %>
<%@ page import="com.hotel.modelo.TipoHabitacion" %>
<%@ page import="com.hotel.modelo.Temporada" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="comun/cabecera.jsp">
    <jsp:param name="titulo" value="Disponibilidad" />
    <jsp:param name="subtitulo" value="Habitaciones libres por rango de fechas" />
    <jsp:param name="activo" value="disponibilidad" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    List<TipoHabitacion> tipos = (List<TipoHabitacion>) request.getAttribute("tipos");
    if (tipos == null) { tipos = new ArrayList<TipoHabitacion>(); }

    Integer fTipo = (Integer) request.getAttribute("filtroTipo");
    int filtroTipo = (fTipo == null) ? 0 : fTipo.intValue();

    LocalDate entrada = (LocalDate) request.getAttribute("entrada");
    LocalDate salida  = (LocalDate) request.getAttribute("salida");
    boolean hayConsulta = (entrada != null && salida != null);

    Long nochesObj = (Long) request.getAttribute("noches");
    long noches = (nochesObj == null) ? 0L : nochesObj.longValue();

    Temporada temporada = (Temporada) request.getAttribute("temporada");

    // El factor de la temporada se aplica sobre la tarifa base para
    // mostrar el precio que realmente pagaria el cliente.
    BigDecimal factor = (temporada == null) ? BigDecimal.ONE : temporada.getFactor();

    List<Habitacion> disponibles = (List<Habitacion>) request.getAttribute("disponibles");
    if (disponibles == null) { disponibles = new ArrayList<Habitacion>(); }
%>

<%-- ========================================= Buscador --%>
<div class="card mb-3">
    <div class="card-body">
        <form method="get" action="<%= ctx %>/disponibilidad" class="row g-3 align-items-end">

            <div class="col-md-3">
                <label class="form-label obligatorio">Fecha de entrada</label>
                <input type="date" class="form-control" name="entrada"
                       value="<%= Vista.txt(entrada) %>" required>
            </div>

            <div class="col-md-3">
                <label class="form-label obligatorio">Fecha de salida</label>
                <input type="date" class="form-control" name="salida"
                       value="<%= Vista.txt(salida) %>" required>
            </div>

            <div class="col-md-3">
                <label class="form-label">Tipo de habitacion</label>
                <select name="idTipo" class="form-select">
                    <option value="0">Todos los tipos</option>
<%
    for (TipoHabitacion t : tipos) {
%>
                    <option value="<%= t.getIdTipo() %>"<%=
                            Vista.selecto(filtroTipo == t.getIdTipo()) %>>
                        <%= Vista.esc(t.getNombre()) %>
                    </option>
<%
    }
%>
                </select>
            </div>

            <div class="col-md-3">
                <button type="submit" class="btn btn-hotel w-100">
                    <i class="bi bi-search me-1"></i> Consultar
                </button>
            </div>
        </form>
    </div>
</div>

<%
    if (hayConsulta) {
%>

    <%-- ================================ Resumen de la busqueda --%>
    <div class="alert alert-light border d-flex flex-wrap align-items-center gap-3 mb-3">
        <div>
            <i class="bi bi-calendar2-check me-1" style="color:var(--hotel-azul);"></i>
            Del <strong><%= Vista.txt(entrada) %></strong>
            al <strong><%= Vista.txt(salida) %></strong>
            &middot; <strong><%= noches %></strong> noche(s)
        </div>

<%
        if (temporada != null) {
            String tono = temporada.isRecargo() ? "warning" : "info";
%>
        <span class="badge bg-<%= tono %>-subtle text-<%= tono %>-emphasis">
            <i class="bi bi-tag me-1"></i>
            Temporada: <%= Vista.esc(temporada.getNombre()) %>
            (<%= Vista.esc(temporada.getVariacionPorcentual()) %>)
        </span>
<%
        }
%>

        <span class="ms-auto fw-semibold">
            <%= disponibles.size() %> habitacion(es) disponible(s)
        </span>
    </div>

    <%-- ==================================== Resultado --%>
<%
        if (disponibles.isEmpty()) {
%>
    <div class="card">
        <div class="card-body text-center text-muted py-5">
            <i class="bi bi-calendar-x fs-1 opacity-25 d-block mb-2"></i>
            <strong>No hay habitaciones disponibles en esas fechas.</strong>
            <div class="mt-2" style="font-size:.85rem;">
                Pruebe con otro rango, con otro tipo de habitacion, o
                revise si hay bloqueos vigentes que se puedan levantar.
            </div>
        </div>
    </div>
<%
        } else {
%>
    <div class="row g-3">
<%
            for (Habitacion h : disponibles) {

                // Precio de la estadia completa con la temporada ya
                // aplicada, que es lo que el cliente pregunta.
                BigDecimal tarifa = h.getTarifaBase().multiply(factor);
                BigDecimal total  = tarifa.multiply(new BigDecimal(noches));
%>
        <div class="col-md-6 col-xl-4">
            <div class="card h-100">
                <div class="card-body">

                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            <div class="fs-4 fw-bold" style="color:var(--hotel-noche);">
                                <%= Vista.esc(h.getNumero()) %>
                            </div>
                            <div class="text-muted"
                                 style="font-size:.76rem; text-transform:uppercase;
                                        letter-spacing:.05em;">
                                <%= Vista.esc(h.getNombreTipo()) %>
                            </div>
                        </div>
                        <span class="badge bg-success-subtle text-success-emphasis">
                            <i class="bi bi-check-circle me-1"></i> Libre
                        </span>
                    </div>

                    <div class="text-muted mb-3" style="font-size:.8rem;">
                        Piso <%= h.getPiso() %> &middot;
                        hasta <%= h.getCapacidadAdultos() %> adulto(s)
<%
                if (h.getCapacidadNinos() > 0) {
%>
                        y <%= h.getCapacidadNinos() %> nino(s)
<%
                }
                if (h.getDescripcion() != null && !h.getDescripcion().isEmpty()) {
%>
                        <div class="mt-1">
                            <%= Vista.esc(h.getDescripcion()) %>
                        </div>
<%
                }
%>
                    </div>

                    <div class="border-top pt-2 mb-3">
                        <div class="d-flex justify-content-between"
                             style="font-size:.82rem;">
                            <span class="text-muted">Tarifa por noche</span>
                            <strong>Q <%= Vista.moneda(tarifa) %></strong>
                        </div>
                        <div class="d-flex justify-content-between">
                            <span class="text-muted" style="font-size:.82rem;">
                                Total <%= noches %> noche(s)
                            </span>
                            <strong class="fs-5" style="color:var(--hotel-azul);">
                                Q <%= Vista.moneda(total) %>
                            </strong>
                        </div>
                    </div>

<%
                if (puedeOperar) {
%>
                    <a class="btn btn-sm btn-oro w-100"
                       href="<%= ctx %>/reservas?accion=nueva&entrada=<%= Vista.txt(entrada) %>&salida=<%= Vista.txt(salida) %>&idHabitacion=<%= h.getIdHabitacion() %>">
                        <i class="bi bi-journal-plus me-1"></i> Reservar
                    </a>
<%
                }
%>
                </div>
            </div>
        </div>
<%
            }
%>
    </div>
<%
        }
    }
%>

<jsp:include page="comun/pie.jsp" />
