<%--
    usuarios/formulario.jsp

    Alta y edicion de usuarios del sistema. Al editar, los datos y la
    contrasena van en formularios separados: cambiar el rol de alguien no
    deberia obligar a reescribir su clave.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.util.Vista" %>

<jsp:include page="../comun/cabecera.jsp">
    <jsp:param name="titulo" value="Usuarios" />
    <jsp:param name="subtitulo" value="Datos de acceso al sistema" />
    <jsp:param name="activo" value="usuarios" />
</jsp:include>

<%
    String ctx = request.getContextPath();

    Usuario usuarioEditado = (Usuario) request.getAttribute("usuarioEditado");
    boolean edicion = (usuarioEditado != null);

    String rolActual = edicion ? usuarioEditado.getRol() : "RECEPCION";
%>

<div class="row justify-content-center g-3">

    <%-- ------------------------------------- Datos del usuario --%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-person-badge me-2"></i>
<%
    if (edicion) {
%>
                Editar <%= Vista.esc(usuarioEditado.getUsuario()) %>
<%
    } else {
%>
                Nuevo usuario
<%
    }
%>
            </div>

            <form method="post" action="<%= ctx %>/usuarios">
                <input type="hidden" name="accion" value="guardar">
                <input type="hidden" name="idUsuario"
                       value="<%= edicion ? usuarioEditado.getIdUsuario() : 0 %>">

                <div class="card-body">

                    <div class="mb-3">
                        <label for="nombreCompleto" class="form-label obligatorio">
                            Nombre completo
                        </label>
                        <input type="text" class="form-control" id="nombreCompleto"
                               name="nombreCompleto" maxlength="120" required
                               value="<%= edicion ? Vista.esc(usuarioEditado.getNombreCompleto()) : "" %>">
                    </div>

                    <div class="mb-3">
                        <label for="usuario" class="form-label obligatorio">
                            Nombre de ingreso
                        </label>
                        <input type="text" class="form-control" id="usuario" name="usuario"
                               maxlength="40" required autocomplete="off"
                               value="<%= edicion ? Vista.esc(usuarioEditado.getUsuario()) : "" %>">
                        <div class="form-text">Con este nombre iniciara sesion.</div>
                    </div>

                    <div class="mb-3">
                        <label for="rol" class="form-label obligatorio">Rol</label>
                        <select class="form-select" id="rol" name="rol">
                            <option value="RECEPCION"<%=
                                Vista.selecto("RECEPCION".equals(rolActual)) %>>
                                Recepcion &mdash; opera reservas y cobros
                            </option>
                            <option value="ADMINISTRADOR"<%=
                                Vista.selecto("ADMINISTRADOR".equals(rolActual)) %>>
                                Administrador &mdash; acceso total
                            </option>
                            <option value="AUDITOR"<%=
                                Vista.selecto("AUDITOR".equals(rolActual)) %>>
                                Auditor &mdash; solo consulta
                            </option>
                        </select>
                    </div>

                    <%-- Al crear se pide la clave aqui mismo. Al editar,
                         la clave se cambia en el otro formulario. --%>
<%
    if (!edicion) {
%>
                    <div class="row g-2 mb-3">
                        <div class="col-md-6">
                            <label for="clave" class="form-label obligatorio">
                                Contrasena
                            </label>
                            <input type="password" class="form-control" id="clave"
                                   name="clave" minlength="6" required autocomplete="new-password">
                        </div>
                        <div class="col-md-6">
                            <label for="confirmacion" class="form-label obligatorio">
                                Confirmar
                            </label>
                            <input type="password" class="form-control" id="confirmacion"
                                   name="confirmacion" minlength="6" required autocomplete="new-password">
                        </div>
                        <div class="col-12">
                            <div class="form-text">Minimo 6 caracteres.</div>
                        </div>
                    </div>
<%
    }
%>

                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="activo"
                               name="activo" value="1"
                               <%= Vista.marcado(!edicion || usuarioEditado.isActivo()) %>>
                        <label class="form-check-label" for="activo">
                            El usuario puede iniciar sesion
                        </label>
                    </div>
                </div>

                <div class="card-footer bg-white d-flex gap-2 justify-content-end">
                    <a href="<%= ctx %>/usuarios" class="btn btn-outline-secondary">Cancelar</a>
                    <button type="submit" class="btn btn-hotel">
                        <i class="bi bi-save me-1"></i> Guardar
                    </button>
                </div>
            </form>
        </div>
    </div>

    <%-- ----------------------------------- Cambio de contrasena --%>
<%
    if (edicion) {
%>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <i class="bi bi-key me-2"></i> Cambiar contrasena
            </div>

            <form method="post" action="<%= ctx %>/usuarios">
                <input type="hidden" name="accion" value="clave">
                <input type="hidden" name="idUsuario"
                       value="<%= usuarioEditado.getIdUsuario() %>">

                <div class="card-body">
                    <p class="text-muted" style="font-size:.84rem;">
                        La contrasena actual no se puede consultar: en la base
                        de datos solo se guarda su resumen cifrado. Lo que si se
                        puede hacer es asignarle una nueva.
                    </p>

                    <div class="mb-3">
                        <label for="claveNueva" class="form-label obligatorio">
                            Contrasena nueva
                        </label>
                        <input type="password" class="form-control" id="claveNueva"
                               name="clave" minlength="6" required autocomplete="new-password">
                    </div>

                    <div class="mb-0">
                        <label for="confirmacionNueva" class="form-label obligatorio">
                            Confirmar contrasena
                        </label>
                        <input type="password" class="form-control" id="confirmacionNueva"
                               name="confirmacion" minlength="6" required autocomplete="new-password">
                        <div class="form-text">Minimo 6 caracteres.</div>
                    </div>
                </div>

                <div class="card-footer bg-white text-end">
                    <button type="submit" class="btn btn-outline-dark"
                            onclick="return confirmar('Se cambiara la contrasena de <%= Vista.esc(usuarioEditado.getUsuario()) %>. Continuar?');">
                        <i class="bi bi-key me-1"></i> Cambiar contrasena
                    </button>
                </div>
            </form>
        </div>
    </div>
<%
    }
%>
</div>

<jsp:include page="../comun/pie.jsp" />
