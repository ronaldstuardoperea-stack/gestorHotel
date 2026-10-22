<%--
    cargos/formulario.jsp

    Registro de consumos a la cuenta de la habitacion: restaurante,
    minibar, lavanderia, spa y demas gastos del huesped.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.Ocupante" %>
<%@ page import="com.hotel.modelo.Cargo" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Consumos y gastos" />
    <jsp:param name="subtitulo" value="Cargos a la cuenta de la habitacion" />
    <jsp:param name="activo" value="reservas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean puedeOperar = (u != null && u.isOperador());

    Reserva reserva = (Reserva) request.getAttribute("reserva");

    List<Ocupante> ocupantes = (List<Ocupante>) request.getAttribute("ocupantes");
    if (ocupantes == null) { ocupantes = new ArrayList<Ocupante>(); }

    List<Cargo> cargos = (List<Cargo>) request.getAttribute("cargos");
    if (cargos == null) { cargos = new ArrayList<Cargo>(); }

    BigDecimal totalConsumos = (BigDecimal) request.getAttribute("totalConsumos");
%>

<div class="mb-3">
    <a href="<%= ctx %>/reservas?accion=detalle&id=<%= reserva.getIdReserva() %>"
       class="btn btn-sm btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i>
        Volver a la reserva <%= Vista.esc(reserva.getCodigo()) %>
    </a>
</div>

<div class="row g-3">

    <%-- --------------------------------- Formulario --%>
    <div class="col-lg-5">
        <div class="card">
            <div class="card-header">
                <i class="bi bi-cart-plus me-2"></i> Anotar un consumo
            </div>

<%
    if (!reserva.isAdmiteConsumos()) {
%>
            <div class="card-body">
                <div class="alert alert-warning mb-0" style="font-size:.85rem;">
                    <i class="bi bi-info-circle me-1"></i>
                    La reserva <%= Vista.esc(reserva.getCodigo()) %> esta en estado
                    <strong><%= Vista.esc(reserva.getEstadoLegible()) %></strong>.
                    Solo se pueden anotar consumos mientras el huesped
                    esta hospedado.
                </div>
            </div>
<%
    } else {
%>
            <form method="post" action="<%= ctx %>/cargos" id="formCargo">
                <input type="hidden" name="idReserva" value="<%= reserva.getIdReserva() %>">

                <div class="card-body">

                    <div class="mb-3">
                        <label for="concepto" class="form-label obligatorio">
                            Concepto
                        </label>
                        <input type="text" class="form-control" id="concepto"
                               name="concepto" maxlength="150" required
                               placeholder="Cena a la carta, lavado de camisas...">
                    </div>

                    <div class="mb-3">
                        <label for="categoria" class="form-label obligatorio">
                            Categoria
                        </label>
                        <select class="form-select" id="categoria" name="categoria">
                            <option value="RESTAURANTE">Restaurante</option>
                            <option value="MINIBAR">Minibar</option>
                            <option value="LAVANDERIA">Lavanderia</option>
                            <option value="SPA">Spa</option>
                            <option value="TELEFONO">Telefono</option>
                            <option value="TRANSPORTE">Transporte</option>
                            <option value="OTRO" selected>Otro</option>
                        </select>
                    </div>

                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label for="cantidad" class="form-label obligatorio">
                                Cantidad
                            </label>
                            <input type="number" class="form-control" id="cantidad"
                                   name="cantidad" step="0.01" min="0.01"
                                   value="1" required oninput="calcularTotal()">
                        </div>
                        <div class="col-6">
                            <label for="precioUnitario" class="form-label obligatorio">
                                Precio unitario (Q)
                            </label>
                            <input type="number" class="form-control" id="precioUnitario"
                                   name="precioUnitario" step="0.01" min="0"
                                   required oninput="calcularTotal()">
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="idOcupante" class="form-label">
                            A nombre de
                        </label>
                        <select class="form-select" id="idOcupante" name="idOcupante">
                            <option value="0">La habitacion (sin asignar)</option>
<%
        for (Ocupante o : ocupantes) {
%>
                            <option value="<%= o.getIdOcupante() %>">
                                <%= Vista.esc(o.getNombreCompleto()) %>
                            </option>
<%
        }
%>
                        </select>
                        <div class="form-text">
                            Si lo asigna a un huesped, podra facturarle sus
                            consumos por separado.
                        </div>
                    </div>

                    <div class="alert alert-light border mb-0 d-flex
                                justify-content-between align-items-center">
                        <span class="text-muted">Total del cargo</span>
                        <strong id="totalCargo" class="fs-5"
                                style="color:var(--hotel-azul);">Q 0.00</strong>
                    </div>
                </div>

                <div class="card-footer bg-white text-end">
                    <button type="submit" class="btn btn-hotel">
                        <i class="bi bi-plus-lg me-1"></i> Cargar a la cuenta
                    </button>
                </div>
            </form>
<%
    }
%>
        </div>
    </div>

    <%-- ------------------------------ Consumos ya anotados --%>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header d-flex justify-content-between align-items-center">
                <span>
                    <i class="bi bi-list-ul me-2"></i>
                    Consumos de la habitacion <%= Vista.esc(reserva.getNumeroHabitacion()) %>
                </span>
                <span class="fw-bold" style="color:var(--hotel-azul);">
                    Q <%= Vista.moneda(totalConsumos) %>
                </span>
            </div>

<%
    if (cargos.isEmpty()) {
%>
            <div class="card-body text-center text-muted py-5">
                <i class="bi bi-cart-x fs-1 opacity-25 d-block mb-2"></i>
                Todavia no hay consumos anotados en esta reserva.
            </div>
<%
    } else {
%>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead>
                        <tr>
                            <th>Concepto</th>
                            <th>A nombre de</th>
                            <th class="numero-tabla">Monto</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
<%
        for (Cargo cg : cargos) {
%>
                        <tr>
                            <td>
                                <i class="bi bi-<%= cg.getIconoCategoria() %> text-muted me-1"></i>
                                <%= Vista.esc(cg.getConcepto()) %>
                                <div class="text-muted" style="font-size:.7rem;">
                                    <%= Vista.cantidad(cg.getCantidad()) %> x
                                    Q <%= Vista.moneda(cg.getPrecioUnitario()) %>
                                    &middot; <%= Vista.txt(cg.getFecha()) %>
                                </div>
                            </td>
                            <td style="font-size:.82rem;">
<%
            if (cg.getNombreOcupante() == null || cg.getNombreOcupante().isEmpty()) {
%>
                                <span class="text-muted">La habitacion</span>
<%
            } else {
%>
                                <%= Vista.esc(cg.getNombreOcupante()) %>
<%
            }
%>
                            </td>
                            <td class="numero-tabla fw-semibold">
                                <%= Vista.moneda(cg.getMonto()) %>
                            </td>
                            <td class="text-end">
<%
            if (cg.isPendienteDeFacturar() && puedeOperar) {
%>
                                <a class="btn btn-sm btn-outline-danger"
                                   href="<%= ctx %>/cargos?accion=eliminar&id=<%= cg.getIdCargo() %>"
                                   title="Eliminar"
                                   onclick="return confirmar('Eliminar el consumo <%= Vista.esc(cg.getConcepto()) %>?');">
                                    <i class="bi bi-trash"></i>
                                </a>
<%
            } else {
%>
                                <span class="badge bg-success-subtle text-success-emphasis"
                                      title="Ya fue facturado">
                                    <i class="bi bi-lock"></i>
                                </span>
<%
            }
%>
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
    </div>
</div>

<script>
    /* Muestra el total del cargo mientras se escribe, para que el
       recepcionista vea lo que va a cobrar antes de guardar. */
    function calcularTotal() {
        var cantidad = parseFloat(document.getElementById('cantidad').value) || 0;
        var precio   = parseFloat(document.getElementById('precioUnitario').value) || 0;
        document.getElementById('totalCargo').textContent =
            'Q ' + (cantidad * precio).toFixed(2);
    }
</script>

<jsp:include page="../comun/pie.jsp" />
