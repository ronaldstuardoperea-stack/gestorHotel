package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Renglon impreso dentro de una factura.
 *
 * Los datos se copian ya calculados (descripcion, cantidad y precio) en
 * lugar de apuntar al cargo original. La razon es legal y practica: una
 * factura emitida es un documento historico. Si dentro de un mes se
 * corrige el precio de un consumo o se borra el registro, la factura que
 * ya se le entrego al cliente debe seguir diciendo exactamente lo mismo
 * que decia el dia que se imprimio.
 *
 * @author Ronald Perea
 */
public class DetalleFactura implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idDetalle;
    private int idFactura;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal importe;

    public DetalleFactura() {
        this.cantidad = BigDecimal.ONE;
        this.precioUnitario = BigDecimal.ZERO;
        this.importe = BigDecimal.ZERO;
    }

    /**
     * Crea un renglon calculando el importe automaticamente.
     *
     * @param descripcion    texto que se imprime en la factura
     * @param cantidad       unidades o noches
     * @param precioUnitario precio de cada unidad
     */
    public DetalleFactura(String descripcion, BigDecimal cantidad,
                          BigDecimal precioUnitario) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.importe = cantidad.multiply(precioUnitario)
                               .setScale(2, RoundingMode.HALF_UP);
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public int getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(int idFactura) {
        this.idFactura = idFactura;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    @Override
    public String toString() {
        return descripcion + " x" + cantidad + " = Q " + importe;
    }
}
