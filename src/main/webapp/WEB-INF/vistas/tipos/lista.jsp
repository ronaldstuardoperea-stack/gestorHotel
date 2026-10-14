<%--
    tipos/lista.jsp

    Catalogo de tipos de habitacion y sus tarifas base. Solo el
    administrador llega hasta aqui, porque de este catalogo dependen los
    precios de todo el hotel.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.hotel.modelo.TipoHabitacion" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Tipos y tarifas" />
    <jsp:param name="subtitulo" value="Catalogo de categorias de habitacion" />
    <jsp:param name="activo" value="tipos" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    List<TipoHabitacion> tipos = (List<TipoHabitacion>) request.getAttribute("tipos");
    if (tipos == null) { tipos = new ArrayList<TipoHabitacion>(); }
%>

<div class="d-flex justify-content-between align-items-center mb-3">
    <div class="text-muted" style="font-size:.85rem;">
        La tarifa base es el precio de lista. La temporada vigente le
        aplica un recargo o un descuento al cotizar una reserva.
    </div>
    <a href="<%= ctx %>/tipos?accion=nuevo" class="btn btn-sm btn-oro">
        <i class="bi bi-plus-lg me-1"></i> Nuevo tipo
    </a>
</div>

<div class="card">
<%
    if (tipos.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        Aun no hay tipos de habitacion registrados.
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Tipo</th>
                    <th>Capacidad</th>
                    <th class="numero-tabla">Tarifa base</th>
                    <th class="numero-tabla">Habitaciones</th>
                    <th>Estado</th>
                    <th class="text-end"></th>
                </tr>
            </thead>
            <tbody>
<%
        for (TipoHabitacion t : tipos) {
%>
                <tr class="<%= t.isActivo() ? "" : "opacity-50" %>">
                    <td>
                        <div class="fw-semibold"><%= Vista.esc(t.getNombre()) %></div>
<%
            if (t.getDescripcion() != null && !t.getDescripcion().isEmpty()) {
%>
                        <div class="text-muted" style="font-size:.76rem;">
                            <%= Vista.esc(t.getDescripcion()) %>
                        </div>
<%
            }
%>
                    </td>

                    <td style="font-size:.84rem;">
                        <%= t.getCapacidadAdultos() %> adulto(s)
<%
            if (t.getCapacidadNinos() > 0) {
%>
                        + <%= t.getCapacidadNinos() %> nino(s)
<%
            }
%>
                        <div class="text-muted" style="font-size:.72rem;">
                            maximo <%= t.getCapacidadTotal() %> personas
                        </div>
                    </td>

                    <td class="numero-tabla fw-bold" style="color:var(--hotel-azul);">
                        Q <%= Vista.moneda(t.getTarifaBase()) %>
                    </td>

                    <td class="numero-tabla"><%= t.getTotalHabitaciones() %></td>

                    <td>
                        <span class="badge bg-<%= t.isActivo() ? "success" : "secondary" %>">
                            <%= t.isActivo() ? "Activo" : "Inactivo" %>
                        </span>
                    </td>

                    <td class="text-end">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="<%= ctx %>/tipos?accion=editar&id=<%= t.getIdTipo() %>">
                            <i class="bi bi-pencil"></i>
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

<jsp:include page="../comun/pie.jsp" />
