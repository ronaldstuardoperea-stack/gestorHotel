<%--
    clientes/formulario.jsp

    Alta y edicion de clientes. Igual que en habitaciones, el mismo
    archivo cubre los dos casos segun llegue o no el objeto 'cliente'.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.hotel.modelo.Cliente" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Clientes" />
    <jsp:param name="subtitulo" value="Registro de datos del huesped" />
    <jsp:param name="activo" value="clientes" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Cliente cliente = (Cliente) request.getAttribute("cliente");
    boolean edicion = (cliente != null);

    String tipoDoc = edicion ? cliente.getTipoDocumento() : "DPI";
%>

<div class="row justify-content-center">
    <div class="col-lg-9">
        <div class="card">
            <div class="card-header">
                <i class="bi bi-person-vcard me-2"></i>
<%
    if (edicion) {
%>
                Editar a <%= Vista.esc(cliente.getNombreCompleto()) %>
<%
    } else {
%>
                Nuevo cliente
<%
    }
%>
            </div>

            <form method="post" action="<%= ctx %>/clientes">
                <input type="hidden" name="idCliente"
                       value="<%= edicion ? cliente.getIdCliente() : 0 %>">

                <div class="card-body">

                    <h6 class="text-muted mb-3" style="font-size:.72rem; font-weight:700;
                        letter-spacing:.1em; text-transform:uppercase;">
                        Identificacion
                    </h6>

                    <div class="row g-3 mb-4">
                        <div class="col-md-6">
                            <label for="nombre" class="form-label obligatorio">Nombres</label>
                            <input type="text" class="form-control" id="nombre" name="nombre"
                                   value="<%= edicion ? Vista.esc(cliente.getNombre()) : "" %>"
                                   maxlength="80" required>
                        </div>

                        <div class="col-md-6">
                            <label for="apellido" class="form-label obligatorio">Apellidos</label>
                            <input type="text" class="form-control" id="apellido" name="apellido"
                                   value="<%= edicion ? Vista.esc(cliente.getApellido()) : "" %>"
                                   maxlength="80" required>
                        </div>

                        <div class="col-md-4">
                            <label for="tipoDocumento" class="form-label obligatorio">
                                Tipo de documento
                            </label>
                            <select class="form-select" id="tipoDocumento" name="tipoDocumento">
                                <option value="DPI"<%=
                                    Vista.selecto("DPI".equals(tipoDoc)) %>>DPI</option>
                                <option value="PASAPORTE"<%=
                                    Vista.selecto("PASAPORTE".equals(tipoDoc)) %>>
                                    Pasaporte</option>
                                <option value="NIT"<%=
                                    Vista.selecto("NIT".equals(tipoDoc)) %>>
                                    NIT (empresa)</option>
                                <option value="OTRO"<%=
                                    Vista.selecto("OTRO".equals(tipoDoc)) %>>
                                    Otro</option>
                            </select>
                        </div>

                        <div class="col-md-4">
                            <label for="documento" class="form-label obligatorio">
                                Numero de documento
                            </label>
                            <input type="text" class="form-control" id="documento" name="documento"
                                   value="<%= edicion ? Vista.esc(cliente.getDocumento()) : "" %>"
                                   maxlength="25" required>
                        </div>

                        <div class="col-md-4">
                            <label for="nit" class="form-label">NIT para facturar</label>
                            <input type="text" class="form-control" id="nit" name="nit"
                                   value="<%= edicion ? Vista.esc(cliente.getNit()) : "C/F" %>"
                                   maxlength="20" placeholder="C/F">
                            <div class="form-text">Escriba C/F si no tiene NIT.</div>
                        </div>
                    </div>

                    <h6 class="text-muted mb-3" style="font-size:.72rem; font-weight:700;
                        letter-spacing:.1em; text-transform:uppercase;">
                        Contacto
                    </h6>

                    <div class="row g-3 mb-4">
                        <div class="col-md-4">
                            <label for="telefono" class="form-label">Telefono</label>
                            <input type="text" class="form-control" id="telefono" name="telefono"
                                   value="<%= edicion ? Vista.esc(cliente.getTelefono()) : "" %>"
                                   maxlength="25" placeholder="5512-3344">
                        </div>

                        <div class="col-md-4">
                            <label for="correo" class="form-label">Correo electronico</label>
                            <input type="email" class="form-control" id="correo" name="correo"
                                   value="<%= edicion ? Vista.esc(cliente.getCorreo()) : "" %>"
                                   maxlength="120" placeholder="cliente@correo.com">
                        </div>

                        <div class="col-md-4">
                            <label for="pais" class="form-label">Pais</label>
                            <input type="text" class="form-control" id="pais" name="pais"
                                   value="<%= edicion ? Vista.esc(cliente.getPais()) : "Guatemala" %>"
                                   maxlength="60">
                        </div>

                        <div class="col-12">
                            <label for="direccion" class="form-label">Direccion</label>
                            <input type="text" class="form-control" id="direccion" name="direccion"
                                   value="<%= edicion ? Vista.esc(cliente.getDireccion()) : "" %>"
                                   maxlength="180">
                        </div>
                    </div>

                    <h6 class="text-muted mb-3" style="font-size:.72rem; font-weight:700;
                        letter-spacing:.1em; text-transform:uppercase;">
                        Observaciones
                    </h6>

                    <div class="mb-0">
                        <label for="notas" class="form-label">
                            Notas para el personal de recepcion
                        </label>
                        <textarea class="form-control" id="notas" name="notas" rows="2"
                                  maxlength="255"
                                  placeholder="Prefiere piso alto, alergico a las plumas, cliente frecuente..."><%=
                                  edicion ? Vista.esc(cliente.getNotas()) : "" %></textarea>
                    </div>
                </div>

                <div class="card-footer bg-white d-flex gap-2 justify-content-end">
                    <a href="<%= ctx %>/clientes" class="btn btn-outline-secondary">Cancelar</a>
                    <button type="submit" class="btn btn-hotel">
                        <i class="bi bi-save me-1"></i> Guardar cliente
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="../comun/pie.jsp" />
