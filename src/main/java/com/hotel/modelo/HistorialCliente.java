package com.hotel.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Perfil economico acumulado de un cliente a lo largo del tiempo.
 *
 * Responde al requerimiento de "historial de clientes con datos
 * economicos, visitas y temporadas". Corresponde a la vista
 * 'vw_historial_cliente' y es de solo lectura.
 *
 * Sirve para que recepcion reconozca de inmediato a un huesped
 * importante: cuantas veces ha venido, cuanto ha gastado y si quedo
 * debiendo algo la ultima vez.
 *
 * @author Ronald Perea
 */
public class HistorialCliente implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idCliente;
    private String cliente;
    private String documento;
    private String telefono;
    private String correo;
    private int totalReservas;
    private int visitasCompletadas;
    private int cancelaciones;
    private int nochesTotales;
    private BigDecimal consumoHistorico;
    private BigDecimal saldoActual;
    private LocalDate ultimaVisita;

    public HistorialCliente() {
        this.consumoHistorico = BigDecimal.ZERO;
        this.saldoActual = BigDecimal.ZERO;
    }

    /**
     * Gasto promedio por noche hospedada. Es un indicador mas util que
     * el total para comparar clientes, porque no premia unicamente a
     * quien se queda mas tiempo.
     *
     * @return promedio por noche, o cero si nunca se ha hospedado
     */
    public BigDecimal getPromedioPorNoche() {
        if (nochesTotales <= 0) {
            return BigDecimal.ZERO;
        }
        return consumoHistorico.divide(
                BigDecimal.valueOf(nochesTotales), 2, RoundingMode.HALF_UP);
    }

    /**
     * Clasifica al cliente segun lo que ha dejado en el hotel.
     *
     * Los cortes son una politica comercial del hotel, no un calculo
     * universal. Se dejan aqui, en un solo lugar, para que cambiarlos
     * sea modificar tres numeros y no buscar condiciones repartidas por
     * toda la aplicacion.
     *
     * @return categoria del cliente
     */
    public String getCategoria() {
        double total = consumoHistorico.doubleValue();
        if (total >= 20000) {
            return "Diamante";
        } else if (total >= 10000) {
            return "Oro";
        } else if (total >= 3000) {
            return "Plata";
        } else if (totalReservas > 0) {
            return "Regular";
        }
        return "Nuevo";
    }

    /**
     * Color de Bootstrap asociado a la categoria del cliente.
     *
     * @return sufijo de color de Bootstrap
     */
    public String getColorCategoria() {
        switch (getCategoria()) {
            case "Diamante": return "info";
            case "Oro":      return "warning";
            case "Plata":    return "secondary";
            case "Regular":  return "primary";
            default:         return "light";
        }
    }

    /**
     * Indica si el cliente es recurrente, es decir, si ya se hospedo
     * mas de una vez.
     *
     * @return true si tiene mas de una visita completada
     */
    public boolean isFrecuente() {
        return visitasCompletadas > 1;
    }

    /**
     * Indica si el cliente tiene cuentas pendientes de cobro.
     *
     * @return true si su saldo es mayor que cero
     */
    public boolean isConDeuda() {
        return saldoActual.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Porcentaje de reservas que el cliente termino cancelando. Un
     * porcentaje alto es una senal para pedirle anticipo antes de
     * apartarle una habitacion.
     *
     * @return porcentaje entre 0 y 100
     */
    public int getPorcentajeCancelacion() {
        if (totalReservas <= 0) {
            return 0;
        }
        return (int) Math.round((cancelaciones * 100.0) / totalReservas);
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

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

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public int getTotalReservas() {
        return totalReservas;
    }

    public void setTotalReservas(int totalReservas) {
        this.totalReservas = totalReservas;
    }

    public int getVisitasCompletadas() {
        return visitasCompletadas;
    }

    public void setVisitasCompletadas(int visitasCompletadas) {
        this.visitasCompletadas = visitasCompletadas;
    }

    public int getCancelaciones() {
        return cancelaciones;
    }

    public void setCancelaciones(int cancelaciones) {
        this.cancelaciones = cancelaciones;
    }

    public int getNochesTotales() {
        return nochesTotales;
    }

    public void setNochesTotales(int nochesTotales) {
        this.nochesTotales = nochesTotales;
    }

    public BigDecimal getConsumoHistorico() {
        return consumoHistorico;
    }

    public void setConsumoHistorico(BigDecimal consumoHistorico) {
        this.consumoHistorico = consumoHistorico;
    }

    public BigDecimal getSaldoActual() {
        return saldoActual;
    }

    public void setSaldoActual(BigDecimal saldoActual) {
        this.saldoActual = saldoActual;
    }

    public LocalDate getUltimaVisita() {
        return ultimaVisita;
    }

    public void setUltimaVisita(LocalDate ultimaVisita) {
        this.ultimaVisita = ultimaVisita;
    }

    @Override
    public String toString() {
        return cliente + " - " + getCategoria();
    }
}
