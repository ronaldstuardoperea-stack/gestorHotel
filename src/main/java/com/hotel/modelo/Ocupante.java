package com.hotel.modelo;

import java.io.Serializable;

/**
 * Persona que efectivamente se hospeda en la habitacion.
 *
 * Se registran por dos razones concretas:
 *
 *  1. Control: el hotel debe saber quien durmio en cada habitacion.
 *  2. Facturacion separada: si un consumo se le atribuye a un ocupante,
 *     se le puede emitir una factura individual por sus gastos, aunque
 *     el hospedaje lo pague otra persona.
 *
 * El ocupante marcado como titular es quien firma el registro de entrada.
 *
 * @author Ronald Perea
 */
public class Ocupante implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idOcupante;
    private int idReserva;
    private String nombreCompleto;
    private String documento;
    private Integer edad;
    private boolean esTitular;

    public Ocupante() {
    }

    /**
     * Constructor completo, usado al leer un registro de la base.
     *
     * @param idOcupante     identificador en la base de datos
     * @param idReserva      reserva a la que pertenece
     * @param nombreCompleto nombre de la persona
     * @param documento      documento de identificacion, puede ser nulo
     * @param edad           edad en anos, puede ser nula
     * @param esTitular      si es quien firma el registro de entrada
     */
    public Ocupante(int idOcupante, int idReserva, String nombreCompleto,
                    String documento, Integer edad, boolean esTitular) {
        this.idOcupante = idOcupante;
        this.idReserva = idReserva;
        this.nombreCompleto = nombreCompleto;
        this.documento = documento;
        this.edad = edad;
        this.esTitular = esTitular;
    }

    /**
     * Indica si el ocupante es menor de edad, dato que recepcion usa
     * para aplicar tarifas de nino y politicas de acompanamiento.
     *
     * @return true si tiene edad registrada y es menor de 18 anos
     */
    public boolean isMenorDeEdad() {
        return edad != null && edad < 18;
    }

    /**
     * Devuelve las iniciales para el avatar de la lista de ocupantes.
     *
     * @return una o dos letras en mayuscula
     */
    public String getIniciales() {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "?";
        }
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].charAt(0) + "" + partes[1].charAt(0)).toUpperCase();
    }

    // -----------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------

    public int getIdOcupante() {
        return idOcupante;
    }

    public void setIdOcupante(int idOcupante) {
        this.idOcupante = idOcupante;
    }

    public int getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(int idReserva) {
        this.idReserva = idReserva;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public boolean isEsTitular() {
        return esTitular;
    }

    public void setEsTitular(boolean esTitular) {
        this.esTitular = esTitular;
    }

    @Override
    public String toString() {
        return nombreCompleto;
    }
}
