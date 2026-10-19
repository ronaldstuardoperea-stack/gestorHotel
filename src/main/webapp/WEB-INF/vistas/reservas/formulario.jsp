<%--
    reservas/formulario.jsp

    Alta y edicion de reservas. El flujo tiene dos pasos porque asi
    funciona el negocio: primero se eligen las fechas, y solo entonces
    el sistema puede decir que habitaciones estan libres para venderlas.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.Habitacion" %>
<%@ page import="com.hotel.modelo.Cliente" %>
<%@ page import="com.hotel.modelo.Temporada" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Reservacion" />
    <jsp:param name="subtitulo" value="Registro de una nueva estadia" />
    <jsp:param name="activo" value="reservas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Reserva reserva = (Reserva) request.getAttribute("reserva");
    boolean edicion = (reserva != null);

    LocalDate fEntrada = edicion ? reserva.getFechaEntrada()
                                 : (LocalDate) request.getAttribute("entrada");
    LocalDate fSalida  = edicion ? reserva.getFechaSalida()
                                 : (LocalDate) request.getAttribute("salida");

    List<Habitacion> disponibles = (List<Habitacion>) request.getAttribute("disponibles");
    if (disponibles == null) { disponibles = new ArrayList<Habitacion>(); }

    List<Cliente> clientes = (List<Cliente>) request.getAttribute("clientes");
    if (clientes == null) { clientes = new ArrayList<Cliente>(); }

    Temporada temporada = (Temporada) request.getAttribute("temporada");

    BigDecimal tarifaSugerida = (BigDecimal) request.getAttribute("tarifaSugerida");

    Integer habPre = (Integer) request.getAttribute("idHabitacionPreseleccionada");
    int idHabitacionPre = (habPre == null) ? 0 : habPre.intValue();

    Integer cliPre = (Integer) request.getAttribute("idClientePreseleccionado");
    int idClientePre = (cliPre == null) ? 0 : cliPre.intValue();

    // Sin fechas elegidas no hay nada que ofrecer todavia.
    boolean sinResultados = (disponibles.isEmpty() && !edicion);
%>

<%-- ================================= Paso 1: las fechas --%>
<div class="card mb-3">
    <div class="card-header">
        <span class="badge bg-secondary me-2">1</span>
        Fechas de la estadia
    </div>
    <div class="card-body">
        <form method="get" action="<%= ctx %>/reservas" class="row g-2 align-items-end">
            <input type="hidden" name="accion" value="<%= edicion ? "editar" : "nueva" %>">
<%
    if (edicion) {
%>
            <input type="hidden" name="id" value="<%= reserva.getIdReserva() %>">
<%
    }
%>

            <div class="col-md-3">
                <label class="form-label obligatorio">Entrada</label>
                <input type="date" class="form-control" name="entrada"
                       value="<%= Vista.txt(fEntrada) %>" required>
            </div>

            <div class="col-md-3">
                <label class="form-label obligatorio">Salida</label>
                <input type="date" class="form-control" name="salida"
                       value="<%= Vista.txt(fSalida) %>" required>
            </div>

            <div class="col-md-3">
                <button type="submit" class="btn btn-hotel">
                    <i class="bi bi-search me-1"></i> Ver habitaciones libres
                </button>
            </div>

            <div class="col-md-3 text-md-end">
<%
    if (temporada != null) {
        String tono = temporada.isRecargo() ? "warning" : "info";
%>
                <span class="badge bg-<%= tono %>-subtle text-<%= tono %>-emphasis">
                    <i class="bi bi-calendar-range me-1"></i>
                    <%= Vista.esc(temporada.getNombre()) %>
                    (<%= Vista.esc(temporada.getVariacionPorcentual()) %>)
                </span>
<%
    }
%>
            </div>
        </form>
    </div>
</div>

<%
    if (sinResultados) {
%>
<div class="card">
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-calendar2-week fs-1 opacity-25 d-block mb-2"></i>
<%
        if (fEntrada == null) {
%>
        Elija las fechas de entrada y salida para ver que
        habitaciones estan disponibles.
<%
        } else {
%>
        No hay habitaciones disponibles del <%= Vista.txt(fEntrada) %>
        al <%= Vista.txt(fSalida) %>. Pruebe con otras fechas.
<%
        }
%>
    </div>
</div>
<%
    } else {
%>

<%-- ============================ Paso 2: los datos --%>
<form method="post" action="<%= ctx %>/reservas" id="formReserva">
    <input type="hidden" name="idReserva" value="<%= edicion ? reserva.getIdReserva() : 0 %>">
    <input type="hidden" name="fechaEntrada" value="<%= Vista.txt(fEntrada) %>">
    <input type="hidden" name="fechaSalida"  value="<%= Vista.txt(fSalida) %>">

    <div class="row g-3">

        <%-- ------------------------- Habitacion --%>
        <div class="col-lg-7">
            <div class="card h-100">
                <div class="card-header">
                    <span class="badge bg-secondary me-2">2</span>
                    Habitacion disponible
                </div>
                <div class="card-body" style="max-height:460px; overflow-y:auto;">

<%
        for (Habitacion h : disponibles) {
            boolean marcada = (edicion && reserva.getIdHabitacion() == h.getIdHabitacion())
                              || (idHabitacionPre == h.getIdHabitacion());
%>
                    <label class="d-flex align-items-center gap-3 p-2 mb-2 border rounded-3
                                  <%= marcada ? "border-primary bg-light" : "" %>"
                           style="cursor:pointer;">

                        <input type="radio" name="idHabitacion" class="form-check-input mt-0"
                               value="<%= h.getIdHabitacion() %>"
                               data-tarifa="<%= h.getTarifaBase() %>"
                               data-adultos="<%= h.getCapacidadAdultos() %>"
                               data-ninos="<%= h.getCapacidadNinos() %>"
                               onchange="actualizarTarifa(this)"
                               <%= Vista.marcado(marcada) %> required>

                        <div class="flex-grow-1">
                            <div class="fw-semibold">
                                Habitacion <%= Vista.esc(h.getNumero()) %>
                                <span class="text-muted fw-normal"
                                      style="font-size:.78rem;">
                                    &middot; piso <%= h.getPiso() %>
                                </span>
                            </div>
                            <div class="text-muted" style="font-size:.78rem;">
                                <%= Vista.esc(h.getNombreTipo()) %> &middot;
                                hasta <%= h.getCapacidadAdultos() %> adulto(s)
<%
            if (h.getCapacidadNinos() > 0) {
%>
                                y <%= h.getCapacidadNinos() %> nino(s)
<%
            }
%>
                            </div>
                        </div>

                        <div class="text-end">
                            <div class="fw-bold" style="color:var(--hotel-azul);">
                                Q <%= Vista.moneda(h.getTarifaBase()) %>
                            </div>
                            <div class="text-muted" style="font-size:.7rem;">
                                tarifa base
                            </div>
                        </div>
                    </label>
<%
        }
%>
                </div>
            </div>
        </div>

        <%-- ------------------------ Datos de la reserva --%>
        <div class="col-lg-5">
            <div class="card h-100">
                <div class="card-header">
                    <span class="badge bg-secondary me-2">3</span>
                    Datos de la reserva
                </div>
                <div class="card-body">

                    <div class="mb-3">
                        <label for="idCliente" class="form-label obligatorio">Cliente</label>
                        <select class="form-select" id="idCliente" name="idCliente" required>
                            <option value="">Seleccione un cliente...</option>
<%
        for (Cliente cl : clientes) {
            boolean elegido = (edicion && reserva.getIdCliente() == cl.getIdCliente())
                              || (idClientePre == cl.getIdCliente());
%>
                            <option value="<%= cl.getIdCliente() %>"<%=
                                    Vista.selecto(elegido) %>>
                                <%= Vista.esc(cl.getNombreCompleto()) %>
                                (<%= Vista.esc(cl.getDocumento()) %>)
                            </option>
<%
        }
%>
                        </select>
                        <div class="form-text">
                            <a href="<%= ctx %>/clientes?accion=nuevo">
                                Registrar un cliente nuevo
                            </a>
                        </div>
                    </div>

                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label for="adultos" class="form-label obligatorio">Adultos</label>
                            <input type="number" class="form-control" id="adultos"
                                   name="adultos" min="1" max="20"
                                   value="<%= edicion ? reserva.getAdultos() : 1 %>" required>
                        </div>
                        <div class="col-6">
                            <label for="ninos" class="form-label">Ninos</label>
                            <input type="number" class="form-control" id="ninos"
                                   name="ninos" min="0" max="20"
                                   value="<%= edicion ? reserva.getNinos() : 0 %>" required>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="tarifaNoche" class="form-label obligatorio">
                            Tarifa por noche (Q)
                        </label>
                        <input type="number" class="form-control" id="tarifaNoche"
                               name="tarifaNoche" step="0.01" min="0.01"
                               value="<%= edicion ? Vista.txt(reserva.getTarifaNoche())
                                                  : Vista.txt(tarifaSugerida) %>"
                               required>
                        <div class="form-text" id="notaTarifa">
<%
        if (temporada != null) {
%>
                            Sugerida segun la temporada
                            "<%= Vista.esc(temporada.getNombre()) %>"
                            (<%= Vista.esc(temporada.getVariacionPorcentual()) %>).
                            Puede ajustarla.
<%
        } else {
%>
                            Tarifa de lista. Puede ajustarla si se
                            autoriza un precio especial.
<%
        }
%>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="observaciones" class="form-label">Observaciones</label>
                        <textarea class="form-control" id="observaciones"
                                  name="observaciones" rows="2" maxlength="255"
                                  placeholder="Peticiones especiales del huesped..."><%=
                                  edicion ? Vista.esc(reserva.getObservaciones()) : "" %></textarea>
                    </div>

                    <%-- Resumen calculado en el navegador, para que el
                         recepcionista vea el total antes de guardar. --%>
                    <div class="alert alert-light border mb-0">
                        <div class="d-flex justify-content-between">
                            <span class="text-muted">Noches</span>
                            <strong id="resumenNoches">-</strong>
                        </div>
                        <div class="d-flex justify-content-between">
                            <span class="text-muted">Total hospedaje</span>
                            <strong id="resumenTotal" style="color:var(--hotel-azul);">
                                Q 0.00
                            </strong>
                        </div>
                    </div>
                </div>

                <div class="card-footer bg-white d-flex gap-2 justify-content-end">
                    <a href="<%= ctx %>/reservas" class="btn btn-outline-secondary">Cancelar</a>
                    <button type="submit" class="btn btn-oro">
                        <i class="bi bi-check-lg me-1"></i>
                        <%= edicion ? "Guardar cambios" : "Crear reserva" %>
                    </button>
                </div>
            </div>
        </div>
    </div>
</form>
<%
    }
%>

<script>
    /* Factor de la temporada vigente, para sugerir el precio. */
    var FACTOR_TEMPORADA = <%= (temporada == null) ? "1" : temporada.getFactor().toString() %>;

    var ENTRADA = '<%= Vista.txt(fEntrada) %>';
    var SALIDA  = '<%= Vista.txt(fSalida) %>';

    /*
     * Al elegir una habitacion se propone su tarifa base ajustada por la
     * temporada. Es solo una sugerencia: el campo queda editable, porque
     * recepcion a veces autoriza un precio especial.
     */
    function actualizarTarifa(radio) {
        var base = parseFloat(radio.getAttribute('data-tarifa'));
        document.getElementById('tarifaNoche').value =
            (base * FACTOR_TEMPORADA).toFixed(2);
        recalcularResumen();
    }

    /* Calcula noches y total con lo que hay en pantalla. */
    function recalcularResumen() {
        // Mientras no se hayan elegido fechas, el paso 3 no esta en
        // pantalla y no hay nada que recalcular.
        if (!ENTRADA || !SALIDA || !document.getElementById('tarifaNoche')) { return; }

        var entrada = new Date(ENTRADA + 'T00:00:00');
        var salida  = new Date(SALIDA  + 'T00:00:00');
        var noches  = Math.round((salida - entrada) / 86400000);
        if (noches < 0) { noches = 0; }

        var tarifa = parseFloat(document.getElementById('tarifaNoche').value) || 0;

        document.getElementById('resumenNoches').textContent = noches;
        document.getElementById('resumenTotal').textContent =
            'Q ' + (noches * tarifa).toFixed(2);
    }

    var campoTarifa = document.getElementById('tarifaNoche');
    if (campoTarifa) {
        campoTarifa.addEventListener('input', recalcularResumen);
        recalcularResumen();
    }
</script>

<jsp:include page="../comun/pie.jsp" />
