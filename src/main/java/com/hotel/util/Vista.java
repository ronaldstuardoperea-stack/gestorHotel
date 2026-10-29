package com.hotel.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Utilidades de apoyo para las paginas JSP.
 *
 * Las vistas de este proyecto estan escritas con scriptlets, es decir,
 * con codigo Java incrustado entre las etiquetas &lt;% %&gt;. Esta clase
 * existe para que ese codigo quede corto y legible, y sobre todo para
 * cubrir dos cosas que las etiquetas JSTL hacian solas y los scriptlets
 * no:
 *
 *  1. ESCAPAR EL HTML. La expresion &lt;%= texto %&gt; escribe el valor
 *     tal cual. Si un cliente se llamara "&lt;script&gt;alert(1)&lt;/script&gt;",
 *     el navegador ejecutaria ese codigo: es la vulnerabilidad conocida
 *     como XSS. El metodo esc() convierte esos caracteres en entidades
 *     para que se muestren como texto y no se ejecuten.
 *
 *  2. FORMATEAR LOS MONTOS. Sin JSTL hay que dar formato a mano, y
 *     hacerlo en cada una de las paginas seria repetir lo mismo decenas
 *     de veces.
 *
 * Todos los metodos toleran null, porque en una vista es normal que un
 * dato opcional venga vacio y no debe reventar la pagina.
 *
 * @author Ronald Perea
 */
public final class Vista {

    /**
     * Idioma con el que se formatean los montos. Se fija a proposito en
     * lugar de dejarlo al del servidor: en un locale de Espana el mismo
     * monto saldria como "1.050,00" y en Guatemala los quetzales se
     * escriben "1,050.00". Locale.US da justamente ese formato.
     *
     * Se usa con String.format, que crea su propio formateador en cada
     * llamada; un DecimalFormat compartido no serviria, porque no es
     * seguro entre hilos y el servidor atiende varias peticiones a la vez.
     */
    private static final Locale MONEDA_GT = Locale.US;

    /** Clase de utilidad: no se instancia. */
    private Vista() {
    }

    /**
     * Convierte un valor en texto seguro para incrustar en el HTML.
     *
     * Reemplaza los cinco caracteres que el navegador interpretaria como
     * marcado. Es lo que hacia la etiqueta &lt;c:out&gt; y lo que NO hace
     * &lt;%= %&gt; por su cuenta.
     *
     * @param valor dato a mostrar; puede ser null
     * @return el texto escapado, o cadena vacia si el valor era null
     */
    public static String esc(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        StringBuilder salida = new StringBuilder(texto.length() + 16);

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '<':  salida.append("&lt;");   break;
                case '>':  salida.append("&gt;");   break;
                case '&':  salida.append("&amp;");  break;
                case '"':  salida.append("&quot;"); break;
                case '\'': salida.append("&#39;");  break;
                default:   salida.append(c);        break;
            }
        }
        return salida.toString();
    }

    /**
     * Muestra un dato que NO proviene del usuario (fechas, numeros,
     * estados del sistema) sin escapar, pero tolerando null.
     *
     * Se usa para valores que el propio sistema genera y que por lo
     * tanto no pueden traer marcado malicioso.
     *
     * @param valor dato a mostrar; puede ser null
     * @return su representacion en texto, o cadena vacia si era null
     */
    public static String txt(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    /**
     * Da formato de dinero a un monto: separador de miles y siempre dos
     * decimales. Por ejemplo, 1234.5 se muestra como "1,234.50".
     *
     * Equivale a la etiqueta
     * &lt;fmt:formatNumber minFractionDigits="2" maxFractionDigits="2"/&gt;
     *
     * @param monto cantidad a formatear; puede ser null
     * @return el monto formateado, o "0.00" si venia null
     */
    public static String moneda(BigDecimal monto) {
        if (monto == null) {
            return "0.00";
        }
        return String.format(MONEDA_GT, "%,.2f", monto.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Da formato a una cantidad que puede no ser dinero: muestra hasta
     * dos decimales pero sin forzarlos.
     *
     * Se usa por ejemplo en la columna de cantidad de los consumos,
     * donde "3" se ve mejor que "3.00".
     *
     * @param cantidad valor a formatear; puede ser null
     * @return la cantidad formateada, o "0" si venia null
     */
    public static String cantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            return "0";
        }
        BigDecimal limpio = cantidad.stripTrailingZeros();
        if (limpio.scale() < 0) {
            limpio = limpio.setScale(0);
        }
        return limpio.toPlainString();
    }

    /**
     * Devuelve el atributo "selected" cuando la condicion se cumple.
     * Sirve para dejar marcada la opcion elegida en un combo sin llenar
     * el HTML de bloques &lt;% if %&gt;.
     *
     * @param condicion si la opcion debe quedar seleccionada
     * @return " selected" o cadena vacia
     */
    public static String selecto(boolean condicion) {
        return condicion ? " selected" : "";
    }

    /**
     * Devuelve el atributo "checked" cuando la condicion se cumple.
     *
     * @param condicion si la casilla debe quedar marcada
     * @return " checked" o cadena vacia
     */
    public static String marcado(boolean condicion) {
        return condicion ? " checked" : "";
    }
}
