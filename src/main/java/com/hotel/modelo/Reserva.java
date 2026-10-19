package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Reserva de una habitacion para un cliente en un rango de fechas.
 *
 * Es la entidad central del sistema: de ella cuelgan los ocupantes, los
 * consumos, los pagos y las facturas.
 *
 * Detalle importante sobre el precio: 'tarifaNoche' se copia dentro de la
 * reserva en el momento de crearla, en lugar de calcularse cada vez que
 * se consulta. Esto se llama congelar el precio. Si el hotel sube las
 * tarifas manana, o cambia el factor de la temporada, la reserva ya hecha
 * conserva el precio que se le prometio al cliente.
 *
 * Ciclo de vida de los estados:
 *
 *   PENDIENTE  --confirmar-->  CONFIRMADA  --check-in-->  EN_CASA
 *                                                            |
 *                                                       check-out
 *                                                            v
 *                                                       FINALIZADA
 *
 *   PENDIENTE o CONFIRMADA  --cancelar-->  CANCELADA
 *   CONFIRMADA que nunca llego  -------->  NO_SHOW
 *
 * @author Ronald Perea
 */
public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Creada pero aun sin anticipo ni confirmacion. */
    public static final String ESTADO_PENDIENTE = "PENDIENTE";

    /** Confirmada: la habitacion queda apartada para esas fechas. */
    public static final String ESTADO_CONFIRMADA = "CONFIRMADA";

    /** El huesped ya hizo check-in y esta hospedado. */
    public static final String ESTADO_EN_CASA = "EN_CASA";

    /** El huesped ya hizo check-out y la cuenta quedo cerrada. */
    public static final String ESTADO_FINALIZADA = "FINALIZADA";

    /** Anulada antes de la llegada. */
    public static final String ESTADO_CANCELADA = "CANCELADA";

    /** El huesped nunca se presento en la fecha acordada. */
    public static final String ESTADO_NO_SHOW = "NO_SHOW";

    private int idReserva;
    private String codigo;
    private int idCliente;
    private int idHabitacion;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private int adultos;
    private int ninos;
    private BigDecimal tarifaNoche;
    private int noches;
    private String estado;
    private LocalDateTime fechaCheckin;
    private LocalDateTime fechaCheckout;
    private String observaciones;
    private int idUsuario;
    private LocalDateTime fechaCreacion;

    /*
     * Datos traidos por JOIN para que las vistas no tengan que hacer
     * consultas adicionales. No existen como columnas de la tabla reserva.
     */
    private String nombreCliente;
    private String documentoCliente;
    private String telefonoCliente;
    private String numeroHabitacion;
    private String nombreTipo;
    private String nombreUsuario;

    public Reserva() {
        this.estado = ESTADO_PENDIENTE;
        this.adultos = 1;
        this.ninos = 0;
        this.tarifaNoche = BigDecimal.ZERO;
    }

    // -----------------------------------------------------------------
    // Reglas de negocio
    // -----------------------------------------------------------------

    /**
     * Calcula cuantas noches abarca el rango de fechas.
     *
     * En hoteleria se cobran noches, no dias: quien entra el lunes y sale
     * el miercoles duerme dos noches, aunque toque tres dias del
     * calendario. Por eso se restan las fechas directamente y no se suma
     * uno al resultado.
     *
     * @return cantidad de noches, o cero si las fechas no son validas
     */
    public int calcularNoches() {
        if (fechaEntrada == null || fechaSalida == null) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
        return dias > 0 ? (int) dias : 0;
    }

    /**
     * Total que se cobrara por el hospedaje, sin contar los consumos.
     *
     * @return noches multiplicadas por la tarifa congelada
     */
    public BigDecimal getTotalHospedaje() {
        if (tarifaNoche == null) {
            return BigDecimal.ZERO;
        }
        return tarifaNoche.multiply(BigDecimal.valueOf(noches))
                          .setScale(2, RoundingMode.HALF_UP);
    }

    /** @return cantidad total de personas que ocuparan la habitacion */
    public int getTotalHuespedes() {
        return adultos + ninos;
    }

    /**
     * Indica si la reserva admite check-in. Solo se le puede dar entrada
     * a una reserva confirmada o pendiente; una cancelada o una que ya
     * esta en casa, no.
     *
     * @return true si el boton de check-in debe estar habilitado
     */
    public boolean isCheckinHabilitado() {
        return ESTADO_CONFIRMADA.equals(estado) || ESTADO_PENDIENTE.equals(estado);
    }

    /**
     * Indica si la reserva admite check-out, lo cual exige que el
     * huesped este efectivamente hospedado.
     *
     * @return true si el estado es EN_CASA
     */
    public boolean isCheckoutHabilitado() {
        return ESTADO_EN_CASA.equals(estado);
    }

    /**
     * Indica si la reserva todavia se puede cancelar. Una vez que el
     * huesped entro, ya no se cancela: se cobra lo consumido.
     *
     * @return true si aun no ha ocurrido el check-in
     */
    public boolean isCancelable() {
        return ESTADO_PENDIENTE.equals(estado) || ESTADO_CONFIRMADA.equals(estado);
    }

    /**
     * Indica si a esta reserva todavia se le pueden anotar consumos.
     * Solo tiene sentido mientras el huesped esta en el hotel.
     *
     * @return true si el estado es EN_CASA
     */
    public boolean isAdmiteConsumos() {
        return ESTADO_EN_CASA.equals(estado);
    }

    /**
     * Indica si la reserva esta activa, es decir, si ocupa la habitacion
     * y debe tomarse en cuenta al revisar disponibilidad.
     *
     * @return true si no esta cancelada, finalizada ni marcada como no-show
     */
    public boolean isVigente() {
        return ESTADO_PENDIENTE.equals(estado)
            || ESTADO_CONFIRMADA.equals(estado)
            || ESTADO_EN_CASA.equals(estado);
    }

    /**
     * Indica si la llegada esta prevista para hoy, para destacarla en el
     * panel de recepcion.
     *
     * @return true si la fecha de entrada es la de hoy
     */
    public boolean isLlegaHoy() {
        return fechaEntrada != null && fechaEntrada.isEqual(LocalDate.now());
    }

    /**
     * Indica si la salida esta prevista para hoy.
     *
     * @return true si la fecha de salida es la de hoy
     */
    public boolean isSaleHoy() {
        return fechaSalida != null && fechaSalida.isEqual(LocalDate.now());
    }

    /**
     * Indica si el huesped se paso de la fecha de salida sin hacer
     * check-out, situacion que recepcion debe atender.
     *
     * @return true si sigue en casa despues de su fecha de salida
     */
    public boolean isVencida() {
        return ESTADO_EN_CASA.equals(estado)
            && fechaSalida != null
            && fechaSalida.isBefore(LocalDate.now());
    }

    /**
     * Color de Bootstrap que corresponde al estado, para las etiquetas
     * de las tablas.
     *
     * @return sufijo de color de Bootstrap
     */
    public String getColorEstado() {
        if (estado == null) {
            return "secondary";
        }
        switch (estado) {
            case ESTADO_PENDIENTE:  return "warning";
            case ESTADO_CONFIRMADA: return "info";
            case ESTADO_EN_CASA:    return "success";
            case ESTADO_FINALIZADA: return "secondary";
            case ESTADO_CANCELADA:  return "danger";
            case ESTADO_NO_SHOW:    return "dark";
            default:                return "secondary";
        }
    }

    /**
     * Texto legible del estado, con espacios en lugar de guion bajo.
     *
     * @return estado listo para mostrar en pantalla
     */
    public String getEstadoLegible() {
        if (estado == null) {
            return "";
        }
        switch (estado) {
            case ESTADO_EN_CASA: return "En casa";
            case ESTADO_NO_SHOW: return "No se presento";
            default:
                return estado.charAt(0) + estado.substring(1).toLowerCase();
        }
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(int idReserva) {
        this.idReserva = idReserva;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public int getIdHabitacion() {
        return idHabitacion;
    }

    public void setIdHabitacion(int idHabitacion) {
        this.idHabitacion = idHabitacion;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public int getAdultos() {
        return adultos;
    }

    public void setAdultos(int adultos) {
        this.adultos = adultos;
    }

    public int getNinos() {
        return ninos;
    }

    public void setNinos(int ninos) {
        this.ninos = ninos;
    }

    public BigDecimal getTarifaNoche() {
        return tarifaNoche;
    }

    public void setTarifaNoche(BigDecimal tarifaNoche) {
        this.tarifaNoche = tarifaNoche;
    }

    public int getNoches() {
        return noches;
    }

    public void setNoches(int noches) {
        this.noches = noches;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCheckin() {
        return fechaCheckin;
    }

    public void setFechaCheckin(LocalDateTime fechaCheckin) {
        this.fechaCheckin = fechaCheckin;
    }

    public LocalDateTime getFechaCheckout() {
        return fechaCheckout;
    }

    public void setFechaCheckout(LocalDateTime fechaCheckout) {
        this.fechaCheckout = fechaCheckout;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getDocumentoCliente() {
        return documentoCliente;
    }

    public void setDocumentoCliente(String documentoCliente) {
        this.documentoCliente = documentoCliente;
    }

    public String getTelefonoCliente() {
        return telefonoCliente;
    }

    public void setTelefonoCliente(String telefonoCliente) {
        this.telefonoCliente = telefonoCliente;
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
        return codigo + " - " + nombreCliente + " (hab. " + numeroHabitacion + ")";
    }
}
