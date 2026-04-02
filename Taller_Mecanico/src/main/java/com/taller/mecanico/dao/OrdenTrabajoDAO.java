package com.taller.mecanico.dao;

import com.taller.mecanico.model.OrdenTrabajo;
import com.taller.mecanico.util.Conexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrdenTrabajoDAO {

    private static OrdenTrabajo mapear(ResultSet rs) throws SQLException {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setIdOrden(rs.getInt("id_orden"));
        Date fi = rs.getDate("fecha_ingreso");
        if (fi != null) {
            o.setFechaIngreso(fi.toLocalDate());
        }
        Date fs = rs.getDate("fecha_salida");
        if (fs != null) {
            o.setFechaSalida(fs.toLocalDate());
        }
        o.setEstado(rs.getString("estado"));
        o.setDescripcion(rs.getString("descripcion"));
        o.setIdVehiculo(rs.getInt("id_vehiculo"));
        int idTec = rs.getInt("id_tecnico");
        if (rs.wasNull()) {
            o.setIdTecnico(null);
        } else {
            o.setIdTecnico(idTec);
        }
        int idUsu = rs.getInt("id_usuario");
        if (rs.wasNull()) {
            o.setIdUsuario(null);
        } else {
            o.setIdUsuario(idUsu);
        }
        return o;
    }

    private static OrdenTrabajo mapearCompleto(ResultSet rs) throws SQLException {
        OrdenTrabajo o = mapear(rs);
        o.setPlacaVehiculo(rs.getString("placa_vehiculo"));
        o.setNombreTecnico(rs.getString("nombre_tecnico"));
        o.setNombreCliente(rs.getString("nombre_cliente"));
        return o;
    }

    private static final String SQL_BASE = "SELECT o.id_orden, o.fecha_ingreso, o.fecha_salida, o.estado, o.descripcion, "
            + "o.id_vehiculo, o.id_tecnico, o.id_usuario, v.placa AS placa_vehiculo, t.nombre AS nombre_tecnico, c.nombre AS nombre_cliente "
            + "FROM ((ordenes_trabajo o INNER JOIN vehiculos v ON o.id_vehiculo = v.id_vehiculo) "
            + "INNER JOIN clientes c ON v.id_cliente = c.id_cliente) "
            + "LEFT JOIN tecnicos t ON o.id_tecnico = t.id_tecnico ";

    public List<OrdenTrabajo> listarConDetalle() throws SQLException {
        return listarConDetalleFiltrado(null, null, null);
    }

    /**
     * @param placa      si no es null, filtra por placa (LIKE)
     * @param estado     si no es null, filtra por estado exacto
     * @param idTecnico  si no es null, filtra por técnico
     */
    public List<OrdenTrabajo> listarConDetalleFiltrado(String placa, String estado, Integer idTecnico) throws SQLException {
        List<OrdenTrabajo> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SQL_BASE).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (placa != null && !placa.isBlank()) {
            sql.append(" AND v.placa LIKE ? ");
            params.add("%" + placa.trim() + "%");
        }
        if (estado != null && !estado.isBlank()) {
            sql.append(" AND o.estado = ? ");
            params.add(estado.trim());
        }
        if (idTecnico != null) {
            sql.append(" AND o.id_tecnico = ? ");
            params.add(idTecnico);
        }
        sql.append(" ORDER BY o.id_orden DESC");
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) {
                    ps.setInt(i + 1, (Integer) p);
                } else {
                    ps.setString(i + 1, (String) p);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCompleto(rs));
                }
            }
        }
        return lista;
    }

    public OrdenTrabajo buscarPorId(int id) throws SQLException {
        String sql = SQL_BASE + " WHERE o.id_orden = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCompleto(rs);
                }
            }
        }
        return null;
    }

    /** Comprueba que la orden exista y esté asignada al técnico indicado. */
    public boolean ordenAsignadaATecnico(int idOrden, int idTecnico) throws SQLException {
        OrdenTrabajo o = buscarPorId(idOrden);
        return o != null && o.getIdTecnico() != null && o.getIdTecnico() == idTecnico;
    }

    public int contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) AS n FROM ordenes_trabajo";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("n");
            }
        }
        return 0;
    }

    public int contarPorTecnico(int idTecnico) throws SQLException {
        String sql = "SELECT COUNT(*) AS n FROM ordenes_trabajo WHERE id_tecnico = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTecnico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("n");
                }
            }
        }
        return 0;
    }

    /** Estado -> cantidad (orden de inserción estable) */
    public Map<String, Long> contarPorEstado() throws SQLException {
        Map<String, Long> map = new LinkedHashMap<>();
        String sql = "SELECT estado, COUNT(*) AS n FROM ordenes_trabajo GROUP BY estado";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("estado"), rs.getLong("n"));
            }
        }
        return map;
    }

    public Map<String, Long> contarPorEstadoPorTecnico(int idTecnico) throws SQLException {
        Map<String, Long> map = new LinkedHashMap<>();
        String sql = "SELECT estado, COUNT(*) AS n FROM ordenes_trabajo WHERE id_tecnico = ? GROUP BY estado";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTecnico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("estado"), rs.getLong("n"));
                }
            }
        }
        return map;
    }

    public void insertar(OrdenTrabajo o) throws SQLException {
        String sql = "INSERT INTO ordenes_trabajo (fecha_ingreso, fecha_salida, estado, descripcion, id_vehiculo, id_tecnico, id_usuario) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (o.getFechaIngreso() != null) {
                ps.setDate(1, Date.valueOf(o.getFechaIngreso()));
            } else {
                ps.setNull(1, java.sql.Types.DATE);
            }
            if (o.getFechaSalida() != null) {
                ps.setDate(2, Date.valueOf(o.getFechaSalida()));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }
            ps.setString(3, o.getEstado());
            ps.setString(4, o.getDescripcion());
            ps.setInt(5, o.getIdVehiculo());
            if (o.getIdTecnico() != null) {
                ps.setInt(6, o.getIdTecnico());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            if (o.getIdUsuario() != null) {
                ps.setInt(7, o.getIdUsuario());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    public void actualizar(OrdenTrabajo o) throws SQLException {
        String sql = "UPDATE ordenes_trabajo SET fecha_ingreso = ?, fecha_salida = ?, estado = ?, descripcion = ?, "
                + "id_vehiculo = ?, id_tecnico = ?, id_usuario = ? WHERE id_orden = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (o.getFechaIngreso() != null) {
                ps.setDate(1, Date.valueOf(o.getFechaIngreso()));
            } else {
                ps.setNull(1, java.sql.Types.DATE);
            }
            if (o.getFechaSalida() != null) {
                ps.setDate(2, Date.valueOf(o.getFechaSalida()));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }
            ps.setString(3, o.getEstado());
            ps.setString(4, o.getDescripcion());
            ps.setInt(5, o.getIdVehiculo());
            if (o.getIdTecnico() != null) {
                ps.setInt(6, o.getIdTecnico());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            if (o.getIdUsuario() != null) {
                ps.setInt(7, o.getIdUsuario());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setInt(8, o.getIdOrden());
            ps.executeUpdate();
        }
    }

    public void eliminar(int idOrden) throws SQLException {
        String sql = "DELETE FROM ordenes_trabajo WHERE id_orden = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            ps.executeUpdate();
        }
    }
}
