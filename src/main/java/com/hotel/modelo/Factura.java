package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento de cobro emitido al cliente.
 *
 * El campo 'tipo' es lo que resuelve el requerimiento de facturacion
 * flexible del enunciado. Una misma reserva puede generar varias
 * facturas distintas segun lo que el cliente pida:
 *
 *   COMPLETA  -> hospedaje mas todos los consumos, en un solo documento.
 *   HOSPEDAJE -> unicamente las noches de habitacion. Se usa cuando la
 *                empresa paga el alojamiento y el huesped sus extras.
 *   CONSUMOS  -> unicamente los gastos pendientes de facturar.
 *   OCUPANTE  -> solo los consumos atribuidos a un huesped concreto,
 *                para dividir la cuenta entre varias personas.
 *
 * Una factura nunca se borra: si tiene un error se ANULA, y los cargos
 * que incluia quedan liberados para volver a facturarse. Asi la
 * numeracion no deja huecos.
 *
 * @author Ronald Perea
 */
public class Factura implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TIPO_COMPLETA = "COMPLETA";
    public static final String TIPO_HOSPEDAJE = "HOSPEDAJE";
    public static final String TIPO_CONSUMOS = "CONSUMOS";
    public static final String TIPO_OCUPANTE = "OCUPANTE";

    /** Tipos validos, en el orden en que se muestran en el formulario. */
    public static final String[] TIPOS = {
        TIPO_COMPLETA, TIPO_HOSPEDAJE, TIPO_CONSUMOS, TIPO_OCUPANTE
    };

    public static final String ESTADO_EMITIDA = "EMITIDA";
    public static final String ESTADO_ANULADA = "ANULADA";

    private int idFactura;
    private String serie;
    private int numero;
    private int idReserva;
    private int idCliente;
    private Integer idOcupante;
    private String nombreReceptor;
    private String nitReceptor;
    private String tipo;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private String estado;
    private String observaciones;
    private LocalDateTime fechaEmision;
    private int idUsuario;

    /** Renglones del documento. Se cargan al abrir el detalle. */
    private List<DetalleFactura> detalles = new ArrayList<>();

    /* Datos traidos por JOIN. */
    private String codigoReserva;
    private String numeroHabitacion;
    private String nombreOcupante;
    private String nombreUsuario;

    public Factura() {
        this.serie = "A";
        this.tipo = TIPO_COMPLETA;
        this.estado = ESTADO_EMITIDA;
        this.nitReceptor = "C/F";
        this.subtotal = BigDecimal.ZERO;
        this.descuento = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
    }

    /**
     * Numero de documento tal como se imprime: serie y correlativo
     * rellenado con ceros, por ejemplo "A-000012".
     *
     * @return numero de factura formateado
     */
    public String getNumeroCompleto() {
        return serie + "-" + String.format("%06d", numero);
    }

    /**
     * Indica si la factura sigue siendo valida.
     *
     * @return true si no ha sido anulada
     */
    public boolean isVigente() {
        return ESTADO_EMITIDA.equals(estado);
    }

    /**
     * Explica en palabras que incluye la factura, para mostrarlo bajo el
     * tipo en la pantalla de consulta.
     *
     * @return descripcion del alcance del documento
     */
    public String getDescripcionTipo() {
        if (tipo == null) {
            return "";
        }
        switch (tipo) {
            case TIPO_COMPLETA:
                return "Hospedaje y todos los consumos";
            case TIPO_HOSPEDAJE:
                return "Unicamente las noches de habitacion";
            case TIPO_CONSUMOS:
                return "Unicamente los consumos y gastos";
            case TIPO_OCUPANTE:
                return "Consumos de un huesped en particular";
            default:
                return "";
        }
    }

    /**
     * Agrega un renglon a la factura.
     *
     * @param detalle renglon a incluir
     */
    public void agregarDetalle(DetalleFactura detalle) {
        this.detalles.add(detalle);
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(int idFactura) {
        this.idFactura = idFactura;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(int idReserva) {
        this.idReserva = idReserva;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public Integer getIdOcupante() {
        return idOcupante;
    }

    public void setIdOcupante(Integer idOcupante) {
        this.idOcupante = idOcupante;
    }

    public String getNombreReceptor() {
        return nombreReceptor;
    }

    public void setNombreReceptor(String nombreReceptor) {
        this.nombreReceptor = nombreReceptor;
    }

    public String getNitReceptor() {
        return nitReceptor;
    }

    public void setNitReceptor(String nitReceptor) {
        this.nitReceptor = nitReceptor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public List<DetalleFactura> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleFactura> detalles) {
        this.detalles = detalles;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public void setCodigoReserva(String codigoReserva) {
        this.codigoReserva = codigoReserva;
    }

    public String getNumeroHabitacion() {
        return numeroHabitacion;
    }

    public void setNumeroHabitacion(String numeroHabitacion) {
        this.numeroHabitacion = numeroHabitacion;
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

    @Override
    public String toString() {
        return "Factura " + getNumeroCompleto() + " - Q " + total;
    }
}
