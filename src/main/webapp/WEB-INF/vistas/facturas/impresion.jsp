<%--
    facturas/impresion.jsp

    Version de la factura lista para imprimir o guardar como PDF.
    No lleva menu ni barra superior: solo el documento, para que la hoja
    salga limpia. Al abrirse llama sola al dialogo de impresion.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.hotel.modelo.Factura" %>
<%@ page import="com.hotel.modelo.DetalleFactura" %>
<%@ page import="com.hotel.modelo.Cliente" %>
<%@ page import="com.hotel.modelo.Reserva" %>
<%@ page import="com.hotel.util.Vista" %>

<%
    String ctx = request.getContextPath();

    Factura factura = (Factura) request.getAttribute("factura");
    Cliente cliente = (Cliente) request.getAttribute("cliente");
    Reserva reserva = (Reserva) request.getAttribute("reserva");

    List<DetalleFactura> detalles = factura.getDetalles();
    if (detalles == null) { detalles = new ArrayList<DetalleFactura>(); }

    boolean conDescuento = (factura.getDescuento() != null
                            && factura.getDescuento().compareTo(BigDecimal.ZERO) > 0);
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Factura <%= Vista.esc(factura.getNumeroCompleto()) %> | Hotel Buena Vista</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"
          rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;600;700&amp;family=Open+Sans:wght@400;600;700&amp;display=swap"
          rel="stylesheet">
    <link href="<%= ctx %>/recursos/css/estilo.css?v=5" rel="stylesheet">

    <style>
        body { background: #fff; padding: 2rem 1rem; }
    </style>
</head>
<body>

<div class="sin-imprimir text-center mb-3">
    <button onclick="window.print()" class="btn btn-hotel btn-sm">
        <i class="bi bi-printer me-1"></i> Imprimir
    </button>
    <a href="<%= ctx %>/facturas?accion=ver&id=<%= factura.getIdFactura() %>"
       class="btn btn-outline-secondary btn-sm">
        Volver al sistema
    </a>
</div>

<div class="hoja-factura">

<%
    if (!factura.isVigente()) {
%>
    <div class="text-center mb-3">
        <span class="badge bg-danger fs-6">DOCUMENTO ANULADO</span>
    </div>
<%
    }
%>

    <%-- --------------------------------------- Encabezado --%>
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
        </div>
    </div>

    <hr>

    <%-- ----------------------------------------- Receptor --%>
    <div class="row mb-4">
        <div class="col-7">
            <div class="text-muted" style="font-size:.7rem; font-weight:700;
                 letter-spacing:.1em; text-transform:uppercase;">
                Facturar a
            </div>
            <div class="fw-semibold"><%= Vista.esc(factura.getNombreReceptor()) %></div>
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

        <div class="col-5 text-end">
            <div class="text-muted" style="font-size:.7rem; font-weight:700;
                 letter-spacing:.1em; text-transform:uppercase;">
                Reserva
            </div>
            <div class="fw-semibold"><%= Vista.esc(factura.getCodigoReserva()) %></div>
            <div class="text-muted" style="font-size:.82rem;">
                Habitacion <%= Vista.esc(factura.getNumeroHabitacion()) %>
            </div>
<%
    if (reserva != null) {
%>
            <div class="text-muted" style="font-size:.78rem;">
                <%= Vista.txt(reserva.getFechaEntrada()) %>
                al <%= Vista.txt(reserva.getFechaSalida()) %>
            </div>
<%
    }
%>
        </div>
    </div>

    <%-- ------------------------------------------ Detalle --%>
    <table class="table table-sm">
        <thead>
            <tr>
                <th>Descripcion</th>
                <th class="numero-tabla">Cant.</th>
                <th class="numero-tabla">P. unitario</th>
                <th class="numero-tabla">Importe</th>
            </tr>
        </thead>
        <tbody>
<%
    for (DetalleFactura d : detalles) {
%>
            <tr>
                <td style="font-size:.85rem;"><%= Vista.esc(d.getDescripcion()) %></td>
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

    <%-- ------------------------------------------ Totales --%>
    <div class="row justify-content-end">
        <div class="col-6">
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
                    <td>Descuento</td>
                    <td class="numero-tabla">
                        - Q <%= Vista.moneda(factura.getDescuento()) %>
                    </td>
                </tr>
<%
    }
%>
                <tr class="border-top">
                    <td class="fw-bold fs-5">Total</td>
                    <td class="numero-tabla fw-bold fs-5" style="color:var(--hotel-azul);">
                        Q <%= Vista.moneda(factura.getTotal()) %>
                    </td>
                </tr>
            </table>
        </div>
    </div>

    <div class="mt-5 pt-4">
        <div class="row">
            <div class="col-6 text-center">
                <div style="border-top:1px solid #333; padding-top:.4rem;
                     font-size:.78rem;">
                    Firma y sello del hotel
                </div>
            </div>
            <div class="col-6 text-center">
                <div style="border-top:1px solid #333; padding-top:.4rem;
                     font-size:.78rem;">
                    Recibi conforme
                </div>
            </div>
        </div>
    </div>

    <div class="text-center text-muted mt-4" style="font-size:.72rem;">
        Gracias por su preferencia &middot; Emitida por
        <%= Vista.esc(factura.getNombreUsuario()) %>
    </div>
</div>

<script>
    /* Abre el dialogo de impresion en cuanto carga la pagina, que es lo
       que el cajero espera al pulsar el boton de imprimir. */
    window.addEventListener('load', function () {
        window.print();
    });
</script>

</body>
</html>
