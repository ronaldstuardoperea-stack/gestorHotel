package com.hotel.util;

/**
 * Excepcion que representa un dato incorrecto enviado por el usuario.
 *
 * Se distingue a proposito de SQLException y de las demas excepciones
 * tecnicas: esta NO indica una falla del sistema, sino que la persona
 * escribio algo que no corresponde (una fecha invalida, un monto
 * negativo, un campo obligatorio vacio).
 *
 * Los Servlets la capturan y devuelven el mensaje tal cual a la pantalla,
 * porque esta redactado en lenguaje entendible para el recepcionista.
 * En cambio, una SQLException se traduce a un mensaje generico y el
 * detalle tecnico se manda a la bitacora del servidor.
 *
 * Es una excepcion no verificada (extiende RuntimeException) para no
 * obligar a declararla en cada firma de metodo dentro de la cadena de
 * validaciones.
 *
 * @author Ronald Perea
 */
public class ValidacionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepcion con un mensaje dirigido al usuario final.
     *
     * @param mensaje explicacion clara de que dato esta mal y por que
     */
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
