package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Resumen economico de una reserva: lo que debe, lo que pago y lo que
 * falta.
 *
 * Esta clase no corresponde a una tabla sino a la vista
 * 'vw_estado_cuenta' de la base de datos. Es un objeto de solo lectura:
 * se consulta para mostrar saldos en el check-out y en la facturacion,
 * pero nunca se inserta ni se actualiza.
 *
 * La formula es siempre la misma:
 *
 *   saldo = (noches x tarifa) + consumos - pagos
 *
 * @author Ronald Perea
 */
public class EstadoCuenta implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idReserva;
    private String codigo;
    private int idCliente;
    private String cliente;
    private String habitacion;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private int noches;
    private BigDecimal tarifaNoche;
    private String estado;
    private BigDecimal totalHospedaje;
    private BigDecimal totalConsumos;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;

    public EstadoCuenta() {
        this.totalHospedaje = BigDecimal.ZERO;
        this.totalConsumos = BigDecimal.ZERO;
        this.totalPagado = BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
    }

    /**
     * Suma de todo lo que se le cobra al huesped, antes de descontar sus
     * pagos.
     *
     * @return hospedaje mas consumos
     */
    public BigDecimal getTotalCargado() {
        return totalHospedaje.add(totalConsumos).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Indica si la cuenta ya esta saldada y el huesped puede retirarse
     * sin pagar nada mas.
     *
     * @return true si el saldo pendiente es cero o menor
     */
    public boolean isSaldada() {
        return saldoPendiente.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Indica si el hotel le debe dinero al cliente, situacion que se da
     * cuando el anticipo fue mayor que el consumo final.
     *
     * @return true si el saldo es negativo
     */
    public boolean isConSaldoAFavor() {
        return saldoPendiente.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Monto que el hotel debe devolver al cliente.
     *
     * @return valor absoluto del saldo a favor, o cero si no hay
     */
    public BigDecimal getSaldoAFavor() {
        return isConSaldoAFavor() ? saldoPendiente.negate() : BigDecimal.ZERO;
    }

    /**
     * Porcentaje de la cuenta que ya esta cubierto por los pagos.
     * Se usa para la barra de progreso de la pantalla de cobros.
     *
     * @return porcentaje entre 0 y 100
     */
    public int getPorcentajePagado() {
        BigDecimal cargado = getTotalCargado();
        if (cargado.compareTo(BigDecimal.ZERO) <= 0) {
            return 100;
        }
        BigDecimal porcentaje = totalPagado
                .multiply(BigDecimal.valueOf(100))
                .divide(cargado, 0, RoundingMode.HALF_UP);
        int valor = porcentaje.intValue();
        if (valor < 0) {
            return 0;
        }
        return Math.min(valor, 100);
    }

    /**
     * Color de Bootstrap para la barra de progreso y las etiquetas de
     * saldo: verde si esta pagada, amarillo si va a medias, rojo si no
     * ha pagado casi nada.
     *
     * @return sufijo de color de Bootstrap
     */
    public String getColorSaldo() {
        if (isSaldada()) {
            return "success";
        }
        return getPorcentajePagado() >= 50 ? "warning" : "danger";
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

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getHabitacion() {
        return habitacion;
    }

    public void setHabitacion(String habitacion) {
        this.habitacion = habitacion;
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

    public int getNoches() {
        return noches;
    }

    public void setNoches(int noches) {
        this.noches = noches;
    }

    public BigDecimal getTarifaNoche() {
        return tarifaNoche;
    }

    public void setTarifaNoche(BigDecimal tarifaNoche) {
        this.tarifaNoche = tarifaNoche;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotalHospedaje() {
        return totalHospedaje;
    }

    public void setTotalHospedaje(BigDecimal totalHospedaje) {
        this.totalHospedaje = totalHospedaje;
    }

    public BigDecimal getTotalConsumos() {
        return totalConsumos;
    }

    public void setTotalConsumos(BigDecimal totalConsumos) {
        this.totalConsumos = totalConsumos;
    }

    public BigDecimal getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(BigDecimal totalPagado) {
        this.totalPagado = totalPagado;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    @Override
    public String toString() {
        return codigo + " - saldo Q " + saldoPendiente;
    }
}
