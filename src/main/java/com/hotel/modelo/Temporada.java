package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Periodo del ano con un precio distinto al habitual.
 *
 * El campo 'factor' es un multiplicador que se aplica sobre la tarifa
 * base del tipo de habitacion:
 *
 *   1.00 -> precio normal
 *   1.40 -> Semana Santa, 40% de recargo
 *   0.85 -> temporada baja, 15% de descuento
 *
 * Guardar un multiplicador en lugar de un precio fijo evita tener que
 * capturar una tarifa por cada combinacion de tipo y temporada: con
 * cinco tipos y cinco temporadas serian veinticinco precios que mantener.
 *
 * @author Ronald Perea
 */
public class Temporada implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idTemporada;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal factor;
    private boolean activo;

    public Temporada() {
        this.factor = BigDecimal.ONE;
        this.activo = true;
    }

    /**
     * Constructor completo, usado al leer un registro de la base.
     *
     * @param idTemporada identificador en la base de datos
     * @param nombre      nombre de la temporada
     * @param fechaInicio primer dia del periodo, inclusive
     * @param fechaFin    ultimo dia del periodo, inclusive
     * @param factor      multiplicador sobre la tarifa base
     * @param activo      si la temporada se toma en cuenta al cotizar
     */
    public Temporada(int idTemporada, String nombre, LocalDate fechaInicio,
                     LocalDate fechaFin, BigDecimal factor, boolean activo) {
        this.idTemporada = idTemporada;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.factor = factor;
        this.activo = activo;
    }

    // -----------------------------------------------------------------
    // Reglas de negocio
    // -----------------------------------------------------------------

    /**
     * Indica si una fecha cae dentro de esta temporada.
     * Los dos extremos del rango se consideran incluidos.
     *
     * @param fecha fecha a evaluar
     * @return true si la fecha pertenece al periodo
     */
    public boolean incluye(LocalDate fecha) {
        if (fecha == null || fechaInicio == null || fechaFin == null) {
            return false;
        }
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    /**
     * Aplica el multiplicador de la temporada a una tarifa base.
     *
     * @param tarifaBase precio por noche sin ajustar
     * @return precio ajustado, redondeado a dos decimales
     */
    public BigDecimal aplicarA(BigDecimal tarifaBase) {
        if (tarifaBase == null) {
            return BigDecimal.ZERO;
        }
        if (factor == null) {
            return tarifaBase.setScale(2, RoundingMode.HALF_UP);
        }
        return tarifaBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Expresa el factor como porcentaje para mostrarlo en pantalla:
     * un factor de 1.40 se lee como "+40%" y uno de 0.85 como "-15%".
     *
     * @return variacion porcentual con signo
     */
    public String getVariacionPorcentual() {
        if (factor == null) {
            return "0%";
        }
        BigDecimal porcentaje = factor
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);
        int comparacion = porcentaje.compareTo(BigDecimal.ZERO);
        if (comparacion == 0) {
            return "Sin recargo";
        }
        return (comparacion > 0 ? "+" : "") + porcentaje + "%";
    }

    /**
     * Indica si la temporada encarece la tarifa, para pintarla de un
     * color distinto en la tabla.
     *
     * @return true si el factor es mayor que uno
     */
    public boolean isRecargo() {
        return factor != null && factor.compareTo(BigDecimal.ONE) > 0;
    }

    /**
     * Indica si la temporada esta vigente el dia de hoy.
     *
     * @return true si hoy cae dentro del periodo y la temporada esta activa
     */
    public boolean isVigente() {
        return activo && incluye(LocalDate.now());
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdTemporada() {
        return idTemporada;
    }

    public void setIdTemporada(int idTemporada) {
        this.idTemporada = idTemporada;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombre + " (" + getVariacionPorcentual() + ")";
    }
}
