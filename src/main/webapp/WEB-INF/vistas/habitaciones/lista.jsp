<%--
    habitaciones/lista.jsp

    Inventario de habitaciones con filtros y vista de rejilla.
    La rejilla imita el tablero fisico de llaves que se usa en la
    recepcion de un hotel: un vistazo basta para saber que hay libre.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Habitacion" %>
<%@ page import="com.hotel.modelo.TipoHabitacion" %>
<%@ page import="com.hotel.util.Vista" %>

<%!
    /* Devuelve el conteo de un estado, o cero si ese estado no aparece
       en el mapa que trae el Servlet. */
    private int cuantas(Map<String, Integer> conteo, String estado) {
        Integer valor = conteo.get(estado);
        return (valor == null) ? 0 : valor.intValue();
    }
%>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Habitaciones" />
    <jsp:param name="subtitulo" value="Inventario y estado actual del hotel" />
    <jsp:param name="activo" value="habitaciones" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());
    boolean esAdmin     = (u != null && u.isAdministrador());

    List<Habitacion> habitaciones =
            (List<Habitacion>) request.getAttribute("habitaciones");
    if (habitaciones == null) { habitaciones = new ArrayList<Habitacion>(); }

    List<TipoHabitacion> tipos = (List<TipoHabitacion>) request.getAttribute("tipos");
    if (tipos == null) { tipos = new ArrayList<TipoHabitacion>(); }

    List<Integer> pisos = (List<Integer>) request.getAttribute("pisos");
    if (pisos == null) { pisos = new ArrayList<Integer>(); }

    Map<String, Integer> conteo = (Map<String, Integer>) request.getAttribute("conteo");
    if (conteo == null) { conteo = new HashMap<String, Integer>(); }

    String filtroEstado = (String) request.getAttribute("filtroEstado");
    if (filtroEstado == null) { filtroEstado = ""; }

    Integer fTipo = (Integer) request.getAttribute("filtroTipo");
    int filtroTipo = (fTipo == null) ? 0 : fTipo.intValue();

    Integer fPiso = (Integer) request.getAttribute("filtroPiso");
    int filtroPiso = (fPiso == null) ? 0 : fPiso.intValue();
%>

<%-- ================================ Resumen por estado --%>
<div class="row g-2 mb-3">
    <div class="col-6 col-md">
        <div class="indicador py-2">
            <div class="etiqueta">Disponibles</div>
            <div class="valor text-success"><%= cuantas(conteo, "DISPONIBLE") %></div>
        </div>
    </div>
    <div class="col-6 col-md">
        <div class="indicador py-2">
            <div class="etiqueta">Ocupadas</div>
            <div class="valor text-danger"><%= cuantas(conteo, "OCUPADA") %></div>
        </div>
    </div>
    <div class="col-6 col-md">
        <div class="indicador py-2">
            <div class="etiqueta">En limpieza</div>
            <div class="valor text-warning"><%= cuantas(conteo, "LIMPIEZA") %></div>
        </div>
    </div>
    <div class="col-6 col-md">
        <div class="indicador py-2">
            <div class="etiqueta">Mantenimiento</div>
            <div class="valor text-secondary"><%= cuantas(conteo, "MANTENIMIENTO") %></div>
        </div>
    </div>
    <div class="col-6 col-md">
        <div class="indicador py-2">
            <div class="etiqueta">Bloqueadas</div>
            <div class="valor text-dark"><%= cuantas(conteo, "BLOQUEADA") %></div>
        </div>
    </div>
</div>

<%-- ========================================= Filtros --%>
<div class="card mb-3">
    <div class="card-body py-3">
        <form method="get" action="<%= ctx %>/habitaciones" class="row g-2 align-items-end">

            <div class="col-md-3">
                <label class="form-label">Estado</label>
                <select name="estado" class="form-select form-select-sm">
                    <option value="">Todos</option>
                    <option value="DISPONIBLE"<%=
                        Vista.selecto("DISPONIBLE".equals(filtroEstado)) %>>Disponible</option>
                    <option value="OCUPADA"<%=
                        Vista.selecto("OCUPADA".equals(filtroEstado)) %>>Ocupada</option>
                    <option value="LIMPIEZA"<%=
                        Vista.selecto("LIMPIEZA".equals(filtroEstado)) %>>En limpieza</option>
                    <option value="MANTENIMIENTO"<%=
                        Vista.selecto("MANTENIMIENTO".equals(filtroEstado)) %>>Mantenimiento</option>
                    <option value="BLOQUEADA"<%=
                        Vista.selecto("BLOQUEADA".equals(filtroEstado)) %>>Bloqueada</option>
                </select>
            </div>

            <div class="col-md-3">
                <label class="form-label">Tipo</label>
                <select name="idTipo" class="form-select form-select-sm">
                    <option value="0">Todos</option>
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

            <div class="col-md-2">
                <label class="form-label">Piso</label>
                <select name="piso" class="form-select form-select-sm">
                    <option value="0">Todos</option>
<%
    for (Integer p : pisos) {
%>
                    <option value="<%= p %>"<%=
                            Vista.selecto(filtroPiso == p.intValue()) %>>Piso <%= p %></option>
<%
    }
%>
                </select>
            </div>

            <div class="col-md-4 d-flex gap-2">
                <button type="submit" class="btn btn-sm btn-hotel">
                    <i class="bi bi-funnel me-1"></i> Filtrar
                </button>
                <a href="<%= ctx %>/habitaciones" class="btn btn-sm btn-outline-secondary">Limpiar</a>

<%
    if (puedeOperar) {
%>
                <a href="<%= ctx %>/habitaciones?accion=nuevo"
                   class="btn btn-sm btn-oro ms-auto">
                    <i class="bi bi-plus-lg me-1"></i> Nueva
                </a>
<%
    }
%>
            </div>
        </form>
    </div>
</div>

<%-- ======================================== Rejilla --%>
<%
    if (habitaciones.isEmpty()) {
%>
<div class="card">
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-house-slash fs-1 opacity-25 d-block mb-2"></i>
        No hay habitaciones que coincidan con los filtros aplicados.
    </div>
</div>
<%
    } else {
%>
<div class="rejilla-habitaciones">
<%
        for (Habitacion h : habitaciones) {
            String color = h.getColorEstado();
%>
    <div class="habitacion <%= color %>">

        <div class="d-flex justify-content-between align-items-start">
            <div>
                <div class="numero"><%= Vista.esc(h.getNumero()) %></div>
                <div class="tipo"><%= Vista.esc(h.getNombreTipo()) %></div>
            </div>
            <i class="bi bi-<%= h.getIconoEstado() %> text-<%= color %> fs-5"></i>
        </div>

        <div class="precio mt-2">
            Q <%= Vista.moneda(h.getTarifaBase()) %>
            <span class="text-muted fw-normal" style="font-size:.72rem;">/ noche</span>
        </div>

        <div class="mt-2 d-flex justify-content-between align-items-center">
            <span class="badge bg-<%= color %>-subtle text-<%= color %>-emphasis"
                  style="font-size:.64rem;">
                <%= Vista.esc(h.getEstado()) %>
            </span>

            <div class="dropdown">
                <button class="btn btn-sm btn-link text-muted p-0"
                        data-bs-toggle="dropdown" aria-label="Opciones">
                    <i class="bi bi-three-dots-vertical"></i>
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li>
                        <a class="dropdown-item"
                           href="<%= ctx %>/habitaciones?accion=detalle&id=<%= h.getIdHabitacion() %>">
                            <i class="bi bi-eye me-2"></i> Ver ficha
                        </a>
                    </li>
<%
            if (puedeOperar) {
%>
                    <li>
                        <a class="dropdown-item"
                           href="<%= ctx %>/habitaciones?accion=editar&id=<%= h.getIdHabitacion() %>">
                            <i class="bi bi-pencil me-2"></i> Editar
                        </a>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li class="dropdown-header">Cambiar estado</li>

                    <%-- Solo se ofrecen los cambios de estado que el
                         recepcionista o el ama de llaves hacen a mano.
                         OCUPADA lo pone el check-in y BLOQUEADA el
                         modulo de bloqueos, no un menu. --%>
                    <li>
                        <a class="dropdown-item"
                           href="<%= ctx %>/habitaciones?accion=estado&id=<%= h.getIdHabitacion() %>&valor=DISPONIBLE">
                            <i class="bi bi-check-circle me-2 text-success"></i> Disponible
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item"
                           href="<%= ctx %>/habitaciones?accion=estado&id=<%= h.getIdHabitacion() %>&valor=LIMPIEZA">
                            <i class="bi bi-brush me-2 text-warning"></i> En limpieza
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item"
                           href="<%= ctx %>/habitaciones?accion=estado&id=<%= h.getIdHabitacion() %>&valor=MANTENIMIENTO">
                            <i class="bi bi-tools me-2 text-secondary"></i> Mantenimiento
                        </a>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li>
                        <a class="dropdown-item"
                           href="<%= ctx %>/bloqueos?accion=nuevo&idHabitacion=<%= h.getIdHabitacion() %>">
                            <i class="bi bi-lock me-2"></i> Bloquear por fechas
                        </a>
                    </li>
<%
            }
            if (esAdmin) {
%>
                    <li><hr class="dropdown-divider"></li>
                    <li>
                        <a class="dropdown-item text-danger"
                           href="<%= ctx %>/habitaciones?accion=eliminar&id=<%= h.getIdHabitacion() %>"
                           onclick="return confirmar('Se dara de baja la habitacion <%= Vista.esc(h.getNumero()) %>. Sus reservas historicas se conservan. Continuar?');">
                            <i class="bi bi-trash me-2"></i> Dar de baja
                        </a>
                    </li>
<%
            }
%>
                </ul>
            </div>
        </div>
    </div>
<%
        }
%>
</div>

<p class="text-muted mt-3 mb-0" style="font-size:.8rem;">
    <%= habitaciones.size() %> habitacion(es) listada(s).
</p>
<%
    }
%>

<jsp:include page="../comun/pie.jsp" />
