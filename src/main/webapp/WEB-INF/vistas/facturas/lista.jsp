<%--
    facturas/lista.jsp

    Consulta de documentos emitidos, con filtros por texto, estado y
    fecha de emision.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.hotel.modelo.Factura" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Facturacion" />
    <jsp:param name="subtitulo" value="Documentos emitidos por el hotel" />
    <jsp:param name="activo" value="facturas" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    List<Factura> facturas = (List<Factura>) request.getAttribute("facturas");
    if (facturas == null) { facturas = new ArrayList<Factura>(); }

    BigDecimal facturadoMes = (BigDecimal) request.getAttribute("facturadoMes");

    Integer emitidas = (Integer) request.getAttribute("emitidasMes");
    int emitidasMes = (emitidas == null) ? 0 : emitidas.intValue();

    String busqueda = (String) request.getAttribute("busqueda");

    String filtroEstado = (String) request.getAttribute("filtroEstado");
    if (filtroEstado == null) { filtroEstado = ""; }

    LocalDate filtroDesde = (LocalDate) request.getAttribute("filtroDesde");
    LocalDate filtroHasta = (LocalDate) request.getAttribute("filtroHasta");
%>

<div class="row g-3 mb-3">
    <div class="col-md-6">
        <div class="indicador">
            <i class="bi bi-cash-stack icono"></i>
            <div class="etiqueta">Facturado este mes</div>
            <div class="valor">
                <span class="fs-6 text-muted">Q</span>
                <%= Vista.moneda(facturadoMes) %>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="indicador">
            <i class="bi bi-files icono"></i>
            <div class="etiqueta">Documentos emitidos este mes</div>
            <div class="valor"><%= emitidasMes %></div>
        </div>
    </div>
</div>

<div class="card mb-3">
    <div class="card-body py-3">
        <form method="get" action="<%= ctx %>/facturas" class="row g-2 align-items-end">

            <div class="col-md-4">
                <label class="form-label">Buscar</label>
                <input type="text" class="form-control form-control-sm" name="q"
                       value="<%= Vista.esc(busqueda) %>"
                       placeholder="Numero, receptor, NIT o codigo de reserva">
            </div>

            <div class="col-md-2">
                <label class="form-label">Estado</label>
                <select name="estado" class="form-select form-select-sm">
                    <option value="">Todos</option>
                    <option value="EMITIDA"<%=
                        Vista.selecto("EMITIDA".equals(filtroEstado)) %>>Emitida</option>
                    <option value="ANULADA"<%=
                        Vista.selecto("ANULADA".equals(filtroEstado)) %>>Anulada</option>
                </select>
            </div>

            <div class="col-md-2">
                <label class="form-label">Desde</label>
                <input type="date" class="form-control form-control-sm" name="desde"
                       value="<%= Vista.txt(filtroDesde) %>">
            </div>

            <div class="col-md-2">
                <label class="form-label">Hasta</label>
                <input type="date" class="form-control form-control-sm" name="hasta"
                       value="<%= Vista.txt(filtroHasta) %>">
            </div>

            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-sm btn-hotel">
                    <i class="bi bi-search"></i>
                </button>
                <a href="<%= ctx %>/facturas" class="btn btn-sm btn-outline-secondary">Limpiar</a>
            </div>
        </form>
    </div>
</div>

<div class="card">
<%
    if (facturas.isEmpty()) {
%>
    <div class="card-body text-center text-muted py-5">
        <i class="bi bi-receipt-cutoff fs-1 opacity-25 d-block mb-2"></i>
        No hay facturas que coincidan con los filtros aplicados.
        <div class="mt-2" style="font-size:.84rem;">
            Las facturas se emiten desde la ficha de cada reserva.
        </div>
    </div>
<%
    } else {
%>
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th>Documento</th>
                    <th>Receptor</th>
                    <th>Reserva</th>
                    <th>Tipo</th>
                    <th class="numero-tabla">Total</th>
                    <th>Estado</th>
                    <th class="text-end">Acciones</th>
                </tr>
            </thead>
            <tbody>
<%
        for (Factura f : facturas) {
%>
                <tr class="<%= f.isVigente() ? "" : "opacity-50" %>">
                    <td>
                        <a href="<%= ctx %>/facturas?accion=ver&id=<%= f.getIdFactura() %>"
                           class="badge-suave text-decoration-none">
                            <%= Vista.esc(f.getNumeroCompleto()) %>
                        </a>
                        <div class="text-muted" style="font-size:.7rem;">
                            <%= Vista.txt(f.getFechaEmision()) %>
                        </div>
                    </td>

                    <td>
                        <div style="font-size:.85rem;">
                            <%= Vista.esc(f.getNombreReceptor()) %>
                        </div>
                        <div class="text-muted" style="font-size:.7rem;">
                            NIT <%= Vista.esc(f.getNitReceptor()) %>
                        </div>
                    </td>

                    <td>
                        <a href="<%= ctx %>/reservas?accion=detalle&id=<%= f.getIdReserva() %>"
                           style="font-size:.82rem;">
                            <%= Vista.esc(f.getCodigoReserva()) %>
                        </a>
                        <div class="text-muted" style="font-size:.7rem;">
                            hab. <%= Vista.esc(f.getNumeroHabitacion()) %>
                        </div>
                    </td>

                    <td>
                        <span class="badge-suave"><%= Vista.esc(f.getTipo()) %></span>
<%
            if (f.getNombreOcupante() != null && !f.getNombreOcupante().isEmpty()) {
%>
                        <div class="text-muted" style="font-size:.68rem;">
                            <%= Vista.esc(f.getNombreOcupante()) %>
                        </div>
<%
            }
%>
                    </td>

                    <td class="numero-tabla fw-semibold">
                        <%= Vista.moneda(f.getTotal()) %>
<%
            if (f.getDescuento() != null
                    && f.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
%>
                        <div class="text-muted" style="font-size:.68rem;">
                            desc. <%= Vista.moneda(f.getDescuento()) %>
                        </div>
<%
            }
%>
                    </td>

                    <td>
                        <span class="badge bg-<%= f.isVigente() ? "success" : "danger" %>">
                            <%= Vista.esc(f.getEstado()) %>
                        </span>
                    </td>

                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/facturas?accion=ver&id=<%= f.getIdFactura() %>"
                               title="Ver">
                                <i class="bi bi-eye"></i>
                            </a>
                            <a class="btn btn-outline-secondary"
                               href="<%= ctx %>/facturas?accion=imprimir&id=<%= f.getIdFactura() %>"
                               target="_blank" title="Imprimir">
                                <i class="bi bi-printer"></i>
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

    <div class="card-footer bg-white text-muted" style="font-size:.8rem;">
        <%= facturas.size() %> documento(s) listado(s).
    </div>
<%
    }
%>
</div>

<jsp:include page="../comun/pie.jsp" />
