<%--
    temporadas/lista.jsp

    Calendario tarifario del hotel. Cada temporada define un
    multiplicador que ajusta el precio de lista en ese periodo.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.Temporada" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Temporadas" />
    <jsp:param name="subtitulo" value="Calendario de tarifas del hotel" />
    <jsp:param name="activo" value="temporadas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    List<Temporada> temporadas = (List<Temporada>) request.getAttribute("temporadas");
    if (temporadas == null) { temporadas = new ArrayList<Temporada>(); }

    Temporada vigente = (Temporada) request.getAttribute("vigente");
%>

<div class="row g-3 mb-3">
    <div class="col-md-8">
        <div class="alert alert-light border mb-0 h-100 d-flex align-items-center"
             style="font-size:.85rem;">
            <div>
                <i class="bi bi-info-circle me-1" style="color:var(--hotel-azul);"></i>
                El <strong>factor</strong> multiplica la tarifa base del tipo de
                habitacion: <code>1.00</code> es el precio normal,
                <code>1.40</code> aplica un recargo del 40% y
                <code>0.85</code> un descuento del 15%.
            </div>
        </div>
    </div>

    <div class="col-md-4">
        <div class="indicador h-100">
            <div class="etiqueta">Temporada vigente hoy</div>
<%
    if (vigente == null) {
%>
            <div class="valor" style="font-size:1.1rem;">Tarifa normal</div>
            <div class="nota">Hoy no hay ninguna temporada activa.</div>
<%
    } else {
%>
            <div class="valor" style="font-size:1.1rem;">
                <%= Vista.esc(vigente.getNombre()) %>
            </div>
            <div class="nota">
                <%= Vista.esc(vigente.getVariacionPorcentual()) %> sobre el precio de lista
            </div>
<%
    }
%>
        </div>
    </div>
</div>

<div class="d-flex justify-content-end mb-3">
    <a href="<%= ctx %>/temporadas?accion=nueva" class="btn btn-sm btn-oro">
        <i class="bi bi-plus-lg me-1"></i> Nueva temporada
    </a>
</div>

<div class="card">
<%
    if (temporadas.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        Aun no hay temporadas registradas. Sin temporadas, todas las
        reservas se cotizan con la tarifa de lista.
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Temporada</th>
                    <th>Periodo</th>
                    <th class="numero-tabla">Factor</th>
                    <th>Efecto</th>
                    <th>Estado</th>
                    <th class="text-end">Acciones</th>
                </tr>
            </thead>
            <tbody>
<%
        for (Temporada t : temporadas) {
            String tono = t.isRecargo() ? "warning" : "info";
%>
                <tr class="<%= t.isActivo() ? "" : "opacity-50" %>">
                    <td>
                        <div class="fw-semibold"><%= Vista.esc(t.getNombre()) %></div>
<%
            if (t.isVigente()) {
%>
                        <span class="badge bg-success"
                              style="font-size:.62rem;">Vigente hoy</span>
<%
            }
%>
                    </td>

                    <td style="font-size:.84rem;">
                        <%= Vista.txt(t.getFechaInicio()) %> &rarr; <%= Vista.txt(t.getFechaFin()) %>
                    </td>

                    <td class="numero-tabla">
                        <%= Vista.moneda(t.getFactor()) %>
                    </td>

                    <td>
                        <span class="badge bg-<%= tono %>-subtle text-<%= tono %>-emphasis">
                            <%= Vista.esc(t.getVariacionPorcentual()) %>
                        </span>
                    </td>

                    <td>
                        <span class="badge bg-<%= t.isActivo() ? "success" : "secondary" %>">
                            <%= t.isActivo() ? "Activa" : "Inactiva" %>
                        </span>
                    </td>

                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/temporadas?accion=editar&id=<%= t.getIdTemporada() %>">
                                <i class="bi bi-pencil"></i>
                            </a>
                            <a class="btn btn-outline-danger"
                               href="<%= ctx %>/temporadas?accion=eliminar&id=<%= t.getIdTemporada() %>"
                               onclick="return confirmar('Se eliminara la temporada <%= Vista.esc(t.getNombre()) %>. Las reservas ya creadas conservan su tarifa. Continuar?');">
                                <i class="bi bi-trash"></i>
                            </a>
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
