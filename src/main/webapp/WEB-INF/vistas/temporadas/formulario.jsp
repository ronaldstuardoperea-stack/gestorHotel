<%--
    temporadas/formulario.jsp

    Alta y edicion de una temporada tarifaria.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.hotel.modelo.Temporada" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Temporadas" />
    <jsp:param name="subtitulo" value="Periodo con tarifa especial" />
    <jsp:param name="activo" value="temporadas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Temporada temporada = (Temporada) request.getAttribute("temporada");
    boolean edicion = (temporada != null);
%>

<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card">
            <div class="card-header">
                <i class="bi bi-calendar-range me-2"></i>
<%
    if (edicion) {
%>
                Editar <%= Vista.esc(temporada.getNombre()) %>
<%
    } else {
%>
                Nueva temporada
<%
    }
%>
            </div>

            <form method="post" action="<%= ctx %>/temporadas">
                <input type="hidden" name="idTemporada"
                       value="<%= edicion ? temporada.getIdTemporada() : 0 %>">

                <div class="card-body">

                    <div class="mb-3">
                        <label for="nombre" class="form-label obligatorio">
                            Nombre de la temporada
                        </label>
                        <input type="text" class="form-control" id="nombre" name="nombre"
                               value="<%= edicion ? Vista.esc(temporada.getNombre()) : "" %>"
                               maxlength="60" required
                               placeholder="Semana Santa, Fin de Ano, Temporada Baja...">
                    </div>

                    <div class="row g-3 mb-3">
                        <div class="col-md-6">
                            <label for="fechaInicio" class="form-label obligatorio">
                                Desde
                            </label>
                            <input type="date" class="form-control" id="fechaInicio"
                                   name="fechaInicio"
                                   value="<%= edicion ? Vista.txt(temporada.getFechaInicio()) : "" %>"
                                   required>
                        </div>

                        <div class="col-md-6">
                            <label for="fechaFin" class="form-label obligatorio">
                                Hasta
                            </label>
                            <input type="date" class="form-control" id="fechaFin"
                                   name="fechaFin"
                                   value="<%= edicion ? Vista.txt(temporada.getFechaFin()) : "" %>"
                                   required>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="factor" class="form-label obligatorio">
                            Factor sobre la tarifa base
                        </label>
                        <input type="number" class="form-control" id="factor" name="factor"
                               step="0.01" min="0.10" max="3.00"
                               value="<%= edicion ? Vista.txt(temporada.getFactor()) : "1.00" %>"
                               required oninput="explicarFactor()">
                        <div class="form-text" id="explicacion">
                            1.00 mantiene el precio de lista.
                        </div>
                    </div>

                    <div class="alert alert-light border" style="font-size:.82rem;">
                        <div class="fw-semibold mb-1">Ejemplo con una habitacion de Q 550.00</div>
                        <div class="d-flex justify-content-between">
                            <span class="text-muted">Precio por noche en esta temporada</span>
                            <strong id="ejemplo" style="color:var(--hotel-azul);">Q 550.00</strong>
                        </div>
                    </div>

                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="activo"
                               name="activo" value="1"
                               <%= Vista.marcado(!edicion || temporada.isActivo()) %>>
                        <label class="form-check-label" for="activo">
                            Esta temporada se toma en cuenta al cotizar reservas
                        </label>
                    </div>
                </div>

                <div class="card-footer bg-white d-flex gap-2 justify-content-end">
                    <a href="<%= ctx %>/temporadas" class="btn btn-outline-secondary">Cancelar</a>
                    <button type="submit" class="btn btn-hotel">
                        <i class="bi bi-save me-1"></i> Guardar
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    /* Traduce el factor a lenguaje llano y muestra un ejemplo, para que
       nadie tenga que adivinar que significa "1.35". */
    function explicarFactor() {
        var factor = parseFloat(document.getElementById('factor').value) || 1;
        var variacion = Math.round((factor - 1) * 100);
        var texto;

        if (variacion === 0) {
            texto = 'Mantiene el precio de lista, sin recargo ni descuento.';
        } else if (variacion > 0) {
            texto = 'Aplica un recargo del ' + variacion + '% sobre el precio de lista.';
        } else {
            texto = 'Aplica un descuento del ' + Math.abs(variacion) + '% sobre el precio de lista.';
        }

        document.getElementById('explicacion').textContent = texto;
        document.getElementById('ejemplo').textContent = 'Q ' + (550 * factor).toFixed(2);
    }

    explicarFactor();
</script>

<jsp:include page="../comun/pie.jsp" />
