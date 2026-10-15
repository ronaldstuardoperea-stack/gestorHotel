package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Categoria comercial de habitacion: Sencilla, Doble, Suite, etc.
 *
 * Es el catalogo del que cuelgan las habitaciones fisicas. La tarifa base
 * y la capacidad viven aqui y no en cada habitacion, de modo que subir el
 * precio de todas las suites sea un solo cambio y no veinte.
 *
 * @author Ronald Perea
 */
public class TipoHabitacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idTipo;
    private String nombre;
    private String descripcion;
    private int capacidadAdultos;
    private int capacidadNinos;
    private BigDecimal tarifaBase;
    private boolean activo;

    /** Cantidad de habitaciones de este tipo. Solo se llena en reportes. */
    private int totalHabitaciones;

    public TipoHabitacion() {
        this.activo = true;
        this.capacidadAdultos = 2;
        this.capacidadNinos = 0;
        this.tarifaBase = BigDecimal.ZERO;
    }

    /**
     * Constructor completo, usado al leer un registro de la base.
     *
     * @param idTipo           identificador del tipo
     * @param nombre           nombre comercial de la categoria
     * @param descripcion      texto que describe lo que incluye
     * @param capacidadAdultos maximo de adultos que admite
     * @param capacidadNinos   maximo de ninos adicionales
     * @param tarifaBase       precio por noche antes de temporada
     * @param activo           si se sigue ofreciendo a la venta
     */
    public TipoHabitacion(int idTipo, String nombre, String descripcion,
                          int capacidadAdultos, int capacidadNinos,
                          BigDecimal tarifaBase, boolean activo) {
        this.idTipo = idTipo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.capacidadAdultos = capacidadAdultos;
        this.capacidadNinos = capacidadNinos;
        this.tarifaBase = tarifaBase;
        this.activo = activo;
    }

    /**
     * Verifica si este tipo de habitacion admite la cantidad de personas
     * solicitada. Se consulta antes de aceptar una reserva, para no
     * meter cinco huespedes en una habitacion sencilla.
     *
     * @param adultos cantidad de adultos
     * @param ninos   cantidad de ninos
     * @return true si el grupo cabe en la habitacion
     */
    public boolean admite(int adultos, int ninos) {
        return adultos <= capacidadAdultos && ninos <= capacidadNinos;
    }

    /**
     * Capacidad total de personas, sumando adultos y ninos.
     * Se muestra en las tarjetas del catalogo.
     *
     * @return maximo de ocupantes de la habitacion
     */
    public int getCapacidadTotal() {
        return capacidadAdultos + capacidadNinos;
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(int idTipo) {
        this.idTipo = idTipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getCapacidadAdultos() {
        return capacidadAdultos;
    }

    public void setCapacidadAdultos(int capacidadAdultos) {
        this.capacidadAdultos = capacidadAdultos;
    }

    public int getCapacidadNinos() {
        return capacidadNinos;
    }

    public void setCapacidadNinos(int capacidadNinos) {
        this.capacidadNinos = capacidadNinos;
    }

    public BigDecimal getTarifaBase() {
        return tarifaBase;
    }

    public void setTarifaBase(BigDecimal tarifaBase) {
        this.tarifaBase = tarifaBase;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getTotalHabitaciones() {
        return totalHabitaciones;
    }

    public void setTotalHabitaciones(int totalHabitaciones) {
        this.totalHabitaciones = totalHabitaciones;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
