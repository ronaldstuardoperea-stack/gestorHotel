<%--
    cabecera.jsp

    Encabezado comun a todas las pantallas del sistema: abre el
    documento, carga Bootstrap, dibuja el menu lateral y la barra
    superior, y muestra los avisos de exito o de error.

    Se incluye desde cada vista pasandole tres parametros:
        titulo    -> nombre de la pantalla
        subtitulo -> linea de apoyo bajo el titulo
        activo    -> clave del modulo, para resaltarlo en el menu

    Los parametros llegan por <jsp:param> y se leen con
    request.getParameter(), igual que si vinieran de un formulario.

    Tenerlo en un solo archivo evita repetir el menu en las treinta y
    cinco pantallas: agregar una opcion nueva se hace aqui una sola vez.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.hotel.modelo.Usuario" %>
<%@ page import="com.hotel.util.Vista" %>

<%
    // Ruta base de la aplicacion. Se antepone a todos los enlaces para
    // que funcionen sin importar con que nombre se despliegue el WAR.
    String ctx = request.getContextPath();

    // Usuario autenticado. El filtro de autenticacion garantiza que
    // aqui nunca llega nadie sin sesion iniciada.
    Usuario u = (Usuario) session.getAttribute("usuario");

    // Parametros que envia la vista que incluye esta cabecera.
    String tituloPagina = request.getParameter("titulo");
    String subtitulo    = request.getParameter("subtitulo");
    String activo       = request.getParameter("activo");
    if (activo == null) { activo = ""; }

    // Avisos pendientes de mostrar.
    String mensajeExito  = (String) session.getAttribute("mensajeExito");
    String mensajeError  = (String) session.getAttribute("mensajeError");
    String errorPeticion = (String) request.getAttribute("error");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= Vista.esc(tituloPagina) %> | Hotel Buena Vista</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"
          rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;600;700&amp;family=Open+Sans:wght@400;600;700&amp;display=swap"
          rel="stylesheet">
    <link href="<%= ctx %>/recursos/css/estilo.css?v=5" rel="stylesheet">
</head>
<body>

<!-- ================================================== Menu lateral -->
<nav class="menu-lateral" id="menuLateral">

    <div class="marca">
        <div class="d-flex align-items-center gap-2">
            <div>
                <div class="nombre">Buena Vista</div>
                <div class="lema">Hotel &amp; Suites</div>
            </div>
        </div>
    </div>

    <div class="grupo">Operacion diaria</div>
    <a class="enlace <%= activo.equals("inicio") ? "activo" : "" %>" href="<%= ctx %>/inicio">
        <i class="bi bi-speedometer2"></i> Tablero
    </a>
    <a class="enlace <%= activo.equals("recepcion") ? "activo" : "" %>" href="<%= ctx %>/recepcion">
        <i class="bi bi-door-open"></i> Recepcion
    </a>
    <a class="enlace <%= activo.equals("disponibilidad") ? "activo" : "" %>" href="<%= ctx %>/disponibilidad">
        <i class="bi bi-calendar2-check"></i> Disponibilidad
    </a>
    <a class="enlace <%= activo.equals("reservas") ? "activo" : "" %>" href="<%= ctx %>/reservas">
        <i class="bi bi-journal-bookmark"></i> Reservas
    </a>

    <div class="grupo">Administracion</div>
    <a class="enlace <%= activo.equals("clientes") ? "activo" : "" %>" href="<%= ctx %>/clientes">
        <i class="bi bi-people"></i> Clientes
    </a>
    <a class="enlace <%= activo.equals("habitaciones") ? "activo" : "" %>" href="<%= ctx %>/habitaciones">
        <i class="bi bi-house-door"></i> Habitaciones
    </a>
    <a class="enlace <%= activo.equals("bloqueos") ? "activo" : "" %>" href="<%= ctx %>/bloqueos">
        <i class="bi bi-lock"></i> Bloqueos
    </a>

    <div class="grupo">Caja</div>
    <a class="enlace <%= activo.equals("facturas") ? "activo" : "" %>" href="<%= ctx %>/facturas">
        <i class="bi bi-receipt"></i> Facturacion
    </a>
    <a class="enlace <%= activo.equals("pagos") ? "activo" : "" %>" href="<%= ctx %>/pagos">
        <i class="bi bi-cash-coin"></i> Caja y pagos
    </a>
    <a class="enlace <%= activo.equals("reportes") ? "activo" : "" %>" href="<%= ctx %>/reportes">
        <i class="bi bi-graph-up-arrow"></i> Reportes
    </a>

<%
    // Los catalogos que fijan precios solo los ve el administrador.
    if (u != null && u.isAdministrador()) {
%>
    <div class="grupo">Configuracion</div>
    <a class="enlace <%= activo.equals("tipos") ? "activo" : "" %>" href="<%= ctx %>/tipos">
        <i class="bi bi-tags"></i> Tipos y tarifas
    </a>
    <a class="enlace <%= activo.equals("temporadas") ? "activo" : "" %>" href="<%= ctx %>/temporadas">
        <i class="bi bi-calendar-range"></i> Temporadas
    </a>
    <a class="enlace <%= activo.equals("usuarios") ? "activo" : "" %>" href="<%= ctx %>/usuarios">
        <i class="bi bi-person-badge"></i> Usuarios
    </a>
<%
    }
%>

    <div class="pie-menu">
        Proyecto Final &mdash; Programacion II<br>
        Universidad Mariano Galvez
    </div>
</nav>

<div class="velo" id="velo" onclick="alternarMenu()"></div>

<!-- ================================================== Contenido -->
<div class="contenido">

    <header class="barra-superior">
        <button class="btn btn-sm btn-outline-secondary boton-menu"
                type="button" onclick="alternarMenu()" aria-label="Abrir menu">
            <i class="bi bi-list"></i>
        </button>

        <div class="flex-grow-1">
            <h1 class="titulo"><%= Vista.esc(tituloPagina) %></h1>
<%
    if (subtitulo != null && !subtitulo.isEmpty()) {
%>
            <p class="subtitulo"><%= Vista.esc(subtitulo) %></p>
<%
    }
%>
        </div>

        <div class="d-none d-md-block text-end me-2">
            <div class="fw-semibold" style="font-size:.84rem;">
                <%= Vista.esc(u == null ? "" : u.getNombreCompleto()) %>
            </div>
            <div class="text-muted" style="font-size:.72rem;">
                <%= Vista.esc(u == null ? "" : u.getRol()) %>
            </div>
        </div>

        <div class="dropdown">
            <a class="avatar oro dropdown-toggle-no-arrow" href="#" role="button"
               data-bs-toggle="dropdown" aria-expanded="false" title="Mi cuenta">
                <%= Vista.esc(u == null ? "?" : u.getIniciales()) %>
            </a>
            <ul class="dropdown-menu dropdown-menu-end">
                <li class="dropdown-header">
                    <%= Vista.esc(u == null ? "" : u.getUsuario()) %>
                </li>
                <li><hr class="dropdown-divider"></li>
                <li>
                    <a class="dropdown-item text-danger" href="<%= ctx %>/logout">
                        <i class="bi bi-box-arrow-right me-1"></i> Cerrar sesion
                    </a>
                </li>
            </ul>
        </div>
    </header>

    <main class="cuerpo">

<%
    /*
        Avisos del sistema.

        Los mensajes guardados en la sesion sobreviven a la redireccion
        que se hace despues de guardar un registro. Se muestran una sola
        vez y se borran de inmediato con removeAttribute, para que no
        reaparezcan al navegar a otra pantalla.
    */
    if (mensajeExito != null && !mensajeExito.isEmpty()) {
%>
        <div class="alert alert-success alert-dismissible fade show d-flex align-items-start gap-2">
            <i class="bi bi-check-circle-fill mt-1"></i>
            <div class="flex-grow-1"><%= Vista.esc(mensajeExito) %></div>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
<%
        session.removeAttribute("mensajeExito");
    }

    if (mensajeError != null && !mensajeError.isEmpty()) {
%>
        <div class="alert alert-danger alert-dismissible fade show d-flex align-items-start gap-2">
            <i class="bi bi-exclamation-triangle-fill mt-1"></i>
            <div class="flex-grow-1"><%= Vista.esc(mensajeError) %></div>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
<%
        session.removeAttribute("mensajeError");
    }

    // Errores puntuales de la peticion actual, sin redireccion.
    if (errorPeticion != null && !errorPeticion.isEmpty()) {
%>
        <div class="alert alert-danger d-flex align-items-start gap-2">
            <i class="bi bi-exclamation-triangle-fill mt-1"></i>
            <div><%= Vista.esc(errorPeticion) %></div>
        </div>
<%
    }
%>
