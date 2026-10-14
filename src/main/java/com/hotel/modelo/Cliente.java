package com.hotel.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Persona o empresa titular de una reserva.
 *
 * El cliente es quien contrata y a quien se le factura. No siempre
 * coincide con el {@link Ocupante}: una empresa puede reservar para un
 * empleado, o un padre de familia puede pagar la habitacion de sus hijos.
 *
 * @author Ronald Perea
 */
public class Cliente implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DOC_DPI = "DPI";
    public static final String DOC_PASAPORTE = "PASAPORTE";
    public static final String DOC_NIT = "NIT";
    public static final String DOC_OTRO = "OTRO";

    private int idCliente;
    private String nombre;
    private String apellido;
    private String tipoDocumento;
    private String documento;
    private String nit;
    private String telefono;
    private String correo;
    private String direccion;
    private String pais;
    private String notas;
    private boolean activo;
    private LocalDateTime fechaRegistro;

    public Cliente() {
        this.tipoDocumento = DOC_DPI;
        this.nit = "C/F";
        this.pais = "Guatemala";
        this.activo = true;
    }

    /**
     * Constructor completo, usado al leer un registro de la base.
     *
     * @param idCliente     identificador en la base de datos
     * @param nombre        nombres de la persona
     * @param apellido      apellidos de la persona
     * @param tipoDocumento uno de los DOC_* de esta clase
     * @param documento     numero del documento de identificacion
     * @param nit           NIT para efectos de facturacion
     * @param telefono      telefono de contacto
     * @param correo        correo electronico
     * @param direccion     direccion de residencia
     * @param pais          pais de procedencia
     * @param notas         observaciones que ayudan a atenderlo mejor
     * @param activo        si sigue vigente en el sistema
     */
    public Cliente(int idCliente, String nombre, String apellido,
                   String tipoDocumento, String documento, String nit,
                   String telefono, String correo, String direccion,
                   String pais, String notas, boolean activo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.apellido = apellido;
        this.tipoDocumento = tipoDocumento;
        this.documento = documento;
        this.nit = nit;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.pais = pais;
        this.notas = notas;
        this.activo = activo;
    }

    /**
     * Nombre y apellido unidos, que es como se muestra el cliente en
     * tablas, facturas y buscadores.
     *
     * @return nombre completo del cliente
     */
    public String getNombreCompleto() {
        return (nombre == null ? "" : nombre) + " "
             + (apellido == null ? "" : apellido);
    }

    /**
     * Devuelve las iniciales para el avatar de las tarjetas.
     *
     * @return dos letras en mayuscula
     */
    public String getIniciales() {
        String inicialNombre = (nombre != null && !nombre.isBlank())
                ? nombre.substring(0, 1) : "";
        String inicialApellido = (apellido != null && !apellido.isBlank())
                ? apellido.substring(0, 1) : "";
        String iniciales = (inicialNombre + inicialApellido).toUpperCase();
        return iniciales.isEmpty() ? "?" : iniciales;
    }

    /**
     * Indica si el cliente es extranjero, dato util para recepcion
     * (documentacion migratoria, idioma de atencion).
     *
     * @return true si su pais no es Guatemala
     */
    public boolean isExtranjero() {
        return pais != null && !"Guatemala".equalsIgnoreCase(pais.trim());
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return getNombreCompleto() + " (" + documento + ")";
    }
}
