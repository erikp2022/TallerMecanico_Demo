package com.taller.mecanico.dao;

import com.taller.mecanico.model.Reparacion;
import com.taller.mecanico.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReparacionDAO {

    private static Reparacion mapear(ResultSet rs) throws SQLException {
        Reparacion r = new Reparacion();
        r.setIdReparacion(rs.getInt("id_reparacion"));
        r.setDescripcion(rs.getString("descripcion"));
        r.setEstado(rs.getString("estado"));
        r.setIdOrden(rs.getInt("id_orden"));
        return r;
    }

    public List<Reparacion> listarPorOrden(int idOrden) throws SQLException {
        List<Reparacion> lista = new ArrayList<>();
        String sql = "SELECT id_reparacion, descripcion, estado, id_orden FROM reparaciones WHERE id_orden = ? ORDER BY id_reparacion";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public List<Reparacion> listarTodas() throws SQLException {
        List<Reparacion> lista = new ArrayList<>();
        String sql = "SELECT id_reparacion, descripcion, estado, id_orden FROM reparaciones ORDER BY id_reparacion DESC";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Reparaciones de órdenes asignadas a un técnico (filtrado por id_tecnico de la orden). */
    public List<Reparacion> listarPorTecnico(int idTecnico) throws SQLException {
        List<Reparacion> lista = new ArrayList<>();
        String sql = "SELECT r.id_reparacion, r.descripcion, r.estado, r.id_orden FROM reparaciones r "
                + "INNER JOIN ordenes_trabajo o ON r.id_orden = o.id_orden WHERE o.id_tecnico = ? ORDER BY r.id_reparacion DESC";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTecnico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public Reparacion buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_reparacion, descripcion, estado, id_orden FROM reparaciones WHERE id_reparacion = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public void insertar(Reparacion r) throws SQLException {
        String sql = "INSERT INTO reparaciones (descripcion, estado, id_orden) VALUES (?,?,?)";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getDescripcion());
            ps.setString(2, r.getEstado());
            ps.setInt(3, r.getIdOrden());
            ps.executeUpdate();
        }
    }

    public void actualizar(Reparacion r) throws SQLException {
        String sql = "UPDATE reparaciones SET descripcion = ?, estado = ?, id_orden = ? WHERE id_reparacion = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getDescripcion());
            ps.setString(2, r.getEstado());
            ps.setInt(3, r.getIdOrden());
            ps.setInt(4, r.getIdReparacion());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM reparaciones WHERE id_reparacion = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
