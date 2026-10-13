<%--
    login.jsp

    Pantalla de ingreso al sistema. Es la unica que no usa la cabecera
    comun, porque aqui todavia no hay sesion y por lo tanto no hay menu
    ni usuario que mostrar.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.hotel.util.Vista" %>

<%
    String ctx = request.getContextPath();

    String error          = (String) request.getAttribute("error");
    String usuarioEscrito = (String) request.getAttribute("usuarioEscrito");
    String mensajeExito   = (String) session.getAttribute("mensajeExito");
    String mensajeError   = (String) session.getAttribute("mensajeError");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ingreso al sistema | Hotel Buena Vista</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"
          rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;600;700&amp;family=Open+Sans:wght@400;600;700&amp;display=swap"
          rel="stylesheet">
    <link href="<%= ctx %>/recursos/css/estilo.css?v=5" rel="stylesheet">
</head>
<body>

<div class="pantalla-login">
    <div class="caja-login">

        <div class="encabezado">
            <h1 class="h4 mt-2 mb-1">Buena Vista</h1>
            <p class="mb-0" style="font-size:.72rem; letter-spacing:.2em;
               text-transform:uppercase; color:var(--hotel-oro-suave);">
                Hotel &amp; Suites
            </p>
        </div>

        <div class="cuerpo-login">

            <p class="text-muted mb-4" style="font-size:.85rem;">
                Sistema de gestion hotelera. Ingrese con las credenciales
                que le asigno el administrador.
            </p>

<%
    if (error != null && !error.isEmpty()) {
%>
            <div class="alert alert-danger d-flex align-items-start gap-2 py-2">
                <i class="bi bi-exclamation-triangle-fill mt-1"></i>
                <div style="font-size:.85rem;"><%= Vista.esc(error) %></div>
            </div>
<%
    }

    if (mensajeExito != null && !mensajeExito.isEmpty()) {
%>
            <div class="alert alert-success py-2" style="font-size:.85rem;">
                <%= Vista.esc(mensajeExito) %>
            </div>
<%
        session.removeAttribute("mensajeExito");
    }

    if (mensajeError != null && !mensajeError.isEmpty()) {
%>
            <div class="alert alert-warning py-2" style="font-size:.85rem;">
                <%= Vista.esc(mensajeError) %>
            </div>
<%
        session.removeAttribute("mensajeError");
    }
%>

            <form method="post" action="<%= ctx %>/login" autocomplete="off">

                <div class="mb-3">
                    <label for="usuario" class="form-label">Usuario</label>
                    <div class="input-group">
                        <span class="input-group-text bg-white"><i class="bi bi-person"></i></span>
                        <input type="text" class="form-control" id="usuario" name="usuario"
                               value="<%= Vista.esc(usuarioEscrito) %>"
                               placeholder="Su nombre de usuario" required autofocus>
                    </div>
                </div>

                <div class="mb-4">
                    <label for="clave" class="form-label">Contrasena</label>
                    <div class="input-group">
                        <span class="input-group-text bg-white"><i class="bi bi-lock"></i></span>
                        <input type="password" class="form-control" id="clave" name="clave"
                               placeholder="Su contrasena" required>
                        <button class="btn btn-outline-secondary" type="button"
                                onclick="alternarClave()" title="Mostrar u ocultar">
                            <i class="bi bi-eye" id="iconoOjo"></i>
                        </button>
                    </div>
                </div>

                <button type="submit" class="btn btn-oro w-100 py-2">
                    <i class="bi bi-box-arrow-in-right me-1"></i> Ingresar
                </button>
            </form>

            <hr class="my-4">

            <p class="text-center text-muted mb-0" style="font-size:.72rem;">
                Proyecto Final de Programacion II<br>
                Universidad Mariano Galvez de Guatemala
            </p>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    /* Muestra u oculta la contrasena escrita. */
    function alternarClave() {
        var campo = document.getElementById('clave');
        var icono = document.getElementById('iconoOjo');
        var oculta = campo.type === 'password';

        campo.type = oculta ? 'text' : 'password';
        icono.className = oculta ? 'bi bi-eye-slash' : 'bi bi-eye';
    }
</script>

</body>
</html>
