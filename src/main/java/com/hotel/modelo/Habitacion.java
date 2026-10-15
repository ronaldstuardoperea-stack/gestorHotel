package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Habitacion fisica del hotel: la unidad que realmente se renta.
 *
 * Conviene entender la diferencia entre dos conceptos que suenan igual:
 *
 *  - El campo 'estado' dice como esta la habitacion HOY (ocupada, en
 *    limpieza, en mantenimiento). Sirve para la vista de ama de llaves.
 *
 *  - La DISPONIBILIDAD para un rango de fechas futuro NO se lee de este
 *    campo. Se calcula en ServicioReservas cruzando las reservas y los
 *    bloqueos vigentes. Una habitacion marcada DISPONIBLE hoy puede
 *    estar vendida para la proxima semana.
 *
 * @author Ronald Perea
 */
public class Habitacion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Libre y lista para recibir huespedes. */
    public static final String ESTADO_DISPONIBLE = "DISPONIBLE";

    /** Hay un huesped hospedado en este momento. */
    public static final String ESTADO_OCUPADA = "OCUPADA";

    /** El huesped ya salio y falta el aseo. */
    public static final String ESTADO_LIMPIEZA = "LIMPIEZA";

    /** Fuera de servicio por una reparacion. */
    public static final String ESTADO_MANTENIMIENTO = "MANTENIMIENTO";

    /** Retirada de la venta por decision administrativa. */
    public static final String ESTADO_BLOQUEADA = "BLOQUEADA";

    private int idHabitacion;
    private String numero;
    private int idTipo;
    private int piso;
    private String estado;
    private String descripcion;
    private boolean activo;

    /*
     * Datos que vienen del JOIN con tipo_habitacion. No se guardan en la
     * tabla habitacion: se llenan al leer para que la vista pueda
     * mostrar el nombre y el precio sin hacer otra consulta.
     */
    private String nombreTipo;
    private BigDecimal tarifaBase;
    private int capacidadAdultos;
    private int capacidadNinos;

    public Habitacion() {
        this.estado = ESTADO_DISPONIBLE;
        this.activo = true;
        this.piso = 1;
    }

    /**
     * Constructor con los datos propios de la tabla habitacion.
     *
     * @param idHabitacion identificador en la base de datos
     * @param numero       numero visible de la puerta (101, 202...)
     * @param idTipo       categoria a la que pertenece
     * @param piso         nivel del edificio
     * @param estado       uno de los ESTADO_* de esta clase
     * @param descripcion  detalle libre de la habitacion
     * @param activo       si sigue formando parte del inventario
     */
    public Habitacion(int idHabitacion, String numero, int idTipo, int piso,
                      String estado, String descripcion, boolean activo) {
        this.idHabitacion = idHabitacion;
        this.numero = numero;
        this.idTipo = idTipo;
        this.piso = piso;
        this.estado = estado;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    // -----------------------------------------------------------------
    // Reglas de negocio
    // -----------------------------------------------------------------

    /**
     * Indica si la habitacion se puede vender hoy mismo, por ejemplo a un
     * huesped que llega sin reserva (walk-in).
     *
     * @return true si esta activa y su estado es DISPONIBLE
     */
    public boolean isLibreAhora() {
        return activo && ESTADO_DISPONIBLE.equals(estado);
    }

    /**
     * Indica si la habitacion esta fuera de servicio, ya sea por
     * mantenimiento o por un bloqueo administrativo.
     *
     * @return true si no se puede rentar en ningun caso
     */
    public boolean isFueraDeServicio() {
        return ESTADO_MANTENIMIENTO.equals(estado) || ESTADO_BLOQUEADA.equals(estado);
    }

    /**
     * Devuelve el nombre de la clase de color de Bootstrap que le
     * corresponde al estado actual. Mantener esta correspondencia en el
     * modelo evita repetir la misma cadena de condiciones en cada JSP.
     *
     * @return sufijo de color: success, danger, warning, secondary o dark
     */
    public String getColorEstado() {
        if (estado == null) {
            return "secondary";
        }
        switch (estado) {
            case ESTADO_DISPONIBLE:    return "success";
            case ESTADO_OCUPADA:       return "danger";
            case ESTADO_LIMPIEZA:      return "warning";
            case ESTADO_MANTENIMIENTO: return "secondary";
            case ESTADO_BLOQUEADA:     return "dark";
            default:                   return "secondary";
        }
    }

    /**
     * Devuelve el nombre del icono de Bootstrap Icons para el estado.
     *
     * @return identificador del icono
     */
    public String getIconoEstado() {
        if (estado == null) {
            return "question-circle";
        }
        switch (estado) {
            case ESTADO_DISPONIBLE:    return "check-circle";
            case ESTADO_OCUPADA:       return "person-fill";
            case ESTADO_LIMPIEZA:      return "brush";
            case ESTADO_MANTENIMIENTO: return "tools";
            case ESTADO_BLOQUEADA:     return "lock-fill";
            default:                   return "question-circle";
        }
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdHabitacion() {
        return idHabitacion;
    }

    public void setIdHabitacion(int idHabitacion) {
        this.idHabitacion = idHabitacion;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public int getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(int idTipo) {
        this.idTipo = idTipo;
    }

    public int getPiso() {
        return piso;
    }

    public void setPiso(int piso) {
        this.piso = piso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreTipo() {
        return nombreTipo;
    }

    public void setNombreTipo(String nombreTipo) {
        this.nombreTipo = nombreTipo;
    }

    public BigDecimal getTarifaBase() {
        return tarifaBase;
    }

    public void setTarifaBase(BigDecimal tarifaBase) {
        this.tarifaBase = tarifaBase;
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

    @Override
    public String toString() {
        return "Habitacion " + numero + " (" + nombreTipo + ")";
    }
}
