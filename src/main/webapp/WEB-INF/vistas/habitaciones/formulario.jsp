<%--
    habitaciones/formulario.jsp

    Alta y edicion de una habitacion. El mismo archivo sirve para ambas:
    si llega el objeto 'habitacion' se esta editando, y si no, se esta
    creando. Asi no hay dos formularios que mantener sincronizados.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.hotel.modelo.Habitacion" %>
<%@ page import="com.hotel.modelo.TipoHabitacion" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Habitaciones" />
    <jsp:param name="subtitulo" value="Registro de una habitacion del inventario" />
    <jsp:param name="activo" value="habitaciones" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Habitacion habitacion = (Habitacion) request.getAttribute("habitacion");
    boolean edicion = (habitacion != null);

    List<TipoHabitacion> tipos = (List<TipoHabitacion>) request.getAttribute("tipos");
    if (tipos == null) { tipos = new java.util.ArrayList<TipoHabitacion>(); }

    String estadoActual = edicion ? habitacion.getEstado() : "DISPONIBLE";
%>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card">
            <div class="card-header">
                <i class="bi bi-house-door me-2"></i>
<%
    if (edicion) {
%>
                Editar habitacion <%= Vista.esc(habitacion.getNumero()) %>
<%
    } else {
%>
                Nueva habitacion
<%
    }
%>
            </div>

            <form method="post" action="<%= ctx %>/habitaciones">
                <%-- Llevar el id oculto es lo que le dice al Servlet si
                     debe insertar (id = 0) o actualizar. --%>
                <input type="hidden" name="idHabitacion"
                       value="<%= edicion ? habitacion.getIdHabitacion() : 0 %>">

                <div class="card-body">
                    <div class="row g-3">

                        <div class="col-md-4">
                            <label for="numero" class="form-label obligatorio">Numero</label>
                            <input type="text" class="form-control" id="numero" name="numero"
                                   value="<%= edicion ? Vista.esc(habitacion.getNumero()) : "" %>"
                                   maxlength="10" placeholder="101" required>
                            <div class="form-text">No se puede repetir en el hotel.</div>
                        </div>

                        <div class="col-md-4">
                            <label for="piso" class="form-label obligatorio">Piso</label>
                            <input type="number" class="form-control" id="piso" name="piso"
                                   value="<%= edicion ? habitacion.getPiso() : 1 %>"
                                   min="1" max="50" required>
                        </div>

                        <div class="col-md-4">
                            <label for="idTipo" class="form-label obligatorio">Tipo</label>
                            <select class="form-select" id="idTipo" name="idTipo" required>
                                <option value="">Seleccione...</option>
<%
    for (TipoHabitacion t : tipos) {
        boolean elegido = edicion && habitacion.getIdTipo() == t.getIdTipo();
%>
                                <option value="<%= t.getIdTipo() %>"<%= Vista.selecto(elegido) %>>
                                    <%= Vista.esc(t.getNombre()) %>
                                    (Q <%= Vista.moneda(t.getTarifaBase()) %>)
                                </option>
<%
    }
%>
                            </select>
                            <div class="form-text">La tarifa la define el tipo.</div>
                        </div>

                        <div class="col-md-6">
                            <label for="estado" class="form-label obligatorio">Estado actual</label>
                            <select class="form-select" id="estado" name="estado" required>
                                <option value="DISPONIBLE"<%=
                                    Vista.selecto("DISPONIBLE".equals(estadoActual)) %>>Disponible</option>
                                <option value="OCUPADA"<%=
                                    Vista.selecto("OCUPADA".equals(estadoActual)) %>>Ocupada</option>
                                <option value="LIMPIEZA"<%=
                                    Vista.selecto("LIMPIEZA".equals(estadoActual)) %>>En limpieza</option>
                                <option value="MANTENIMIENTO"<%=
                                    Vista.selecto("MANTENIMIENTO".equals(estadoActual)) %>>Mantenimiento</option>
                                <option value="BLOQUEADA"<%=
                                    Vista.selecto("BLOQUEADA".equals(estadoActual)) %>>Bloqueada</option>
                            </select>
                        </div>

                        <div class="col-12">
                            <label for="descripcion" class="form-label">Descripcion</label>
                            <textarea class="form-control" id="descripcion" name="descripcion"
                                      rows="2" maxlength="255"
                                      placeholder="Vista al jardin, balcon, adaptada para silla de ruedas..."><%=
                                      edicion ? Vista.esc(habitacion.getDescripcion()) : "" %></textarea>
                        </div>
                    </div>
                </div>

                <div class="card-footer bg-white d-flex gap-2 justify-content-end">
                    <a href="<%= ctx %>/habitaciones" class="btn btn-outline-secondary">Cancelar</a>
                    <button type="submit" class="btn btn-hotel">
                        <i class="bi bi-save me-1"></i> Guardar
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="../comun/pie.jsp" />
