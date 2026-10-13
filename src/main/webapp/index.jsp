<%--
    index.jsp

    Pagina de arranque de la aplicacion. No muestra nada: solo manda al
    tablero. Si no hay sesion iniciada, el filtro de autenticacion
    intercepta esa peticion y desvia al login, asi que aqui no hace
    falta repetir esa verificacion.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    response.sendRedirect(request.getContextPath() + "/inicio");
%>
