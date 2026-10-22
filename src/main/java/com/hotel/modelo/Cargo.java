package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Consumo o gasto anotado a la cuenta de una reserva.
 *
 * Cubre todo lo que el huesped consume fuera del hospedaje: restaurante,
 * minibar, lavanderia, spa, llamadas, transporte. Se le suele llamar
 * "cargo a la habitacion".
 *
 * Dos campos merecen explicacion:
 *
 *  - idOcupante: si viene lleno, el consumo queda atribuido a un huesped
 *    concreto y puede facturarse por separado. Si viene nulo, el cargo
 *    es de la habitacion en general.
 *
 *  - idFactura: se llena cuando el cargo ya fue incluido en una factura.
 *    Mientras sea nulo, el cargo esta pendiente de facturar. Asi se evita
 *    cobrar dos veces el mismo consumo.
 *
 * @author Ronald Perea
 */
public class Cargo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CAT_RESTAURANTE = "RESTAURANTE";
    public static final String CAT_MINIBAR = "MINIBAR";
    public static final String CAT_LAVANDERIA = "LAVANDERIA";
    public static final String CAT_SPA = "SPA";
    public static final String CAT_TELEFONO = "TELEFONO";
    public static final String CAT_TRANSPORTE = "TRANSPORTE";
    public static final String CAT_OTRO = "OTRO";

    /** Categorias validas, en el orden en que se muestran en el combo. */
    public static final String[] CATEGORIAS = {
        CAT_RESTAURANTE, CAT_MINIBAR, CAT_LAVANDERIA,
        CAT_SPA, CAT_TELEFONO, CAT_TRANSPORTE, CAT_OTRO
    };

    private int idCargo;
    private int idReserva;
    private Integer idOcupante;
    private String concepto;
    private String categoria;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal monto;
    private Integer idFactura;
    private LocalDateTime fecha;
    private int idUsuario;

    /* Datos traidos por JOIN, para mostrar sin consultas adicionales. */
    private String nombreOcupante;
    private String nombreUsuario;
    private String codigoReserva;

    public Cargo() {
        this.categoria = CAT_OTRO;
        this.cantidad = BigDecimal.ONE;
        this.precioUnitario = BigDecimal.ZERO;
        this.monto = BigDecimal.ZERO;
    }

    /**
     * Recalcula el importe del cargo como cantidad por precio unitario.
     *
     * El resultado se guarda tambien en la columna 'monto' de la base.
     * Podria calcularse al vuelo en cada consulta, pero guardarlo hace
     * que los reportes de ingresos sean mucho mas simples y rapidos.
     */
    public void recalcularMonto() {
        if (cantidad == null || precioUnitario == null) {
            this.monto = BigDecimal.ZERO;
            return;
        }
        this.monto = cantidad.multiply(precioUnitario)
                             .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Indica si el cargo todavia no ha sido incluido en ninguna factura.
     *
     * @return true si esta pendiente de facturar
     */
    public boolean isPendienteDeFacturar() {
        return idFactura == null;
    }

    /**
     * Indica si el consumo esta atribuido a un huesped en particular.
     *
     * @return true si tiene ocupante asignado
     */
    public boolean isConOcupante() {
        return idOcupante != null;
    }

    /**
     * Icono de Bootstrap Icons que representa la categoria del consumo.
     *
     * @return identificador del icono
     */
    public String getIconoCategoria() {
        if (categoria == null) {
            return "receipt";
        }
        switch (categoria) {
            case CAT_RESTAURANTE: return "cup-hot";
            case CAT_MINIBAR:     return "cup-straw";
            case CAT_LAVANDERIA:  return "basket";
            case CAT_SPA:         return "flower1";
            case CAT_TELEFONO:    return "telephone";
            case CAT_TRANSPORTE:  return "car-front";
            default:              return "receipt";
        }
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdCargo() {
        return idCargo;
    }

    public void setIdCargo(int idCargo) {
        this.idCargo = idCargo;
    }

    public int getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(int idReserva) {
        this.idReserva = idReserva;
    }

    public Integer getIdOcupante() {
        return idOcupante;
    }

    public void setIdOcupante(Integer idOcupante) {
        this.idOcupante = idOcupante;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public Integer getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(Integer idFactura) {
        this.idFactura = idFactura;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreOcupante() {
        return nombreOcupante;
    }

    public void setNombreOcupante(String nombreOcupante) {
        this.nombreOcupante = nombreOcupante;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public void setCodigoReserva(String codigoReserva) {
        this.codigoReserva = codigoReserva;
    }

    @Override
    public String toString() {
        return concepto + " (Q " + monto + ")";
    }
}
