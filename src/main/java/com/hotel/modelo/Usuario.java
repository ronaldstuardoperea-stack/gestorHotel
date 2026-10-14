package com.hotel.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Empleado del hotel autorizado para usar el sistema.
 *
 * Esta clase se guarda en la sesion HTTP despues de un inicio de sesion
 * correcto, por eso implementa Serializable: Tomcat necesita poder
 * escribirla en disco cuando reinicia y quiere conservar las sesiones
 * abiertas.
 *
 * El objeto que vive en la sesion NUNCA lleva el hash ni el salt: esos
 * campos se usan solo dentro del DAO al momento de validar la clave y se
 * dejan en null despues. Asi, aunque un JSP imprimiera el usuario por
 * error, no habria forma de filtrar la contrasena.
 *
 * @author Ronald Perea
 */
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Rol con permisos totales, incluida la administracion de usuarios. */
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    /** Rol operativo: atiende reservas, check-in, cobros y facturas. */
    public static final String ROL_RECEPCION = "RECEPCION";

    /** Rol de solo lectura: consulta y reportes, sin modificar nada. */
    public static final String ROL_AUDITOR = "AUDITOR";

    private int idUsuario;
    private String nombreCompleto;
    private String usuario;
    private String salt;
    private String claveHash;
    private String rol;
    private boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimoAcceso;

    /** Constructor vacio, necesario para construir el objeto por pasos. */
    public Usuario() {
        this.activo = true;
        this.rol = ROL_RECEPCION;
    }

    /**
     * Constructor con los datos que se muestran en pantalla.
     * Deja fuera el salt y el hash a proposito.
     *
     * @param idUsuario      identificador en la base de datos
     * @param nombreCompleto nombre de la persona
     * @param usuario        nombre de ingreso al sistema
     * @param rol            uno de los ROL_* definidos en esta clase
     * @param activo         si puede o no iniciar sesion
     */
    public Usuario(int idUsuario, String nombreCompleto, String usuario,
                   String rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.usuario = usuario;
        this.rol = rol;
        this.activo = activo;
    }

    // -----------------------------------------------------------------
    // Reglas de negocio propias del usuario
    // -----------------------------------------------------------------

    /**
     * Indica si el usuario puede administrar el sistema: crear otros
     * usuarios, modificar el catalogo de habitaciones y anular facturas.
     *
     * @return true si el rol es ADMINISTRADOR
     */
    public boolean isAdministrador() {
        return ROL_ADMINISTRADOR.equals(rol);
    }

    /**
     * Indica si el usuario solo puede consultar.
     * El auditor revisa reportes y estados de cuenta, pero no registra
     * movimientos ni modifica catalogos.
     *
     * @return true si el rol es AUDITOR
     */
    public boolean isSoloLectura() {
        return ROL_AUDITOR.equals(rol);
    }

    /**
     * Indica si el usuario puede registrar movimientos: reservas,
     * check-in, check-out, cargos, pagos y facturas.
     *
     * @return true para ADMINISTRADOR y RECEPCION
     */
    public boolean isOperador() {
        return !isSoloLectura();
    }

    /**
     * Devuelve las iniciales del nombre, que la barra superior muestra
     * dentro del circulo del avatar.
     *
     * @return una o dos letras en mayuscula
     */
    public String getIniciales() {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "?";
        }
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].charAt(0) + "" + partes[1].charAt(0)).toUpperCase();
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public void setClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    @Override
    public String toString() {
        return nombreCompleto + " (" + usuario + " / " + rol + ")";
    }
}
