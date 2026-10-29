package com.hotel.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Convierte y valida los datos que llegan desde los formularios web.
 *
 * Todo lo que manda un navegador llega como texto, incluso los numeros y
 * las fechas. Esta clase se encarga de convertir ese texto al tipo Java
 * correcto y, si el valor no sirve, lanza una {@link ValidacionException}
 * con un mensaje claro que el Servlet puede mostrar en pantalla.
 *
 * Concentrar las validaciones aqui cumple con el requerimiento de
 * "validacion de datos y control de errores para evitar fallos en
 * ejecucion": ningun Servlet llama a Integer.parseInt() directamente, y
 * por lo tanto ningun dato mal escrito puede tumbar la aplicacion con un
 * NumberFormatException sin controlar.
 *
 * @author Ronald Perea
 */
public class Validador {

    /**
     * Patron de correo electronico. Es deliberadamente permisivo: basta
     * con que haya texto, una arroba, texto, un punto y una extension.
     * Validar correos con exactitud total es imposible por expresion
     * regular, y rechazar direcciones validas molesta mas que ayuda.
     */
    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    /** Clase de utilidad: no se instancia. */
    private Validador() {
    }

    // -----------------------------------------------------------------
    // TEXTO
    // -----------------------------------------------------------------

    /**
     * Limpia un texto opcional: quita espacios sobrantes y convierte la
     * cadena vacia en null, para que la base de datos guarde NULL y no
     * una cadena en blanco.
     *
     * @param valor texto crudo del formulario (puede ser null)
     * @return texto limpio, o null si venia vacio
     */
    public static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /**
     * Exige que un campo de texto venga con contenido.
     *
     * @param valor  texto crudo del formulario
     * @param campo  nombre del campo tal como lo ve el usuario
     * @return texto limpio y garantizado no vacio
     * @throws ValidacionException si el campo viene vacio
     */
    public static String textoRequerido(String valor, String campo) {
        String limpio = limpiar(valor);
        if (limpio == null) {
            throw new ValidacionException("El campo " + campo + " es obligatorio.");
        }
        return limpio;
    }

    /**
     * Exige texto y ademas verifica que no exceda el largo maximo de la
     * columna, para que el error se muestre en pantalla en lugar de
     * reventar como "Data too long for column" al insertar.
     *
     * @param valor     texto crudo del formulario
     * @param campo     nombre visible del campo
     * @param maxLargo  largo maximo permitido
     * @return texto limpio y validado
     * @throws ValidacionException si viene vacio o es demasiado largo
     */
    public static String textoRequerido(String valor, String campo, int maxLargo) {
        String limpio = textoRequerido(valor, campo);
        if (limpio.length() > maxLargo) {
            throw new ValidacionException(
                    "El campo " + campo + " no puede tener mas de "
                    + maxLargo + " caracteres.");
        }
        return limpio;
    }

    /**
     * Limpia un texto opcional recortandolo al largo maximo de la
     * columna. Al ser opcional no se considera error que sobre texto:
     * simplemente se trunca.
     *
     * @param valor    texto crudo del formulario
     * @param maxLargo largo maximo permitido
     * @return texto limpio y recortado, o null si venia vacio
     */
    public static String textoOpcional(String valor, int maxLargo) {
        String limpio = limpiar(valor);
        if (limpio != null && limpio.length() > maxLargo) {
            limpio = limpio.substring(0, maxLargo);
        }
        return limpio;
    }

    /**
     * Valida un correo electronico opcional.
     *
     * @param valor correo crudo del formulario
     * @return correo limpio, o null si no se escribio ninguno
     * @throws ValidacionException si se escribio algo que no es un correo
     */
    public static String correoOpcional(String valor) {
        String limpio = limpiar(valor);
        if (limpio == null) {
            return null;
        }
        if (!PATRON_CORREO.matcher(limpio).matches()) {
            throw new ValidacionException(
                    "El correo electronico '" + limpio + "' no tiene un formato valido.");
        }
        return limpio;
    }

    // -----------------------------------------------------------------
    // NUMEROS ENTEROS
    // -----------------------------------------------------------------

    /**
     * Convierte un texto a numero entero obligatorio.
     *
     * @param valor texto crudo del formulario
     * @param campo nombre visible del campo
     * @return el numero convertido
     * @throws ValidacionException si viene vacio o no es un numero
     */
    public static int entero(String valor, String campo) {
        String limpio = textoRequerido(valor, campo);
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            throw new ValidacionException(
                    "El campo " + campo + " debe ser un numero entero. "
                    + "Se recibio: '" + limpio + "'.");
        }
    }

    /**
     * Convierte un texto a entero y verifica que caiga dentro de un rango.
     *
     * @param valor  texto crudo del formulario
     * @param campo  nombre visible del campo
     * @param minimo valor minimo aceptado (inclusive)
     * @param maximo valor maximo aceptado (inclusive)
     * @return el numero convertido y validado
     * @throws ValidacionException si no es numero o esta fuera de rango
     */
    public static int enteroEnRango(String valor, String campo, int minimo, int maximo) {
        int numero = entero(valor, campo);
        if (numero < minimo || numero > maximo) {
            throw new ValidacionException(
                    "El campo " + campo + " debe estar entre "
                    + minimo + " y " + maximo + ". Se recibio: " + numero + ".");
        }
        return numero;
    }

    /**
     * Convierte un texto a entero, devolviendo un valor por omision
     * cuando el campo viene vacio o no es un numero. Util para
     * parametros de la URL como el numero de pagina o un filtro.
     *
     * @param valor       texto crudo
     * @param porOmision  valor a usar si no hay un numero valido
     * @return el numero convertido o el valor por omision
     */
    public static int enteroOpcional(String valor, int porOmision) {
        String limpio = limpiar(valor);
        if (limpio == null) {
            return porOmision;
        }
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            return porOmision;
        }
    }

    // -----------------------------------------------------------------
    // MONTOS
    // -----------------------------------------------------------------

    /**
     * Convierte un texto a monto de dinero.
     *
     * Se usa BigDecimal y no double porque double arrastra errores de
     * redondeo (0.1 + 0.2 no da exactamente 0.3), y en una factura de
     * hotel eso se acumula hasta descuadrar el total.
     *
     * Se aceptan comas como separador de miles, porque es comun que el
     * recepcionista escriba "1,250.00".
     *
     * @param valor texto crudo del formulario
     * @param campo nombre visible del campo
     * @return monto con dos decimales
     * @throws ValidacionException si viene vacio o no es un numero
     */
    public static BigDecimal monto(String valor, String campo) {
        String limpio = textoRequerido(valor, campo).replace(",", "");
        try {
            return new BigDecimal(limpio).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new ValidacionException(
                    "El campo " + campo + " debe ser una cantidad valida. "
                    + "Se recibio: '" + limpio + "'.");
        }
    }

    /**
     * Convierte un texto a monto y exige que sea mayor que cero.
     * Se usa en pagos y precios, donde un cero o un negativo no tiene
     * sentido y descuadraria los saldos.
     *
     * @param valor texto crudo del formulario
     * @param campo nombre visible del campo
     * @return monto positivo con dos decimales
     * @throws ValidacionException si no es numero o no es positivo
     */
    public static BigDecimal montoPositivo(String valor, String campo) {
        BigDecimal cantidad = monto(valor, campo);
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException(
                    "El campo " + campo + " debe ser mayor que cero.");
        }
        return cantidad;
    }

    /**
     * Convierte un texto a monto y exige que no sea negativo.
     * Se usa en descuentos, donde cero si es un valor valido.
     *
     * @param valor texto crudo del formulario
     * @param campo nombre visible del campo
     * @return monto mayor o igual a cero
     * @throws ValidacionException si no es numero o es negativo
     */
    public static BigDecimal montoNoNegativo(String valor, String campo) {
        BigDecimal cantidad = monto(valor, campo);
        if (cantidad.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException(
                    "El campo " + campo + " no puede ser negativo.");
        }
        return cantidad;
    }

    // -----------------------------------------------------------------
    // FECHAS
    // -----------------------------------------------------------------

    /**
     * Convierte un texto a fecha. Espera el formato ISO (aaaa-mm-dd),
     * que es exactamente el que envian los campos input type="date" de
     * HTML5, de modo que no hay ambiguedad entre dia y mes.
     *
     * @param valor texto crudo del formulario
     * @param campo nombre visible del campo
     * @return la fecha convertida
     * @throws ValidacionException si viene vacia o mal escrita
     */
    public static LocalDate fecha(String valor, String campo) {
        String limpio = textoRequerido(valor, campo);
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            throw new ValidacionException(
                    "El campo " + campo + " no es una fecha valida. "
                    + "Use el formato aaaa-mm-dd.");
        }
    }

    /**
     * Convierte un texto a fecha opcional.
     *
     * @param valor texto crudo del formulario
     * @return la fecha convertida, o null si el campo venia vacio
     * @throws ValidacionException si hay texto pero no es una fecha
     */
    public static LocalDate fechaOpcional(String valor) {
        String limpio = limpiar(valor);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            throw new ValidacionException(
                    "La fecha '" + limpio + "' no es valida. Use el formato aaaa-mm-dd.");
        }
    }

    // -----------------------------------------------------------------
    // CATALOGOS
    // -----------------------------------------------------------------

    /**
     * Verifica que un valor pertenezca a una lista cerrada de opciones.
     *
     * Se usa para los campos que en la base son ENUM (estados, metodos de
     * pago, categorias). Protege contra un usuario que manipule el HTML
     * del formulario para enviar un valor inventado, que en la base
     * terminaria como cadena vacia sin aviso.
     *
     * @param valor    texto crudo del formulario
     * @param campo    nombre visible del campo
     * @param opciones valores aceptados
     * @return el valor validado, en mayusculas
     * @throws ValidacionException si el valor no esta entre las opciones
     */
    public static String opcion(String valor, String campo, String... opciones) {
        String limpio = textoRequerido(valor, campo).toUpperCase();
        for (String opcion : opciones) {
            if (opcion.equals(limpio)) {
                return limpio;
            }
        }
        throw new ValidacionException(
                "El valor '" + limpio + "' no es valido para el campo " + campo + ".");
    }
}
