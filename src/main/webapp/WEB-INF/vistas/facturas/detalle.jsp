<%--
    facturas/detalle.jsp

    Vista de una factura emitida, con su detalle y las acciones de
    imprimir y anular.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.modelo.Factura" %>
<%@ page import="com.hotel.modelo.DetalleFactura" %>
<%@ page import="com.hotel.modelo.Cliente" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Factura" />
    <jsp:param name="subtitulo" value="Detalle del documento emitido" />
    <jsp:param name="activo" value="facturas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario u = (Usuario) session.getAttribute("usuario");
    boolean esAdmin = (u != null && u.isAdministrador());

    Factura factura = (Factura) request.getAttribute("factura");
    Cliente cliente = (Cliente) request.getAttribute("cliente");

    List<DetalleFactura> detalles = factura.getDetalles();
    if (detalles == null) { detalles = new ArrayList<DetalleFactura>(); }

    boolean conDescuento = (factura.getDescuento() != null
                            && factura.getDescuento().compareTo(BigDecimal.ZERO) > 0);

    // Solo el administrador anula, y solo un documento que siga vigente.
    boolean puedeAnular = (esAdmin && factura.isVigente());
%>

<div class="mb-3 d-flex flex-wrap gap-2">
    <a href="<%= ctx %>/facturas" class="btn btn-sm btn-outline-secondary">
        <i class="bi bi-arrow-left me-1"></i> Volver a facturacion
    </a>
    <a href="<%= ctx %>/reservas?accion=detalle&id=<%= factura.getIdReserva() %>"
       class="btn btn-sm btn-outline-secondary">
        <i class="bi bi-journal-bookmark me-1"></i> Ver la reserva
    </a>
    <a href="<%= ctx %>/facturas?accion=imprimir&id=<%= factura.getIdFactura() %>"
       target="_blank" class="btn btn-sm btn-hotel">
        <i class="bi bi-printer me-1"></i> Imprimir
    </a>

<%
    if (puedeAnular) {
%>
    <button type="button" class="btn btn-sm btn-outline-danger ms-auto"
            data-bs-toggle="modal" data-bs-target="#modalAnular">
        <i class="bi bi-x-octagon me-1"></i> Anular factura
    </button>
<%
    }
%>
</div>

<%
    if (!factura.isVigente()) {
%>
<div class="alert alert-danger d-flex align-items-start gap-2">
    <i class="bi bi-x-octagon-fill mt-1"></i>
    <div>
        <strong>Esta factura esta anulada.</strong>
        Los consumos que incluia volvieron a quedar pendientes de
        facturar. El documento se conserva para no dejar huecos en la
        numeracion.
    </div>
</div>
<%
    }
%>

<div class="row justify-content-center">
    <div class="col-lg-9">
        <div class="hoja-factura">

            <%-- ------------------------------- Encabezado --%>
            <div class="d-flex justify-content-between align-items-start mb-4">
                <div>
                    <div class="d-flex align-items-center gap-2">
                        <div>
                            <div class="fw-bold fs-5" style="color:var(--hotel-noche);">
                                Hotel Buena Vista
                            </div>
                            <div class="text-muted" style="font-size:.72rem;
                                 letter-spacing:.18em; text-transform:uppercase;">
                                Hotel &amp; Suites
                            </div>
                        </div>
                    </div>
                    <div class="text-muted mt-2" style="font-size:.78rem;">
                        Antigua Guatemala, Sacatepequez<br>
                        NIT 1234567-8 &middot; Tel. 7832-0000
                    </div>
                </div>

                <div class="text-end">
                    <div class="text-muted" style="font-size:.72rem;
                         letter-spacing:.1em; text-transform:uppercase;">
                        Factura
                    </div>
                    <div class="fs-4 fw-bold" style="color:var(--hotel-azul);">
                        <%= Vista.esc(factura.getNumeroCompleto()) %>
                    </div>
                    <div class="text-muted" style="font-size:.78rem;">
                        <%= Vista.txt(factura.getFechaEmision()) %>
                    </div>
                    <span class="badge bg-<%= factura.isVigente() ? "success" : "danger" %> mt-1">
                        <%= Vista.esc(factura.getEstado()) %>
                    </span>
                </div>
            </div>

            <hr>

            <%-- --------------------------------- Receptor --%>
            <div class="row mb-4">
                <div class="col-sm-7">
                    <div class="text-muted" style="font-size:.7rem; font-weight:700;
                         letter-spacing:.1em; text-transform:uppercase;">
                        Facturar a
                    </div>
                    <div class="fw-semibold">
                        <%= Vista.esc(factura.getNombreReceptor()) %>
                    </div>
                    <div class="text-muted" style="font-size:.82rem;">
                        NIT <%= Vista.esc(factura.getNitReceptor()) %>
                    </div>
<%
    if (cliente != null && cliente.getDireccion() != null
            && !cliente.getDireccion().isEmpty()) {
%>
                    <div class="text-muted" style="font-size:.78rem;">
                        <%= Vista.esc(cliente.getDireccion()) %>
                    </div>
<%
    }
%>
                </div>

                <div class="col-sm-5 text-sm-end">
                    <div class="text-muted" style="font-size:.7rem; font-weight:700;
                         letter-spacing:.1em; text-transform:uppercase;">
                        Reserva
                    </div>
                    <div class="fw-semibold"><%= Vista.esc(factura.getCodigoReserva()) %></div>
                    <div class="text-muted" style="font-size:.82rem;">
                        Habitacion <%= Vista.esc(factura.getNumeroHabitacion()) %>
                    </div>
                    <div class="text-muted" style="font-size:.78rem;">
                        <%= Vista.esc(factura.getDescripcionTipo()) %>
                    </div>
<%
    if (factura.getNombreOcupante() != null && !factura.getNombreOcupante().isEmpty()) {
%>
                    <div class="text-muted" style="font-size:.78rem;">
                        Huesped: <%= Vista.esc(factura.getNombreOcupante()) %>
                    </div>
<%
    }
%>
                </div>
            </div>

            <%-- ---------------------------------- Detalle --%>
            <table class="table table-sm">
                <thead>
                    <tr>
                        <th>Descripcion</th>
                        <th class="numero-tabla">Cantidad</th>
                        <th class="numero-tabla">Precio unitario</th>
                        <th class="numero-tabla">Importe</th>
                    </tr>
                </thead>
                <tbody>
<%
    for (DetalleFactura d : detalles) {
%>
                    <tr>
                        <td style="font-size:.85rem;">
                            <%= Vista.esc(d.getDescripcion()) %>
                        </td>
                        <td class="numero-tabla">
                            <%= Vista.cantidad(d.getCantidad()) %>
                        </td>
                        <td class="numero-tabla">
                            <%= Vista.moneda(d.getPrecioUnitario()) %>
                        </td>
                        <td class="numero-tabla fw-semibold">
                            <%= Vista.moneda(d.getImporte()) %>
                        </td>
                    </tr>
<%
    }
%>
                </tbody>
            </table>

            <%-- ----------------------------------- Totales --%>
            <div class="row justify-content-end">
                <div class="col-sm-6">
                    <table class="table table-sm">
                        <tr>
                            <td>Subtotal</td>
                            <td class="numero-tabla">
                                Q <%= Vista.moneda(factura.getSubtotal()) %>
                            </td>
                        </tr>
<%
    if (conDescuento) {
%>
                        <tr>
                            <td class="text-danger">Descuento</td>
                            <td class="numero-tabla text-danger">
                                - Q <%= Vista.moneda(factura.getDescuento()) %>
                            </td>
                        </tr>
<%
    }
%>
                        <tr class="border-top">
                            <td class="fw-bold fs-5">Total</td>
                            <td class="numero-tabla fw-bold fs-5"
                                style="color:var(--hotel-azul);">
                                Q <%= Vista.moneda(factura.getTotal()) %>
                            </td>
                        </tr>
                    </table>
                </div>
            </div>

<%
    if (factura.getObservaciones() != null && !factura.getObservaciones().isEmpty()) {
%>
            <div class="alert alert-light border mt-3 mb-0" style="font-size:.8rem;">
                <%= Vista.esc(factura.getObservaciones()) %>
            </div>
<%
    }
%>

            <hr>

            <div class="text-center text-muted" style="font-size:.74rem;">
                Emitida por <%= Vista.esc(factura.getNombreUsuario()) %>
                &middot; Documento generado por el sistema de gestion hotelera
            </div>
        </div>
    </div>
</div>

<%-- ==================================== Modal de anulacion --%>
<%
    if (puedeAnular) {
%>
<div class="modal fade" id="modalAnular" tabindex="-1">
    <div class="modal-dialog">
        <form method="post" action="<%= ctx %>/facturas" class="modal-content">
            <input type="hidden" name="accion" value="anular">
            <input type="hidden" name="idFactura" value="<%= factura.getIdFactura() %>">

            <div class="modal-header">
                <h5 class="modal-title">
                    Anular la factura <%= Vista.esc(factura.getNumeroCompleto()) %>
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>

            <div class="modal-body">
                <p style="font-size:.88rem;">
                    La factura no se borra: queda marcada como anulada para que
                    la numeracion no deje huecos, y los consumos que incluia
                    vuelven a quedar pendientes de facturar.
                </p>

                <label for="motivo" class="form-label obligatorio">
                    Motivo de la anulacion
                </label>
                <input type="text" class="form-control" id="motivo" name="motivo"
                       maxlength="150" required
                       placeholder="NIT equivocado, monto incorrecto, solicitud del cliente...">
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary"
                        data-bs-dismiss="modal">Cancelar</button>
                <button type="submit" class="btn btn-danger">
                    <i class="bi bi-x-octagon me-1"></i> Anular factura
                </button>
            </div>
        </form>
    </div>
</div>
<%
    }
%>

<jsp:include page="../comun/pie.jsp" />
