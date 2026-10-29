package com.hotel.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto unico de acceso a la base de datos del hotel.
 *
 * Toda la aplicacion pide sus conexiones aqui. Centralizarlo en una sola
 * clase tiene una ventaja practica: si manana cambia el servidor, el
 * usuario o la contrasena de MySQL, se modifica UN solo archivo y no las
 * decenas de clases DAO que consultan la base.
 *
 * Cada metodo DAO abre su conexion y la cierra con try-with-resources.
 * Para un sistema de este tamano es suficiente y evita la complejidad de
 * un pool de conexiones.
 *
 * @author Ronald Perea
 */
public class ConexionBD {

    /**
     * Cadena de conexion JDBC.
     *
     * useSSL=false        -> XAMPP no trae certificado, evita la advertencia.
     * serverTimezone=UTC  -> impide el error de zona horaria del driver.
     * allowPublicKeyRetrieval=true -> necesario en algunas versiones de MySQL 8.
     * characterEncoding=UTF-8 -> para que las tildes y la letra n viajen bien.
     */
    private static final String URL =
            "jdbc:mysql://localhost:3306/hotel_db"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true"
            + "&characterEncoding=UTF-8";

    /** Usuario de MySQL. En XAMPP el predeterminado es 'root'. */
    private static final String USUARIO = "root";

    /** Contrasena de MySQL. En XAMPP viene vacia por omision. */
    private static final String PASSWORD = "";

    /**
     * Constructor privado: esta es una clase de utilidad y no tiene
     * sentido crear objetos de ella, porque todos sus metodos son
     * estaticos.
     */
    private ConexionBD() {
    }

    /*
     * El driver se registra una sola vez, cuando la clase se carga en
     * memoria. Si el JAR del conector no esta en el classpath, el fallo
     * se detecta de inmediato y no en medio de una consulta.
     */
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "No se encontro el driver de MySQL. Verifique que la "
                    + "dependencia mysql-connector-j este declarada en el pom.xml.");
        }
    }

    /**
     * Abre y devuelve una conexion activa a la base de datos.
     *
     * Quien llama a este metodo es responsable de cerrar la conexion.
     * La forma recomendada es try-with-resources:
     *
     * <pre>
     *   try (Connection con = ConexionBD.conectar()) { ... }
     * </pre>
     *
     * @return conexion lista para usar
     * @throws SQLException si el servidor no responde o las credenciales
     *                      son incorrectas
     */
    public static Connection conectar() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USUARIO, PASSWORD);
        } catch (SQLException e) {
            // Se relanza con un mensaje entendible para el usuario final,
            // conservando la excepcion original como causa.
            throw new SQLException(
                    "No se pudo conectar con la base de datos 'hotel_db'. "
                    + "Verifique que MySQL este iniciado en XAMPP. "
                    + "Detalle tecnico: " + e.getMessage(), e);
        }
    }

    /**
     * Comprueba si la base de datos esta accesible en este momento.
     * Se usa para avisar al operador cuando MySQL no esta levantado, en
     * lugar de mostrarle un error tecnico en medio de una operacion.
     *
     * @return true si la conexion se establecio correctamente
     */
    public static boolean hayConexion() {
        try (Connection con = conectar()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
