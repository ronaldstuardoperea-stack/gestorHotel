package com.hotel.dao;

import com.hotel.modelo.Habitacion;
import com.hotel.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de las habitaciones fisicas del hotel.
 *
 * Contiene la consulta mas importante del sistema: la de disponibilidad
 * por rango de fechas, que es la que decide si una habitacion se puede
 * vender o no.
 *
 * @author Ronald Perea
 */
public class HabitacionDAO {

    /**
     * Consulta base que trae la habitacion junto con los datos de su
     * categoria. Se reutiliza en casi todos los metodos de lectura para
     * no repetir el mismo JOIN una y otra vez.
     */
    private static final String SELECT_BASE =
            "SELECT h.*, t.nombre AS nombre_tipo, t.tarifa_base, "
          + "       t.capacidad_adultos, t.capacidad_ninos "
          + "FROM habitacion h "
          + "JOIN tipo_habitacion t ON t.id_tipo = h.id_tipo ";

    /**
     * Inserta una habitacion nueva en el inventario.
     *
     * @param habitacion datos de la habitacion
     * @return el identificador asignado por la base de datos
     * @throws SQLException si el numero ya existe o falla la insercion
     */
    public int insertar(Habitacion habitacion) throws SQLException {
        String sql = "INSERT INTO habitacion "
                   + "(numero, id_tipo, piso, estado, descripcion, activo) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, habitacion.getNumero());
            ps.setInt(2, habitacion.getIdTipo());
            ps.setInt(3, habitacion.getPiso());
            ps.setString(4, habitacion.getEstado());
            ps.setString(5, habitacion.getDescripcion());
            ps.setBoolean(6, habitacion.isActivo());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (llaves.next()) {
                    int id = llaves.getInt(1);
                    habitacion.setIdHabitacion(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Actualiza los datos de una habitacion.
     *
     * @param habitacion datos a guardar, con su identificador
     * @throws SQLException si falla la actualizacion
     */
    public void actualizar(Habitacion habitacion) throws SQLException {
        String sql = "UPDATE habitacion SET numero = ?, id_tipo = ?, piso = ?, "
                   + "       estado = ?, descripcion = ?, activo = ? "
                   + "WHERE id_habitacion = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, habitacion.getNumero());
            ps.setInt(2, habitacion.getIdTipo());
            ps.setInt(3, habitacion.getPiso());
            ps.setString(4, habitacion.getEstado());
            ps.setString(5, habitacion.getDescripcion());
            ps.setBoolean(6, habitacion.isActivo());
            ps.setInt(7, habitacion.getIdHabitacion());
            ps.executeUpdate();
        }
    }

    /**
     * Cambia unicamente el estado operativo de una habitacion.
     *
     * Lo usa el panel de ama de llaves para marcar una habitacion como
     * limpia o en mantenimiento sin abrir el formulario completo.
     *
     * @param idHabitacion habitacion a modificar
     * @param estado       uno de los ESTADO_* de la clase Habitacion
     * @throws SQLException si falla la actualizacion
     */
    public void cambiarEstado(int idHabitacion, String estado) throws SQLException {
        String sql = "UPDATE habitacion SET estado = ? WHERE id_habitacion = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idHabitacion);
            ps.executeUpdate();
        }
    }

    /**
     * Retira una habitacion del inventario activo.
     *
     * No se borra el registro: hay reservas historicas que apuntan a el.
     * Un DELETE fallaria por la llave foranea o, peor, dejaria facturas
     * antiguas sin poder decir en que habitacion se hospedo el cliente.
     *
     * @param idHabitacion habitacion a dar de baja
     * @throws SQLException si falla la actualizacion
     */
    public void desactivar(int idHabitacion) throws SQLException {
        String sql = "UPDATE habitacion SET activo = 0 WHERE id_habitacion = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idHabitacion);
            ps.executeUpdate();
        }
    }

    /**
     * Busca una habitacion por su identificador.
     *
     * @param idHabitacion identificador a buscar
     * @return la habitacion encontrada, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Habitacion buscarPorId(int idHabitacion) throws SQLException {
        String sql = SELECT_BASE + "WHERE h.id_habitacion = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idHabitacion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Verifica si un numero de habitacion ya esta en uso.
     *
     * @param numero    numero a verificar
     * @param idExcluir habitacion a ignorar; se usa al editar para que
     *                  no choque consigo misma
     * @return true si el numero pertenece a otra habitacion
     * @throws SQLException si falla la consulta
     */
    public boolean existeNumero(String numero, int idExcluir) throws SQLException {
        String sql = "SELECT 1 FROM habitacion WHERE numero = ? AND id_habitacion <> ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numero);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Lista todas las habitaciones activas del inventario.
     *
     * @return lista ordenada por piso y numero
     * @throws SQLException si falla la consulta
     */
    public List<Habitacion> listarTodas() throws SQLException {
        String sql = SELECT_BASE + "WHERE h.activo = 1 ORDER BY h.piso, h.numero";
        return ejecutarLista(sql);
    }

    /**
     * Lista las habitaciones aplicando filtros opcionales de estado,
     * tipo y piso.
     *
     * La consulta se arma por partes, agregando una condicion solo
     * cuando el filtro correspondiente trae valor. Aun asi los valores
     * viajan como parametros de PreparedStatement y no concatenados en
     * el texto, que es lo que evita la inyeccion SQL.
     *
     * @param estado estado a filtrar, o null para todos
     * @param idTipo categoria a filtrar, o 0 para todas
     * @param piso   piso a filtrar, o 0 para todos
     * @return lista de habitaciones que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Habitacion> listarConFiltros(String estado, int idTipo, int piso)
            throws SQLException {

        StringBuilder sql = new StringBuilder(SELECT_BASE + "WHERE h.activo = 1 ");
        List<Object> parametros = new ArrayList<>();

        if (estado != null && !estado.isBlank()) {
            sql.append("AND h.estado = ? ");
            parametros.add(estado);
        }
        if (idTipo > 0) {
            sql.append("AND h.id_tipo = ? ");
            parametros.add(idTipo);
        }
        if (piso > 0) {
            sql.append("AND h.piso = ? ");
            parametros.add(piso);
        }
        sql.append("ORDER BY h.piso, h.numero");

        List<Habitacion> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Devuelve las habitaciones que se pueden vender en un rango de
     * fechas. Es el corazon de la consulta de disponibilidad.
     *
     * Una habitacion se considera disponible cuando cumple todo esto:
     *
     *  1. Esta activa y no esta en mantenimiento ni bloqueada.
     *  2. No tiene ninguna reserva vigente que se cruce con las fechas.
     *  3. No tiene ningun bloqueo activo que se cruce con las fechas.
     *
     * La regla de cruce de fechas merece atencion. Dos estadias se
     * traslapan cuando:
     *
     *     entrada_existente < salida_nueva  Y  salida_existente > entrada_nueva
     *
     * Las comparaciones son estrictas a proposito. Si un huesped sale el
     * dia 10 y otro entra el dia 10, no hay conflicto: el primero
     * desocupa por la manana y el segundo entra por la tarde. Usar >= o
     * <= dejaria esa noche sin vender por nada.
     *
     * @param fechaEntrada dia de llegada
     * @param fechaSalida  dia de salida
     * @param idTipo       filtrar por categoria, o 0 para todas
     * @return habitaciones libres en ese rango
     * @throws SQLException si falla la consulta
     */
    public List<Habitacion> listarDisponibles(LocalDate fechaEntrada,
                                              LocalDate fechaSalida,
                                              int idTipo) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE
                + "WHERE h.activo = 1 "
                + "  AND h.estado NOT IN ('MANTENIMIENTO', 'BLOQUEADA') "
                + "  AND NOT EXISTS ( "
                + "        SELECT 1 FROM reserva r "
                + "         WHERE r.id_habitacion = h.id_habitacion "
                + "           AND r.estado IN ('PENDIENTE','CONFIRMADA','EN_CASA') "
                + "           AND r.fecha_entrada < ? "
                + "           AND r.fecha_salida  > ? ) "
                + "  AND NOT EXISTS ( "
                + "        SELECT 1 FROM bloqueo_habitacion b "
                + "         WHERE b.id_habitacion = h.id_habitacion "
                + "           AND b.activo = 1 "
                + "           AND b.fecha_inicio < ? "
                + "           AND b.fecha_fin   >= ? ) ");

        if (idTipo > 0) {
            sql.append("AND h.id_tipo = ? ");
        }
        sql.append("ORDER BY h.piso, h.numero");

        List<Habitacion> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            ps.setObject(1, fechaSalida);   // reserva.fecha_entrada < salida nueva
            ps.setObject(2, fechaEntrada);  // reserva.fecha_salida  > entrada nueva
            ps.setObject(3, fechaSalida);   // bloqueo.fecha_inicio  < salida nueva
            ps.setObject(4, fechaEntrada);  // bloqueo.fecha_fin    >= entrada nueva
            if (idTipo > 0) {
                ps.setInt(5, idTipo);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Comprueba si una habitacion concreta esta libre en un rango.
     *
     * Aplica exactamente la misma regla de cruce que
     * {@link #listarDisponibles}, pero para una sola habitacion. El
     * parametro idReservaExcluir permite editar una reserva sin que el
     * sistema la detecte como conflicto consigo misma.
     *
     * @param idHabitacion     habitacion a verificar
     * @param fechaEntrada     dia de llegada
     * @param fechaSalida      dia de salida
     * @param idReservaExcluir reserva a ignorar, o 0 si es una reserva nueva
     * @return true si la habitacion se puede vender en esas fechas
     * @throws SQLException si falla la consulta
     */
    public boolean estaDisponible(int idHabitacion, LocalDate fechaEntrada,
                                  LocalDate fechaSalida, int idReservaExcluir)
            throws SQLException {

        String sqlReservas =
                "SELECT COUNT(*) FROM reserva "
              + " WHERE id_habitacion = ? "
              + "   AND id_reserva <> ? "
              + "   AND estado IN ('PENDIENTE','CONFIRMADA','EN_CASA') "
              + "   AND fecha_entrada < ? "
              + "   AND fecha_salida  > ?";

        String sqlBloqueos =
                "SELECT COUNT(*) FROM bloqueo_habitacion "
              + " WHERE id_habitacion = ? "
              + "   AND activo = 1 "
              + "   AND fecha_inicio < ? "
              + "   AND fecha_fin   >= ?";

        try (Connection con = ConexionBD.conectar()) {

            try (PreparedStatement ps = con.prepareStatement(sqlReservas)) {
                ps.setInt(1, idHabitacion);
                ps.setInt(2, idReservaExcluir);
                ps.setObject(3, fechaSalida);
                ps.setObject(4, fechaEntrada);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; // ya hay una reserva que se cruza
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlBloqueos)) {
                ps.setInt(1, idHabitacion);
                ps.setObject(2, fechaSalida);
                ps.setObject(3, fechaEntrada);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; // hay un bloqueo administrativo
                    }
                }
            }
        }
        return true;
    }

    /**
     * Cuenta cuantas habitaciones hay en cada estado.
     *
     * Alimenta las tarjetas de resumen del tablero. Se resuelve con un
     * GROUP BY en lugar de cinco consultas separadas.
     *
     * @return mapa de estado a cantidad, incluyendo los estados en cero
     * @throws SQLException si falla la consulta
     */
    public Map<String, Integer> contarPorEstado() throws SQLException {
        // LinkedHashMap conserva el orden de insercion, para que las
        // tarjetas del tablero salgan siempre en la misma secuencia.
        Map<String, Integer> conteo = new LinkedHashMap<>();
        conteo.put(Habitacion.ESTADO_DISPONIBLE, 0);
        conteo.put(Habitacion.ESTADO_OCUPADA, 0);
        conteo.put(Habitacion.ESTADO_LIMPIEZA, 0);
        conteo.put(Habitacion.ESTADO_MANTENIMIENTO, 0);
        conteo.put(Habitacion.ESTADO_BLOQUEADA, 0);

        String sql = "SELECT estado, COUNT(*) AS cantidad "
                   + "FROM habitacion WHERE activo = 1 GROUP BY estado";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                conteo.put(rs.getString("estado"), rs.getInt("cantidad"));
            }
        }
        return conteo;
    }

    /**
     * Devuelve la lista de pisos que existen en el hotel, para armar el
     * combo de filtro sin escribirlos a mano.
     *
     * @return lista de numeros de piso, en orden ascendente
     * @throws SQLException si falla la consulta
     */
    public List<Integer> listarPisos() throws SQLException {
        String sql = "SELECT DISTINCT piso FROM habitacion WHERE activo = 1 ORDER BY piso";
        List<Integer> pisos = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pisos.add(rs.getInt("piso"));
            }
        }
        return pisos;
    }

    /**
     * Ejecuta una consulta que devuelve habitaciones sin parametros.
     *
     * @param sql consulta a ejecutar
     * @return lista de habitaciones
     * @throws SQLException si falla la consulta
     */
    private List<Habitacion> ejecutarLista(String sql) throws SQLException {
        List<Habitacion> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Convierte la fila actual del ResultSet en una Habitacion, incluidos
     * los datos de su categoria que trae el JOIN.
     *
     * @param rs cursor posicionado en una fila valida
     * @return objeto con los datos de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Habitacion mapear(ResultSet rs) throws SQLException {
        Habitacion habitacion = new Habitacion(
                rs.getInt("id_habitacion"),
                rs.getString("numero"),
                rs.getInt("id_tipo"),
                rs.getInt("piso"),
                rs.getString("estado"),
                rs.getString("descripcion"),
                rs.getBoolean("activo"));

        habitacion.setNombreTipo(rs.getString("nombre_tipo"));
        habitacion.setTarifaBase(rs.getBigDecimal("tarifa_base"));
        habitacion.setCapacidadAdultos(rs.getInt("capacidad_adultos"));
        habitacion.setCapacidadNinos(rs.getInt("capacidad_ninos"));
        return habitacion;
    }
}
