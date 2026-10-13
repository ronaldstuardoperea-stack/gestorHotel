<%--
    error.jsp

    Pantalla de error del sistema. Esta declarada en web.xml como
    destino de los errores 404, 500 y de cualquier excepcion que llegue
    hasta el contenedor.

    Muestra un mensaje entendible en lugar del rastro de la excepcion.
    El detalle tecnico completo queda en la bitacora del servidor, donde
    le sirve al administrador y no al huesped que esta esperando en el
    mostrador.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ page import="com.hotel.util.Vista" %>

<%
    String ctx = request.getContextPath();

    // Tomcat deja el codigo del error HTTP en este atributo reservado.
    Integer codigo = (Integer) request.getAttribute("javax.servlet.error.status_code");
    boolean esNoEncontrado = (codigo != null && codigo == 404);

    String mensajePropio = (String) request.getAttribute("error");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Error | Hotel Buena Vista</title>
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
            <div class="simbolo"><i class="bi bi-exclamation-octagon"></i></div>
            <h1 class="h4 mt-2 mb-0">
                <%= esNoEncontrado ? "Pagina no encontrada" : "Ocurrio un problema" %>
            </h1>
        </div>

        <div class="cuerpo-login text-center">

<%
    if (mensajePropio != null && !mensajePropio.isEmpty()) {
%>
            <p class="mb-4"><%= Vista.esc(mensajePropio) %></p>
<%
    } else if (esNoEncontrado) {
%>
            <p class="mb-4">
                La direccion que intento abrir no existe en el sistema.
                Es posible que el enlace este equivocado o que la
                pantalla haya cambiado de lugar.
            </p>
<%
    } else {
%>
            <p class="mb-4">
                El sistema no pudo completar la operacion. Vuelva a
                intentarlo; si el problema continua, avise al
                administrador para que revise la bitacora del servidor.
            </p>
<%
    }
%>

            <div class="d-grid gap-2">
                <a href="<%= ctx %>/inicio" class="btn btn-hotel">
                    <i class="bi bi-house-door me-1"></i> Volver al tablero
                </a>
                <a href="javascript:history.back()" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Regresar
                </a>
            </div>
        </div>
    </div>
</div>

</body>
</html>
