<%--
    facturas/formulario.jsp

    Emision de facturas. Aqui se resuelve el requerimiento de
    "facturacion flexible (por reserva, habitacion, huesped y gastos
    separados)": el cajero elige que incluye el documento y el sistema
    arma el detalle en consecuencia.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.modelo.Cliente" %>
<%@ page import="com.hotel.modelo.Ocupante" %>
<%@ page import="com.hotel.modelo.Cargo" %>
<%@ page import="com.hotel.modelo.Factura" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Emitir factura" />
    <jsp:param name="subtitulo" value="Facturacion flexible de la reserva" />
    <jsp:param name="activo" value="facturas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Reserva reserva = (Reserva) request.getAttribute("reserva");
    Cliente cliente = (Cliente) request.getAttribute("cliente");

    List<Ocupante> ocupantes = (List<Ocupante>) request.getAttribute("ocupantes");
    if (ocupantes == null) { ocupantes = new ArrayList<Ocupante>(); }

    List<Cargo> pendientes = (List<Cargo>) request.getAttribute("pendientes");
    if (pendientes == null) { pendientes = new ArrayList<Cargo>(); }

    List<Factura> emitidas = (List<Factura>) request.getAttribute("emitidas");
    if (emitidas == null) { emitidas = new ArrayList<Factura>(); }

    BigDecimal totalPendiente = (BigDecimal) request.getAttribute("totalPendiente");
    if (totalPendiente == null) { totalPendiente = BigDecimal.ZERO; }

    BigDecimal totalHospedaje = reserva.getTotalHospedaje();
    if (totalHospedaje == null) { totalHospedaje = BigDecimal.ZERO; }

    BigDecimal totalCompleta = totalHospedaje.add(totalPendiente);

    boolean sinOcupantes = ocupantes.isEmpty();
%>

<div class="mb-3">
    <a href="<%= ctx %>/reservas?accion=detalle&id=<%= reserva.getIdReserva() %>"
       class="btn btn-sm btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i>
        Volver a la reserva <%= Vista.esc(reserva.getCodigo()) %>
    </a>
</div>

<form method="post" action="<%= ctx %>/facturas" id="formFactura">
    <input type="hidden" name="accion" value="emitir">
    <input type="hidden" name="idReserva" value="<%= reserva.getIdReserva() %>">

    <div class="row g-3">

        <%-- ============================ Tipo de documento --%>
        <div class="col-lg-7">
            <div class="card mb-3">
                <div class="card-header">
                    <i class="bi bi-receipt me-2"></i> Que incluye la factura
                </div>
                <div class="card-body">

                    <%-- Cada opcion lleva su total previsto para que el
                         cajero compare antes de elegir. --%>

                    <label class="d-flex gap-3 p-3 mb-2 border rounded-3" style="cursor:pointer;">
                        <input class="form-check-input mt-1 flex-shrink-0" type="radio"
                               name="tipo" value="COMPLETA" checked
                               onchange="cambiarTipo(this.value)">
                        <div class="flex-grow-1">
                            <div class="fw-semibold">Factura completa</div>
                            <div class="text-muted" style="font-size:.8rem;">
                                Las noches de hospedaje mas todos los consumos
                                pendientes, en un solo documento.
                            </div>
                        </div>
                        <div class="text-end flex-shrink-0">
                            <div class="fw-bold" style="color:var(--hotel-azul);">
                                Q <%= Vista.moneda(totalCompleta) %>
                            </div>
                        </div>
                    </label>

                    <label class="d-flex gap-3 p-3 mb-2 border rounded-3" style="cursor:pointer;">
                        <input class="form-check-input mt-1 flex-shrink-0" type="radio"
                               name="tipo" value="HOSPEDAJE"
                               onchange="cambiarTipo(this.value)">
                        <div class="flex-grow-1">
                            <div class="fw-semibold">Solo hospedaje</div>
                            <div class="text-muted" style="font-size:.8rem;">
                                Unicamente las <%= reserva.getNoches() %> noches de habitacion.
                                Se usa cuando la empresa paga el alojamiento y el
                                huesped sus extras.
                            </div>
                        </div>
                        <div class="text-end flex-shrink-0">
                            <div class="fw-bold" style="color:var(--hotel-azul);">
                                Q <%= Vista.moneda(totalHospedaje) %>
                            </div>
                        </div>
                    </label>

                    <label class="d-flex gap-3 p-3 mb-2 border rounded-3" style="cursor:pointer;">
                        <input class="form-check-input mt-1 flex-shrink-0" type="radio"
                               name="tipo" value="CONSUMOS"
                               onchange="cambiarTipo(this.value)">
                        <div class="flex-grow-1">
                            <div class="fw-semibold">Solo consumos y gastos</div>
                            <div class="text-muted" style="font-size:.8rem;">
                                Restaurante, minibar, lavanderia y demas cargos que
                                aun no se han facturado.
                            </div>
                        </div>
                        <div class="text-end flex-shrink-0">
                            <div class="fw-bold" style="color:var(--hotel-azul);">
                                Q <%= Vista.moneda(totalPendiente) %>
                            </div>
                        </div>
                    </label>

                    <label class="d-flex gap-3 p-3 mb-0 border rounded-3" style="cursor:pointer;">
                        <input class="form-check-input mt-1 flex-shrink-0" type="radio"
                               name="tipo" value="OCUPANTE"
                               onchange="cambiarTipo(this.value)"
                               <%= sinOcupantes ? "disabled" : "" %>>
                        <div class="flex-grow-1">
                            <div class="fw-semibold">
                                Consumos de un huesped en particular
<%
    if (sinOcupantes) {
%>
                                <span class="badge bg-secondary" style="font-size:.6rem;">
                                    Sin ocupantes registrados
                                </span>
<%
    }
%>
                            </div>
                            <div class="text-muted" style="font-size:.8rem;">
                                Para dividir la cuenta cuando varias personas
                                comparten la habitacion.
                            </div>

                            <div class="mt-2" id="cajaOcupante" style="display:none;">
                                <select class="form-select form-select-sm" name="idOcupante">
                                    <option value="0">Seleccione al huesped...</option>
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
                            </div>
                        </div>
                    </label>
                </div>
            </div>

            <%-- ------------------- Consumos pendientes --%>
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-cart me-2"></i> Consumos pendientes de facturar</span>
                    <span class="fw-bold" style="color:var(--hotel-azul);">
                        Q <%= Vista.moneda(totalPendiente) %>
                    </span>
                </div>

<%
    if (pendientes.isEmpty()) {
%>
                <div class="card-body text-center text-muted py-4">
                    No hay consumos pendientes de facturar.
                </div>
<%
    } else {
%>
                <div class="table-responsive">
                    <table class="table table-sm mb-0">
                        <thead>
                            <tr>
                                <th>Concepto</th>
                                <th>A nombre de</th>
                                <th class="numero-tabla">Monto</th>
                            </tr>
                        </thead>
                        <tbody>
<%
        for (Cargo cg : pendientes) {
%>
                            <tr>
                                <td>
                                    <i class="bi bi-<%= cg.getIconoCategoria() %> text-muted me-1"></i>
                                    <%= Vista.esc(cg.getConcepto()) %>
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
                                <td class="numero-tabla">
                                    <%= Vista.moneda(cg.getMonto()) %>
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

        <%-- ============================== Datos del receptor --%>
        <div class="col-lg-5">
            <div class="card mb-3">
                <div class="card-header">
                    <i class="bi bi-person-vcard me-2"></i> Datos del receptor
                </div>
                <div class="card-body">

                    <div class="mb-3">
                        <label for="nombreReceptor" class="form-label">Nombre</label>
                        <input type="text" class="form-control" id="nombreReceptor"
                               name="nombreReceptor" maxlength="150"
                               value="<%= cliente == null ? "" : Vista.esc(cliente.getNombreCompleto()) %>">
                        <div class="form-text">
                            Cambielo si la factura va a nombre de otra persona
                            o de una empresa.
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="nitReceptor" class="form-label">NIT</label>
                        <input type="text" class="form-control" id="nitReceptor"
                               name="nitReceptor" maxlength="20"
                               value="<%= cliente == null ? "" : Vista.esc(cliente.getNit()) %>">
                    </div>

                    <div class="mb-3">
                        <label for="descuento" class="form-label">Descuento (Q)</label>
                        <input type="number" class="form-control" id="descuento"
                               name="descuento" step="0.01" min="0" value="0">
                        <div class="form-text">
                            Deje en cero si no se autoriza ningun descuento.
                        </div>
                    </div>

                    <div class="alert alert-light border mb-0">
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">Subtotal previsto</span>
                            <strong id="subtotalPrevisto">Q 0.00</strong>
                        </div>
                        <div class="d-flex justify-content-between">
                            <span class="text-muted">Total a facturar</span>
                            <strong id="totalPrevisto" class="fs-5"
                                    style="color:var(--hotel-azul);">Q 0.00</strong>
                        </div>
                    </div>
                </div>

                <div class="card-footer bg-white text-end">
                    <button type="submit" class="btn btn-oro"
                            onclick="return confirmar('Se emitira la factura. Una vez emitida solo se puede anular. Continuar?');">
                        <i class="bi bi-receipt me-1"></i> Emitir factura
                    </button>
                </div>
            </div>

            <%-- ------------------- Facturas ya emitidas --%>
<%
    if (!emitidas.isEmpty()) {
%>
            <div class="card">
                <div class="card-header">
                    <i class="bi bi-files me-2"></i> Facturas ya emitidas
                </div>
                <ul class="list-group list-group-flush">
<%
        for (Factura f : emitidas) {
%>
                    <li class="list-group-item d-flex justify-content-between
                               align-items-center">
                        <div>
                            <a href="<%= ctx %>/facturas?accion=ver&id=<%= f.getIdFactura() %>"
                               class="fw-semibold">
                                <%= Vista.esc(f.getNumeroCompleto()) %>
                            </a>
                            <div class="text-muted" style="font-size:.72rem;">
                                <%= Vista.esc(f.getTipo()) %> &middot;
                                <%= Vista.txt(f.getFechaEmision()) %>
                            </div>
                        </div>
                        <div class="text-end">
                            <div class="fw-semibold">
                                Q <%= Vista.moneda(f.getTotal()) %>
                            </div>
                            <span class="badge bg-<%= f.isVigente() ? "success" : "danger" %>"
                                  style="font-size:.6rem;">
                                <%= Vista.esc(f.getEstado()) %>
                            </span>
                        </div>
                    </li>
<%
        }
%>
                </ul>
            </div>
<%
    }
%>
        </div>
    </div>
</form>

<script>
    /*
     * Totales previstos por tipo de documento, calculados en el
     * servidor. El navegador solo los muestra segun lo que elija el
     * cajero; el calculo de verdad lo vuelve a hacer el servidor al
     * emitir, que es donde debe estar la autoridad sobre el dinero.
     */
    var TOTALES = {
        'COMPLETA':  <%= totalCompleta %>,
        'HOSPEDAJE': <%= totalHospedaje %>,
        'CONSUMOS':  <%= totalPendiente %>,
        'OCUPANTE':  0
    };

    /* Muestra u oculta el combo de huespedes y actualiza el total. */
    function cambiarTipo(tipo) {
        document.getElementById('cajaOcupante').style.display =
            (tipo === 'OCUPANTE') ? 'block' : 'none';
        recalcular();
    }

    function recalcular() {
        var marcado = document.querySelector('input[name="tipo"]:checked');
        if (!marcado) { return; }

        var subtotal  = TOTALES[marcado.value] || 0;
        var descuento = parseFloat(document.getElementById('descuento').value) || 0;
        var total     = subtotal - descuento;
        if (total < 0) { total = 0; }

        document.getElementById('subtotalPrevisto').textContent =
            'Q ' + subtotal.toFixed(2);
        document.getElementById('totalPrevisto').textContent =
            'Q ' + total.toFixed(2);
    }

    document.getElementById('descuento').addEventListener('input', recalcular);
    recalcular();
</script>

<jsp:include page="../comun/pie.jsp" />
