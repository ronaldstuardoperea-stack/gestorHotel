package com.hotel.servicio;

import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.OcupanteDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.dao.TemporadaDAO;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.Temporada;
import com.hotel.util.ValidacionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reglas de negocio del ciclo de vida de una reserva.
 *
 * Esta clase existe para que los Servlets no tengan que conocer las
 * reglas del hotel. Un Servlet recibe datos del formulario, los valida
 * y le pide a este servicio que haga la operacion; el servicio decide si
 * se puede o no, y en que orden hay que tocar las tablas.
 *
 * Es la capa que en el patron MVC suele quedar repartida entre el
 * controlador y el modelo. Separarla tiene una ventaja concreta: la
 * regla de "no se puede hacer check-in antes de la fecha de entrada"
 * esta escrita en un solo lugar, y da igual si la llamada viene del
 * modulo de reservas o del panel de recepcion.
 *
 * @author Ronald Perea
 */
public class ServicioReservas {

    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final HabitacionDAO habitacionDAO = new HabitacionDAO();
    private final TemporadaDAO temporadaDAO = new TemporadaDAO();
    private final OcupanteDAO ocupanteDAO = new OcupanteDAO();

    /**
     * Margen de tolerancia para adelantar un check-in, en dias.
     *
     * Un huesped que llega un dia antes de lo previsto es algo normal en
     * un hotel y recepcion debe poder atenderlo. Lo que no tiene sentido
     * es darle entrada a una reserva de dentro de tres semanas.
     */
    private static final int DIAS_TOLERANCIA_CHECKIN = 1;

    // -----------------------------------------------------------------
    // Tarifas
    // -----------------------------------------------------------------

    /**
     * Calcula la tarifa por noche que corresponde a una habitacion en
     * una fecha determinada.
     *
     * Se toma la tarifa base del tipo de habitacion y se le aplica el
     * factor de la temporada vigente en la fecha de entrada. Si esa
     * fecha no cae en ninguna temporada, se cobra el precio de lista.
     *
     * Se usa la fecha de ENTRADA para toda la estadia, en lugar de
     * cobrar cada noche con su propia temporada. Es la practica habitual
     * en hoteles pequenos: se cotiza un precio por noche al reservar y
     * ese es el que aplica, aunque la estadia cruce el cambio de
     * temporada.
     *
     * @param idHabitacion habitacion a cotizar
     * @param fechaEntrada dia de llegada
     * @return tarifa sugerida por noche
     * @throws SQLException        si falla la consulta
     * @throws ValidacionException si la habitacion no existe
     */
    public BigDecimal calcularTarifaSugerida(int idHabitacion, LocalDate fechaEntrada)
            throws SQLException {

        Habitacion habitacion = habitacionDAO.buscarPorId(idHabitacion);
        if (habitacion == null) {
            throw new ValidacionException("La habitacion seleccionada no existe.");
        }

        BigDecimal tarifaBase = habitacion.getTarifaBase();
        Temporada temporada = temporadaDAO.buscarPorFecha(fechaEntrada);

        if (temporada == null) {
            return tarifaBase.setScale(2, RoundingMode.HALF_UP);
        }
        return temporada.aplicarA(tarifaBase);
    }

    /**
     * Devuelve la temporada vigente en una fecha, para mostrarle al
     * recepcionista por que el precio sugerido subio o bajo.
     *
     * @param fecha dia a consultar
     * @return la temporada, o null si esa fecha no tiene ninguna
     * @throws SQLException si falla la consulta
     */
    public Temporada obtenerTemporada(LocalDate fecha) throws SQLException {
        return temporadaDAO.buscarPorFecha(fecha);
    }

    // -----------------------------------------------------------------
    // Alta y modificacion
    // -----------------------------------------------------------------

    /**
     * Crea una reserva nueva aplicando todas las validaciones del hotel.
     *
     * @param reserva datos capturados en el formulario
     * @return el identificador de la reserva creada
     * @throws SQLException        si falla la insercion
     * @throws ValidacionException si alguna regla del hotel no se cumple
     */
    public int crearReserva(Reserva reserva) throws SQLException {
        validarFechas(reserva);
        validarCapacidad(reserva);
        validarDisponibilidad(reserva, 0);

        reserva.setNoches(reserva.calcularNoches());
        reserva.setCodigo(reservaDAO.generarCodigo());

        if (reserva.getEstado() == null) {
            reserva.setEstado(Reserva.ESTADO_PENDIENTE);
        }
        return reservaDAO.insertar(reserva);
    }

    /**
     * Modifica una reserva que todavia no ha hecho check-in.
     *
     * Una vez que el huesped entro, cambiarle las fechas o la habitacion
     * dejaria inconsistentes los consumos ya anotados y el estado de la
     * habitacion. En ese caso hay que hacer check-out y crear una
     * reserva nueva.
     *
     * @param reserva datos a guardar, con su identificador
     * @throws SQLException        si falla la actualizacion
     * @throws ValidacionException si la reserva ya no admite cambios
     */
    public void modificarReserva(Reserva reserva) throws SQLException {
        Reserva actual = obtenerOFallar(reserva.getIdReserva());

        if (!actual.isCancelable()) {
            throw new ValidacionException(
                    "La reserva " + actual.getCodigo() + " esta en estado "
                    + actual.getEstadoLegible() + " y ya no se puede modificar.");
        }

        validarFechas(reserva);
        validarCapacidad(reserva);
        // Se excluye la propia reserva del chequeo de traslape: de lo
        // contrario se detectaria a si misma como conflicto.
        validarDisponibilidad(reserva, reserva.getIdReserva());

        reserva.setNoches(reserva.calcularNoches());
        reservaDAO.actualizar(reserva);
    }

    // -----------------------------------------------------------------
    // Cambios de estado
    // -----------------------------------------------------------------

    /**
     * Confirma una reserva pendiente.
     *
     * @param idReserva reserva a confirmar
     * @throws SQLException        si falla la actualizacion
     * @throws ValidacionException si la reserva no esta pendiente
     */
    public void confirmar(int idReserva) throws SQLException {
        Reserva reserva = obtenerOFallar(idReserva);

        if (!Reserva.ESTADO_PENDIENTE.equals(reserva.getEstado())) {
            throw new ValidacionException(
                    "Solo se pueden confirmar reservas pendientes. La reserva "
                    + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible() + ".");
        }
        reservaDAO.cambiarEstado(idReserva, Reserva.ESTADO_CONFIRMADA);
    }

    /**
     * Cancela una reserva antes de la llegada del huesped.
     *
     * Los anticipos ya recibidos no se borran: quedan registrados y, si
     * corresponde devolverlos, se registra un pago de tipo REEMBOLSO
     * desde el modulo de pagos. Asi el movimiento de caja queda
     * documentado en lugar de desaparecer.
     *
     * @param idReserva reserva a cancelar
     * @throws SQLException        si falla la actualizacion
     * @throws ValidacionException si la reserva ya no se puede cancelar
     */
    public void cancelar(int idReserva) throws SQLException {
        Reserva reserva = obtenerOFallar(idReserva);

        if (!reserva.isCancelable()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible() + " y ya no se puede cancelar.");
        }
        reservaDAO.cambiarEstado(idReserva, Reserva.ESTADO_CANCELADA);
    }

    /**
     * Marca una reserva como no presentada.
     *
     * Se usa cuando pasa la fecha de entrada y el huesped nunca llego.
     * A diferencia de la cancelacion, deja constancia de que el hotel
     * mantuvo la habitacion apartada sin venderla, lo que justifica
     * retener el anticipo segun la politica del hotel.
     *
     * @param idReserva reserva a marcar
     * @throws SQLException        si falla la actualizacion
     * @throws ValidacionException si la reserva no estaba por llegar
     */
    public void marcarNoShow(int idReserva) throws SQLException {
        Reserva reserva = obtenerOFallar(idReserva);

        if (!reserva.isCheckinHabilitado()) {
            throw new ValidacionException(
                    "Solo se puede marcar como no presentada una reserva "
                    + "pendiente o confirmada.");
        }
        if (reserva.getFechaEntrada().isAfter(LocalDate.now())) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " es para el "
                    + reserva.getFechaEntrada()
                    + ". Todavia no se puede marcar como no presentada.");
        }
        reservaDAO.cambiarEstado(idReserva, Reserva.ESTADO_NO_SHOW);
    }

    /**
     * Registra la entrada del huesped al hotel.
     *
     * Antes de dar entrada se verifica que la reserva este en un estado
     * valido, que la fecha sea razonable y que la habitacion no este
     * ocupada por alguien mas.
     *
     * @param idReserva reserva que llega
     * @throws SQLException        si falla la operacion
     * @throws ValidacionException si no se cumple alguna condicion
     */
    public void registrarCheckin(int idReserva) throws SQLException {
        Reserva reserva = obtenerOFallar(idReserva);

        if (!reserva.isCheckinHabilitado()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible() + " y no admite check-in.");
        }

        LocalDate hoy = LocalDate.now();
        if (reserva.getFechaSalida().isBefore(hoy) || reserva.getFechaSalida().isEqual(hoy)) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " tiene fecha de salida el "
                    + reserva.getFechaSalida() + ", que ya paso. "
                    + "Corrija las fechas antes de dar entrada.");
        }

        long diasFaltantes = ChronoUnit.DAYS.between(hoy, reserva.getFechaEntrada());
        if (diasFaltantes > DIAS_TOLERANCIA_CHECKIN) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " es para el "
                    + reserva.getFechaEntrada() + ". Faltan " + diasFaltantes
                    + " dias y no se puede adelantar tanto el check-in.");
        }

        Habitacion habitacion = habitacionDAO.buscarPorId(reserva.getIdHabitacion());
        if (habitacion == null) {
            throw new ValidacionException("La habitacion de la reserva ya no existe.");
        }
        if (Habitacion.ESTADO_OCUPADA.equals(habitacion.getEstado())) {
            throw new ValidacionException(
                    "La habitacion " + habitacion.getNumero() + " aparece ocupada. "
                    + "Verifique si el huesped anterior ya hizo check-out.");
        }
        if (habitacion.isFueraDeServicio()) {
            throw new ValidacionException(
                    "La habitacion " + habitacion.getNumero() + " esta en "
                    + habitacion.getEstado().toLowerCase()
                    + " y no puede recibir huespedes.");
        }

        reservaDAO.registrarCheckin(idReserva, reserva.getIdHabitacion());
    }

    /**
     * Registra la salida del huesped.
     *
     * No se exige que la cuenta este saldada: hay hoteles que permiten
     * salir con saldo cuando el cliente es una empresa con credito. El
     * sistema muestra el saldo pendiente en pantalla y deja la decision
     * en manos de recepcion, pero la deuda queda registrada.
     *
     * @param idReserva reserva que sale
     * @throws SQLException        si falla la operacion
     * @throws ValidacionException si el huesped no estaba hospedado
     */
    public void registrarCheckout(int idReserva) throws SQLException {
        Reserva reserva = obtenerOFallar(idReserva);

        if (!reserva.isCheckoutHabilitado()) {
            throw new ValidacionException(
                    "La reserva " + reserva.getCodigo() + " esta en estado "
                    + reserva.getEstadoLegible()
                    + ". Solo se puede dar salida a un huesped que este en casa.");
        }
        reservaDAO.registrarCheckout(idReserva, reserva.getIdHabitacion());
    }

    // -----------------------------------------------------------------
    // Validaciones internas
    // -----------------------------------------------------------------

    /**
     * Verifica que el rango de fechas tenga sentido.
     *
     * @param reserva reserva a validar
     * @throws ValidacionException si las fechas no son coherentes
     */
    private void validarFechas(Reserva reserva) {
        LocalDate entrada = reserva.getFechaEntrada();
        LocalDate salida = reserva.getFechaSalida();

        if (entrada == null || salida == null) {
            throw new ValidacionException("Debe indicar la fecha de entrada y la de salida.");
        }
        if (!salida.isAfter(entrada)) {
            throw new ValidacionException(
                    "La fecha de salida debe ser posterior a la de entrada. "
                    + "Una estadia tiene que cubrir al menos una noche.");
        }
        if (entrada.isBefore(LocalDate.now().minusDays(1))) {
            throw new ValidacionException(
                    "No se pueden crear reservas con fecha de entrada en el pasado.");
        }

        long noches = ChronoUnit.DAYS.between(entrada, salida);
        if (noches > 365) {
            throw new ValidacionException(
                    "Una reserva no puede exceder 365 noches. "
                    + "Verifique las fechas capturadas.");
        }
    }

    /**
     * Verifica que el grupo quepa en el tipo de habitacion elegido.
     *
     * @param reserva reserva a validar
     * @throws SQLException        si falla la consulta
     * @throws ValidacionException si se excede la capacidad
     */
    private void validarCapacidad(Reserva reserva) throws SQLException {
        Habitacion habitacion = habitacionDAO.buscarPorId(reserva.getIdHabitacion());
        if (habitacion == null) {
            throw new ValidacionException("La habitacion seleccionada no existe.");
        }
        if (reserva.getAdultos() > habitacion.getCapacidadAdultos()) {
            throw new ValidacionException(
                    "La habitacion " + habitacion.getNumero() + " (" + habitacion.getNombreTipo()
                    + ") admite maximo " + habitacion.getCapacidadAdultos()
                    + " adultos y se indicaron " + reserva.getAdultos() + ".");
        }
        if (reserva.getNinos() > habitacion.getCapacidadNinos()) {
            throw new ValidacionException(
                    "La habitacion " + habitacion.getNumero() + " (" + habitacion.getNombreTipo()
                    + ") admite maximo " + habitacion.getCapacidadNinos()
                    + " ninos y se indicaron " + reserva.getNinos() + ".");
        }
    }

    /**
     * Verifica que la habitacion este libre en las fechas pedidas.
     *
     * @param reserva          reserva a validar
     * @param idReservaExcluir reserva a ignorar al buscar traslapes, o 0
     * @throws SQLException        si falla la consulta
     * @throws ValidacionException si la habitacion ya esta comprometida
     */
    private void validarDisponibilidad(Reserva reserva, int idReservaExcluir)
            throws SQLException {
        boolean libre = habitacionDAO.estaDisponible(
                reserva.getIdHabitacion(),
                reserva.getFechaEntrada(),
                reserva.getFechaSalida(),
                idReservaExcluir);

        if (!libre) {
            Habitacion habitacion = habitacionDAO.buscarPorId(reserva.getIdHabitacion());
            String numero = habitacion != null ? habitacion.getNumero() : "seleccionada";
            throw new ValidacionException(
                    "La habitacion " + numero + " no esta disponible del "
                    + reserva.getFechaEntrada() + " al " + reserva.getFechaSalida()
                    + ". Ya tiene una reserva o un bloqueo en esas fechas.");
        }
    }

    /**
     * Recupera una reserva y falla con un mensaje claro si no existe.
     *
     * @param idReserva identificador a buscar
     * @return la reserva encontrada
     * @throws SQLException        si falla la consulta
     * @throws ValidacionException si la reserva no existe
     */
    private Reserva obtenerOFallar(int idReserva) throws SQLException {
        Reserva reserva = reservaDAO.buscarPorId(idReserva);
        if (reserva == null) {
            throw new ValidacionException(
                    "No se encontro la reserva solicitada (codigo interno "
                    + idReserva + ").");
        }
        return reserva;
    }

    /**
     * Indica si faltan ocupantes por registrar en una reserva.
     *
     * Se usa en el check-in para recordarle a recepcion que anote los
     * datos de todos los huespedes. Es un aviso, no un impedimento.
     *
     * @param reserva reserva a revisar
     * @return true si hay menos ocupantes registrados que huespedes
     * @throws SQLException si falla la consulta
     */
    public boolean faltanOcupantes(Reserva reserva) throws SQLException {
        int registrados = ocupanteDAO.contarPorReserva(reserva.getIdReserva());
        return registrados < reserva.getTotalHuespedes();
    }
}
