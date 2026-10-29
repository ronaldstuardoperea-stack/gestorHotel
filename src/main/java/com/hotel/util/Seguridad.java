package com.hotel.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utilidades de seguridad para el manejo de contrasenas.
 *
 * Las contrasenas NUNCA se guardan en texto plano en la base de datos.
 * En su lugar se guarda el resumen (hash) SHA-256 de la combinacion
 * salt + contrasena.
 *
 * El "salt" es una cadena aleatoria distinta para cada usuario. Sirve
 * para que dos personas que escojan la misma contrasena terminen con
 * hashes diferentes, de modo que ver la tabla no revele quienes
 * comparten clave.
 *
 * Importante: el calculo aqui produce exactamente el mismo resultado que
 * la funcion SHA2(CONCAT(salt, clave), 256) de MariaDB. Por eso los
 * usuarios de prueba se pueden sembrar directamente desde el script SQL
 * y aun asi validan correctamente en este login.
 *
 * @author Ronald Perea
 */
public class Seguridad {

    /** Generador de numeros aleatorios apto para uso criptografico. */
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Caracteres validos en la representacion hexadecimal. */
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** Clase de utilidad: no se instancia. */
    private Seguridad() {
    }

    /**
     * Genera un salt nuevo de 32 caracteres hexadecimales (16 bytes).
     * Se llama una sola vez, al crear o cambiar la clave de un usuario.
     *
     * @return cadena aleatoria de 32 caracteres
     */
    public static String generarSalt() {
        byte[] bytes = new byte[16];
        ALEATORIO.nextBytes(bytes);
        return aHexadecimal(bytes);
    }

    /**
     * Calcula el hash SHA-256 de (salt + clave).
     *
     * @param salt  cadena aleatoria asociada al usuario
     * @param clave contrasena en texto plano escrita por la persona
     * @return hash de 64 caracteres hexadecimales en minuscula
     */
    public static String calcularHash(String salt, String clave) {
        if (salt == null) {
            salt = "";
        }
        if (clave == null) {
            clave = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resumen = digest.digest(
                    (salt + clave).getBytes(StandardCharsets.UTF_8));
            return aHexadecimal(resumen);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 forma parte de la plataforma Java desde siempre;
            // si faltara, la instalacion del JDK estaria danada.
            throw new IllegalStateException(
                    "El algoritmo SHA-256 no esta disponible en esta JVM.", e);
        }
    }

    /**
     * Verifica si una contrasena escrita corresponde al hash guardado.
     *
     * La comparacion se hace con MessageDigest.isEqual, que recorre
     * siempre los dos arreglos completos. Asi el tiempo de respuesta no
     * delata cuantos caracteres del hash se acertaron.
     *
     * @param claveEscrita contrasena que la persona acaba de teclear
     * @param salt         salt guardado para ese usuario
     * @param hashGuardado hash guardado en la base de datos
     * @return true si la contrasena es correcta
     */
    public static boolean verificar(String claveEscrita, String salt, String hashGuardado) {
        if (claveEscrita == null || hashGuardado == null) {
            return false;
        }
        String hashCalculado = calcularHash(salt, claveEscrita);
        return MessageDigest.isEqual(
                hashCalculado.getBytes(StandardCharsets.UTF_8),
                hashGuardado.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convierte un arreglo de bytes a su representacion hexadecimal en
     * minusculas, que es el mismo formato que devuelve SHA2() en MariaDB.
     *
     * @param bytes datos binarios a convertir
     * @return cadena hexadecimal de longitud 2 * bytes.length
     */
    private static String aHexadecimal(byte[] bytes) {
        char[] salida = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int valor = bytes[i] & 0xFF;          // byte sin signo (0..255)
            salida[i * 2] = HEX[valor >>> 4];     // primeros 4 bits
            salida[i * 2 + 1] = HEX[valor & 0x0F]; // ultimos 4 bits
        }
        return new String(salida);
    }
}
