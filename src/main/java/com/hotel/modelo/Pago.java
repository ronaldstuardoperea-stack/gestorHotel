package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de dinero asociado a una reserva.
 *
 * El ANTICIPO que pide el enunciado no es una tabla aparte: es
 * simplemente un pago registrado antes de la llegada del huesped, que
 * sirve para garantizar la reserva. Por eso los cuatro tipos conviven en
 * la misma tabla y comparten la misma logica de saldos.
 *
 * El REEMBOLSO es el unico que resta en lugar de sumar: representa
 * dinero que sale de la caja hacia el cliente, normalmente al cancelar
 * una reserva que ya tenia anticipo.
 *
 * @author Ronald Perea
 */
public class Pago implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Pago adelantado para garantizar la reserva. */
    public static final String TIPO_ANTICIPO = "ANTICIPO";

    /** Pago parcial durante la estadia. */
    public static final String TIPO_ABONO = "ABONO";

    /** Pago final que salda la cuenta, normalmente al hacer check-out. */
    public static final String TIPO_LIQUIDACION = "LIQUIDACION";

    /** Devolucion de dinero al cliente. Resta del total pagado. */
    public static final String TIPO_REEMBOLSO = "REEMBOLSO";

    /** Tipos validos, en el orden en que se muestran en el combo. */
    public static final String[] TIPOS = {
        TIPO_ANTICIPO, TIPO_ABONO, TIPO_LIQUIDACION, TIPO_REEMBOLSO
    };

    public static final String METODO_EFECTIVO = "EFECTIVO";
    public static final String METODO_TARJETA = "TARJETA";
    public static final String METODO_TRANSFERENCIA = "TRANSFERENCIA";
    public static final String METODO_DEPOSITO = "DEPOSITO";

    /** Metodos de pago validos. */
    public static final String[] METODOS = {
        METODO_EFECTIVO, METODO_TARJETA, METODO_TRANSFERENCIA, METODO_DEPOSITO
    };

    private int idPago;
    private int idReserva;
    private String tipo;
    private String metodo;
    private BigDecimal monto;
    private String referencia;
    private LocalDateTime fecha;
    private int idUsuario;

    /* Datos traidos por JOIN. */
    private String nombreUsuario;
    private String codigoReserva;
    private String nombreCliente;

    public Pago() {
        this.tipo = TIPO_ABONO;
        this.metodo = METODO_EFECTIVO;
        this.monto = BigDecimal.ZERO;
    }

    /**
     * Devuelve el monto con el signo que le corresponde para sumar
     * saldos: negativo si es un reembolso, positivo en los demas casos.
     *
     * Tener esta regla en el modelo evita repetir la misma condicion en
     * cada consulta y en cada reporte.
     *
     * @return monto con signo
     */
    public BigDecimal getMontoConSigno() {
        if (monto == null) {
            return BigDecimal.ZERO;
        }
        return TIPO_REEMBOLSO.equals(tipo) ? monto.negate() : monto;
    }

    /**
     * Indica si el movimiento es una salida de dinero de la caja.
     *
     * @return true si es un reembolso
     */
    public boolean isSalida() {
        return TIPO_REEMBOLSO.equals(tipo);
    }

    /**
     * Color de Bootstrap que corresponde al tipo de pago.
     *
     * @return sufijo de color de Bootstrap
     */
    public String getColorTipo() {
        if (tipo == null) {
            return "secondary";
        }
        switch (tipo) {
            case TIPO_ANTICIPO:    return "info";
            case TIPO_ABONO:       return "primary";
            case TIPO_LIQUIDACION: return "success";
            case TIPO_REEMBOLSO:   return "danger";
            default:               return "secondary";
        }
    }

    /**
     * Icono de Bootstrap Icons que representa el metodo de pago.
     *
     * @return identificador del icono
     */
    public String getIconoMetodo() {
        if (metodo == null) {
            return "cash";
        }
        switch (metodo) {
            case METODO_EFECTIVO:      return "cash-stack";
            case METODO_TARJETA:       return "credit-card";
            case METODO_TRANSFERENCIA: return "arrow-left-right";
            case METODO_DEPOSITO:      return "bank";
            default:                   return "cash";
        }
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdPago() {
        return idPago;
    }

    public void setIdPago(int idPago) {
        this.idPago = idPago;
    }

    public int getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(int idReserva) {
        this.idReserva = idReserva;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
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

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    @Override
    public String toString() {
        return tipo + " Q " + monto + " (" + metodo + ")";
    }
}
