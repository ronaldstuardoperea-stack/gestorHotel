package com.hotel.modelo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Periodo en el que una habitacion queda retirada de la venta.
 *
 * Responde al requerimiento de "bloqueo de habitaciones". Se usa cuando
 * la habitacion no se puede rentar por una razon que no es un huesped:
 * una remodelacion, una fuga de agua, o porque la direccion la aparto
 * para uso interno.
 *
 * La diferencia con el estado MANTENIMIENTO de la habitacion es el
 * tiempo: el estado dice como esta la habitacion hoy, mientras que el
 * bloqueo cubre un rango de fechas y por lo tanto impide que alguien
 * reserve esa habitacion para el mes que viene.
 *
 * Un bloqueo no se borra cuando termina: se marca como inactivo, para
 * conservar el historial de por que la habitacion no estuvo disponible.
 *
 * @author Ronald Perea
 */
public class BloqueoHabitacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idBloqueo;
    private int idHabitacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivo;
    private int idUsuario;
    private LocalDateTime fechaRegistro;
    private boolean activo;

    /* Datos traidos por JOIN. */
    private String numeroHabitacion;
    private String nombreTipo;
    private String nombreUsuario;

    public BloqueoHabitacion() {
        this.activo = true;
    }

    /**
     * Constructor completo, usado al leer un registro de la base.
     *
     * @param idBloqueo    identificador en la base de datos
     * @param idHabitacion habitacion que se retira de la venta
     * @param fechaInicio  primer dia bloqueado, inclusive
     * @param fechaFin     ultimo dia bloqueado, inclusive
     * @param motivo       razon del bloqueo
     * @param idUsuario    empleado que lo registro
     * @param activo       si el bloqueo sigue surtiendo efecto
     */
    public BloqueoHabitacion(int idBloqueo, int idHabitacion,
                             LocalDate fechaInicio, LocalDate fechaFin,
                             String motivo, int idUsuario, boolean activo) {
        this.idBloqueo = idBloqueo;
        this.idHabitacion = idHabitacion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.motivo = motivo;
        this.idUsuario = idUsuario;
        this.activo = activo;
    }

    /**
     * Indica si el bloqueo esta surtiendo efecto el dia de hoy.
     *
     * @return true si esta activo y hoy cae dentro del rango
     */
    public boolean isVigente() {
        LocalDate hoy = LocalDate.now();
        return activo
            && fechaInicio != null && fechaFin != null
            && !hoy.isBefore(fechaInicio)
            && !hoy.isAfter(fechaFin);
    }

    /**
     * Indica si el bloqueo ya termino segun el calendario, aunque siga
     * marcado como activo.
     *
     * @return true si la fecha de fin ya paso
     */
    public boolean isVencido() {
        return fechaFin != null && fechaFin.isBefore(LocalDate.now());
    }

    /**
     * Indica si el bloqueo se traslapa con un rango de fechas dado.
     *
     * La condicion es la regla clasica de solapamiento de intervalos:
     * dos rangos se cruzan si cada uno empieza antes de que el otro
     * termine. Se usa al verificar disponibilidad antes de reservar.
     *
     * @param desde primer dia del rango a comparar
     * @param hasta ultimo dia del rango a comparar
     * @return true si hay traslape y la habitacion no se puede vender
     */
    public boolean seCruzaCon(LocalDate desde, LocalDate hasta) {
        if (!activo || fechaInicio == null || fechaFin == null
                || desde == null || hasta == null) {
            return false;
        }
        return !fechaInicio.isAfter(hasta) && !fechaFin.isBefore(desde);
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdBloqueo() {
        return idBloqueo;
    }

    public void setIdBloqueo(int idBloqueo) {
        this.idBloqueo = idBloqueo;
    }

    public int getIdHabitacion() {
        return idHabitacion;
    }

    public void setIdHabitacion(int idHabitacion) {
        this.idHabitacion = idHabitacion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNumeroHabitacion() {
        return numeroHabitacion;
    }

    public void setNumeroHabitacion(String numeroHabitacion) {
        this.numeroHabitacion = numeroHabitacion;
    }

    public String getNombreTipo() {
        return nombreTipo;
    }

    public void setNombreTipo(String nombreTipo) {
        this.nombreTipo = nombreTipo;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    @Override
    public String toString() {
        return "Habitacion " + numeroHabitacion + " bloqueada del "
             + fechaInicio + " al " + fechaFin;
    }
}
