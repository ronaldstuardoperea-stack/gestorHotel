<%--
    bloqueos/formulario.jsp

    Registro de un bloqueo: saca una habitacion de la venta durante un
    rango de fechas.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.hotel.modelo.Habitacion" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Bloquear habitacion" />
    <jsp:param name="subtitulo" value="Retirar una habitacion de la venta" />
    <jsp:param name="activo" value="bloqueos" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    List<Habitacion> habitaciones =
            (List<Habitacion>) request.getAttribute("habitaciones");
    if (habitaciones == null) { habitaciones = new java.util.ArrayList<Habitacion>(); }

    Integer preseleccion = (Integer) request.getAttribute("idHabitacionPreseleccionada");
    int idPreseleccionado = (preseleccion == null) ? 0 : preseleccion.intValue();
%>

<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card">
            <div class="card-header">
                <i class="bi bi-lock me-2"></i> Nuevo bloqueo
            </div>

            <form method="post" action="<%= ctx %>/bloqueos">
                <div class="card-body">

                    <div class="mb-3">
                        <label for="idHabitacion" class="form-label obligatorio">
                            Habitacion
                        </label>
                        <select class="form-select" id="idHabitacion" name="idHabitacion" required>
                            <option value="">Seleccione una habitacion...</option>
<%
    for (Habitacion h : habitaciones) {
%>
                            <option value="<%= h.getIdHabitacion() %>"
                                    <%= Vista.selecto(idPreseleccionado == h.getIdHabitacion()) %>>
                                <%= Vista.esc(h.getNumero()) %> &mdash;
                                <%= Vista.esc(h.getNombreTipo()) %>
                                (piso <%= h.getPiso() %>)
                            </option>
<%
    }
%>
                        </select>
                    </div>

                    <div class="row g-3 mb-3">
                        <div class="col-md-6">
                            <label for="fechaInicio" class="form-label obligatorio">
                                Desde
                            </label>
                            <input type="date" class="form-control" id="fechaInicio"
                                   name="fechaInicio" required>
                        </div>

                        <div class="col-md-6">
                            <label for="fechaFin" class="form-label obligatorio">
                                Hasta
                            </label>
                            <input type="date" class="form-control" id="fechaFin"
                                   name="fechaFin" required>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="motivo" class="form-label obligatorio">
                            Motivo del bloqueo
                        </label>
                        <input type="text" class="form-control" id="motivo" name="motivo"
                               maxlength="150" required
                               placeholder="Remodelacion del bano, cambio de alfombra, uso interno...">
                        <div class="form-text">
                            Quedara registrado junto con su nombre y la fecha, como
                            constancia de por que la habitacion no estuvo a la venta.
                        </div>
                    </div>

                    <div class="alert alert-warning mb-0 py-2" style="font-size:.84rem;">
                        <i class="bi bi-exclamation-triangle me-1"></i>
                        Si la habitacion ya tiene reservas vigentes en esas fechas, el
                        sistema no permitira el bloqueo: primero hay que reubicar o
                        cancelar esas reservas.
                    </div>
                </div>

                <div class="card-footer bg-white d-flex gap-2 justify-content-end">
                    <a href="<%= ctx %>/bloqueos" class="btn btn-outline-secondary">Cancelar</a>
                    <button type="submit" class="btn btn-hotel">
                        <i class="bi bi-lock me-1"></i> Registrar bloqueo
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="../comun/pie.jsp" />
