<%--
    tipos/formulario.jsp

    Alta y edicion de un tipo de habitacion con su tarifa base.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.hotel.modelo.TipoHabitacion" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Tipos y tarifas" />
    <jsp:param name="subtitulo" value="Categoria de habitacion" />
    <jsp:param name="activo" value="tipos" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    TipoHabitacion tipo = (TipoHabitacion) request.getAttribute("tipo");
    boolean edicion = (tipo != null);
%>

<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card">
            <div class="card-header">
                <i class="bi bi-tags me-2"></i>
<%
    if (edicion) {
%>
                Editar <%= Vista.esc(tipo.getNombre()) %>
<%
    } else {
%>
                Nuevo tipo de habitacion
<%
    }
%>
            </div>

            <form method="post" action="<%= ctx %>/tipos">
                <input type="hidden" name="idTipo" value="<%= edicion ? tipo.getIdTipo() : 0 %>">

                <div class="card-body">

                    <div class="mb-3">
                        <label for="nombre" class="form-label obligatorio">
                            Nombre de la categoria
                        </label>
                        <input type="text" class="form-control" id="nombre" name="nombre"
                               value="<%= edicion ? Vista.esc(tipo.getNombre()) : "" %>"
                               maxlength="60" required
                               placeholder="Sencilla, Doble, Suite Ejecutiva...">
                    </div>

                    <div class="mb-3">
                        <label for="descripcion" class="form-label">Descripcion</label>
                        <textarea class="form-control" id="descripcion" name="descripcion"
                                  rows="2" maxlength="255"
                                  placeholder="Que incluye la habitacion..."><%=
                                  edicion ? Vista.esc(tipo.getDescripcion()) : "" %></textarea>
                    </div>

                    <div class="row g-3 mb-3">
                        <div class="col-md-4">
                            <label for="capacidadAdultos" class="form-label obligatorio">
                                Adultos
                            </label>
                            <input type="number" class="form-control" id="capacidadAdultos"
                                   name="capacidadAdultos" min="1" max="20"
                                   value="<%= edicion ? tipo.getCapacidadAdultos() : 2 %>" required>
                        </div>

                        <div class="col-md-4">
                            <label for="capacidadNinos" class="form-label obligatorio">
                                Ninos adicionales
                            </label>
                            <input type="number" class="form-control" id="capacidadNinos"
                                   name="capacidadNinos" min="0" max="20"
                                   value="<%= edicion ? tipo.getCapacidadNinos() : 0 %>" required>
                        </div>

                        <div class="col-md-4">
                            <label for="tarifaBase" class="form-label obligatorio">
                                Tarifa base (Q)
                            </label>
                            <input type="number" class="form-control" id="tarifaBase"
                                   name="tarifaBase" step="0.01" min="0.01"
                                   value="<%= edicion ? Vista.txt(tipo.getTarifaBase()) : "" %>"
                                   required>
                        </div>
                    </div>

                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="activo"
                               name="activo" value="1"
                               <%= Vista.marcado(!edicion || tipo.isActivo()) %>>
                        <label class="form-check-label" for="activo">
                            Esta categoria se sigue ofreciendo a la venta
                        </label>
                    </div>

<%
    if (edicion) {
%>
                    <div class="alert alert-info mt-3 mb-0 py-2" style="font-size:.82rem;">
                        <i class="bi bi-info-circle me-1"></i>
                        Cambiar la tarifa afecta solo a las reservas futuras.
                        Las ya creadas conservan el precio que se congelo al
                        momento de reservarlas.
                    </div>
<%
    }
%>
                </div>

                <div class="card-footer bg-white d-flex gap-2 justify-content-end">
                    <a href="<%= ctx %>/tipos" class="btn btn-outline-secondary">Cancelar</a>
                    <button type="submit" class="btn btn-hotel">
                        <i class="bi bi-save me-1"></i> Guardar
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="../comun/pie.jsp" />
