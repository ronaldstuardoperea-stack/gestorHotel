package com.hotel.servicio;

import com.hotel.dao.CargoDAO;
import com.hotel.dao.ClienteDAO;
import com.hotel.dao.FacturaDAO;
import com.hotel.dao.OcupanteDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Cargo;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.DetalleFactura;
import com.hotel.modelo.Factura;
import com.hotel.modelo.Ocupante;
import com.hotel.modelo.Reserva;
import com.hotel.util.ValidacionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de negocio de la facturacion.
 *
 * Su trabajo es armar el contenido de una factura segun el tipo que pida
 * el cliente, y garantizar que nada se cobre dos veces.
 *
 * Hay dos mecanismos distintos para evitar el doble cobro, uno para cada
 * concepto:
 *
 *  - Los CONSUMOS llevan una columna id_factura. Al facturarse quedan
 *    marcados y ya no vuelven a aparecer entre los pendientes.
 *
 *  - El HOSPEDAJE no tiene esa marca, porque no es una fila sino un
 *    calculo (noches por tarifa). Para el se revisa si la reserva ya
 *    tiene una factura vigente que lo incluya.
 *
 * @author Ronald Perea
 */
public class ServicioFacturacion {

    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final CargoDAO cargoDAO = new CargoDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final OcupanteDAO ocupanteDAO = new OcupanteDAO();

    /**
     * Arma y emite una factura.
     *
     * @param idReserva   reserva a facturar
     * @param tipo        uno de los TIPO_* de la clase Factura
     * @param idOcupante  huesped a facturar; solo se usa con TIPO_OCUPANTE
     * @param descuento   descuento a aplicar sobre el subtotal
     * @param nitReceptor NIT que se imprime; si viene vacio se usa el del cliente
     * @param nombreReceptor nombre que se imprime; si viene vacio se usa el del cliente
     * @param idUsuario   empleado que emite el documento
     * @return el identificador de la factura creada
     * @throws SQLException        si falla la emision
     * @throws ValidacionException si no hay nada que facturar o ya se facturo
     */
    public int emitirFactura(int idReserva, String tipo, Integer idOcupante,
                             BigDecimal descuento, String nitReceptor,
                             String nombreReceptor, int idUsuario) throws SQLException {

        Reserva reserva = reservaDAO.buscarPorId(idReserva);
        if (reserva == null) {
            throw new ValidacionException("No se encontro la reserva a facturar.");
        }
        if (Reserva.ESTADO_CANCELADA.equals(reserva.getEstado())) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo()
                    + " esta cancelada y no se puede facturar.");
        }

        Cliente cliente = clienteDAO.buscarPorId(reserva.getIdCliente());
        if (cliente == null) {
            throw new ValidacionException("No se encontro el cliente de la reserva.");
        }

        Factura factura = new Factura();
        factura.setIdReserva(idReserva);
        factura.setIdCliente(cliente.getIdCliente());
        factura.setTipo(tipo);
        factura.setIdUsuario(idUsuario);

        // Si no se indica otro receptor, se factura a nombre del titular.
        factura.setNombreReceptor(
                (nombreReceptor == null || nombreReceptor.isBlank())
                        ? cliente.getNombreCompleto() : nombreReceptor);
        factura.setNitReceptor(
                (nitReceptor == null || nitReceptor.isBlank())
                        ? cliente.getNit() : nitReceptor);

        List<Integer> idsCargos = new ArrayList<>();

        switch (tipo) {
            case Factura.TIPO_COMPLETA:
                verificarHospedajeNoFacturado(idReserva, reserva.getCodigo());
                agregarHospedaje(factura, reserva);
                agregarConsumos(factura, idsCargos,
                        cargoDAO.listarPendientesDeFacturar(idReserva));
                break;

            case Factura.TIPO_HOSPEDAJE:
                verificarHospedajeNoFacturado(idReserva, reserva.getCodigo());
                agregarHospedaje(factura, reserva);
                break;

            case Factura.TIPO_CONSUMOS:
                agregarConsumos(factura, idsCargos,
                        cargoDAO.listarPendientesDeFacturar(idReserva));
                break;

            case Factura.TIPO_OCUPANTE:
                if (idOcupante == null) {
                    throw new ValidacionException(
                            "Debe seleccionar el huesped cuyos consumos se van a facturar.");
                }
                Ocupante ocupante = ocupanteDAO.buscarPorId(idOcupante);
                if (ocupante == null || ocupante.getIdReserva() != idReserva) {
                    throw new ValidacionException(
                            "El huesped seleccionado no pertenece a esta reserva.");
                }
                factura.setIdOcupante(idOcupante);
                agregarConsumos(factura, idsCargos,
                        cargoDAO.listarPendientesPorOcupante(idReserva, idOcupante));
                break;

            default:
                throw new ValidacionException("El tipo de factura '" + tipo + "' no es valido.");
        }

        if (factura.getDetalles().isEmpty()) {
            throw new ValidacionException(
                    "No hay nada pendiente de facturar para esta reserva "
                    + "con el tipo de documento seleccionado.");
        }

        calcularTotales(factura, descuento);
        return facturaDAO.emitir(factura, idsCargos);
    }

    /**
     * Anula una factura y libera los consumos que incluia.
     *
     * @param idFactura factura a anular
     * @param motivo    razon de la anulacion
     * @throws SQLException        si falla la operacion
     * @throws ValidacionException si la factura no existe o ya estaba anulada
     */
    public void anularFactura(int idFactura, String motivo) throws SQLException {
        Factura factura = facturaDAO.buscarPorId(idFactura);
        if (factura == null) {
            throw new ValidacionException("No se encontro la factura indicada.");
        }
        if (!factura.isVigente()) {
            throw new ValidacionException(
                    "La factura " + factura.getNumeroCompleto() + " ya esta anulada.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ValidacionException("Debe indicar el motivo de la anulacion.");
        }
        facturaDAO.anular(idFactura, motivo.trim());
    }

    /**
     * Calcula cuanto sumaria una factura de cierto tipo, sin emitirla.
     *
     * Sirve para mostrarle al cajero el total antes de confirmar, para
     * que pueda corregir si eligio el tipo equivocado.
     *
     * @param idReserva  reserva a cotizar
     * @param tipo       tipo de documento
     * @param idOcupante huesped, solo para TIPO_OCUPANTE
     * @return total que tendria la factura, sin descuento
     * @throws SQLException si falla la consulta
     */
    public BigDecimal calcularTotalPrevisto(int idReserva, String tipo, Integer idOcupante)
            throws SQLException {
        BigDecimal total = BigDecimal.ZERO;

        if (Factura.TIPO_COMPLETA.equals(tipo) || Factura.TIPO_HOSPEDAJE.equals(tipo)) {
            Reserva reserva = reservaDAO.buscarPorId(idReserva);
            if (reserva != null) {
                total = total.add(reserva.getTotalHospedaje());
            }
        }
        if (Factura.TIPO_COMPLETA.equals(tipo) || Factura.TIPO_CONSUMOS.equals(tipo)) {
            total = total.add(cargoDAO.sumarPendientes(idReserva));
        }
        if (Factura.TIPO_OCUPANTE.equals(tipo) && idOcupante != null) {
            for (Cargo cargo : cargoDAO.listarPendientesPorOcupante(idReserva, idOcupante)) {
                total = total.add(cargo.getMonto());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // -----------------------------------------------------------------
    // Armado del detalle
    // -----------------------------------------------------------------

    /**
     * Agrega a la factura el renglon del hospedaje.
     *
     * Se imprime como una sola linea con la cantidad de noches y la
     * tarifa, que es como lo espera ver el huesped.
     *
     * @param factura factura en construccion
     * @param reserva reserva de la que se toman las noches y la tarifa
     */
    private void agregarHospedaje(Factura factura, Reserva reserva) {
        String descripcion = "Hospedaje habitacion " + reserva.getNumeroHabitacion()
                + " (" + reserva.getNombreTipo() + ") del "
                + reserva.getFechaEntrada() + " al " + reserva.getFechaSalida();

        factura.agregarDetalle(new DetalleFactura(
                descripcion,
                BigDecimal.valueOf(reserva.getNoches()),
                reserva.getTarifaNoche()));
    }

    /**
     * Agrega a la factura un renglon por cada consumo, y guarda sus
     * identificadores para poder marcarlos como facturados.
     *
     * @param factura   factura en construccion
     * @param idsCargos lista donde se acumulan los consumos incluidos
     * @param cargos    consumos a facturar
     */
    private void agregarConsumos(Factura factura, List<Integer> idsCargos,
                                 List<Cargo> cargos) {
        for (Cargo cargo : cargos) {
            String descripcion = cargo.getConcepto();

            // Si el consumo pertenece a un huesped concreto, se indica en
            // la descripcion. Ayuda cuando varias personas comparten la
            // habitacion y cada una revisa su parte de la cuenta.
            if (cargo.getNombreOcupante() != null) {
                descripcion += " - " + cargo.getNombreOcupante();
            }

            factura.agregarDetalle(new DetalleFactura(
                    descripcion,
                    cargo.getCantidad(),
                    cargo.getPrecioUnitario()));

            idsCargos.add(cargo.getIdCargo());
        }
    }

    /**
     * Suma los renglones y aplica el descuento.
     *
     * @param factura   factura con sus detalles ya armados
     * @param descuento descuento solicitado; puede ser null
     * @throws ValidacionException si el descuento es negativo o supera el subtotal
     */
    private void calcularTotales(Factura factura, BigDecimal descuento) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleFactura detalle : factura.getDetalles()) {
            subtotal = subtotal.add(detalle.getImporte());
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal rebaja = (descuento == null) ? BigDecimal.ZERO : descuento;
        if (rebaja.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El descuento no puede ser negativo.");
        }
        if (rebaja.compareTo(subtotal) > 0) {
            throw new ValidacionException(
                    "El descuento (Q " + rebaja + ") no puede ser mayor que el "
                    + "subtotal de la factura (Q " + subtotal + ").");
        }

        factura.setSubtotal(subtotal);
        factura.setDescuento(rebaja.setScale(2, RoundingMode.HALF_UP));
        factura.setTotal(subtotal.subtract(rebaja).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Comprueba que el hospedaje de la reserva no se haya facturado ya.
     *
     * Recorre las facturas vigentes de la reserva buscando alguna de
     * tipo COMPLETA u HOSPEDAJE, que son las dos que incluyen las
     * noches. Una factura anulada no cuenta, porque su cobro quedo sin
     * efecto.
     *
     * @param idReserva     reserva a revisar
     * @param codigoReserva codigo visible, para el mensaje de error
     * @throws SQLException        si falla la consulta
     * @throws ValidacionException si el hospedaje ya fue facturado
     */
    private void verificarHospedajeNoFacturado(int idReserva, String codigoReserva)
            throws SQLException {
        for (Factura emitida : facturaDAO.listarPorReserva(idReserva)) {
            if (!emitida.isVigente()) {
                continue;
            }
            if (Factura.TIPO_COMPLETA.equals(emitida.getTipo())
                    || Factura.TIPO_HOSPEDAJE.equals(emitida.getTipo())) {
                throw new ValidacionException(
                        "El hospedaje de la reserva " + codigoReserva
                        + " ya fue facturado en el documento "
                        + emitida.getNumeroCompleto()
                        + ". Si necesita corregirlo, anule esa factura primero.");
            }
        }
    }
}
