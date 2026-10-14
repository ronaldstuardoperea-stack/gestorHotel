<%--
    pie.jsp

    Cierre comun a todas las pantallas: termina el contenido, carga el
    JavaScript de Bootstrap y define el par de funciones propias que usa
    la interfaz.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

    </main><%-- cierra .cuerpo --%>
</div><%-- cierra .contenido --%>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

<script>
    /*
     * Abre y cierra el menu lateral en telefonos y tabletas.
     * En pantallas grandes el menu esta siempre visible y este boton
     * ni siquiera se muestra.
     */
    function alternarMenu() {
        document.getElementById('menuLateral').classList.toggle('abierto');
        document.getElementById('velo').classList.toggle('visible');
    }

    /*
     * Pide confirmacion antes de una accion que no se puede deshacer.
     * Se usa en los enlaces de eliminar, cancelar y anular.
     */
    function confirmar(mensaje) {
        return window.confirm(mensaje);
    }

    /*
     * Los avisos de exito se ocultan solos despues de seis segundos.
     * Los de error NO: el usuario tiene que leerlos y cerrarlos el
     * mismo, para que no se le pase un problema por no mirar a tiempo.
     */
    window.setTimeout(function () {
        document.querySelectorAll('.alert-success').forEach(function (aviso) {
            bootstrap.Alert.getOrCreateInstance(aviso).close();
        });
    }, 6000);
</script>

</body>
</html>
