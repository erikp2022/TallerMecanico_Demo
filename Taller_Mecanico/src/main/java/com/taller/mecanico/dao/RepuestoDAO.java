package com.taller.mecanico.dao;

import com.taller.mecanico.model.Repuesto;
import com.taller.mecanico.util.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepuestoDAO {

    private static Repuesto mapear(ResultSet rs) throws SQLException {
        Repuesto r = new Repuesto();
        r.setIdRepuesto(rs.getInt("id_repuesto"));
        r.setNombre(rs.getString("nombre"));
        r.setPrecio(rs.getBigDecimal("precio"));
        r.setStock(rs.getInt("stock"));
        return r;
    }

    public List<Repuesto> listarTodos() throws SQLException {
        List<Repuesto> lista = new ArrayList<>();
        String sql = "SELECT id_repuesto, nombre, precio, stock FROM repuestos ORDER BY id_repuesto";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Repuesto buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_repuesto, nombre, precio, stock FROM repuestos WHERE id_repuesto = ?";
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

    public int obtenerStock(int idRepuesto) throws SQLException {
        String sql = "SELECT stock FROM repuestos WHERE id_repuesto = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idRepuesto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        }
        return 0;
    }

    public void insertar(Repuesto r) throws SQLException {
        String sql = "INSERT INTO repuestos (nombre, precio, stock) VALUES (?,?,?)";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getNombre());
            ps.setBigDecimal(2, r.getPrecio() != null ? r.getPrecio() : BigDecimal.ZERO);
            ps.setInt(3, r.getStock() != null ? r.getStock() : 0);
            ps.executeUpdate();
        }
    }

    public void actualizar(Repuesto r) throws SQLException {
        String sql = "UPDATE repuestos SET nombre = ?, precio = ?, stock = ? WHERE id_repuesto = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getNombre());
            ps.setBigDecimal(2, r.getPrecio() != null ? r.getPrecio() : BigDecimal.ZERO);
            ps.setInt(3, r.getStock() != null ? r.getStock() : 0);
            ps.setInt(4, r.getIdRepuesto());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM repuestos WHERE id_repuesto = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Ajusta stock (positivo suma, negativo resta). */
    public void ajustarStock(int idRepuesto, int delta) throws SQLException {
        String sql = "UPDATE repuestos SET stock = stock + ? WHERE id_repuesto = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, idRepuesto);
            ps.executeUpdate();
        }
    }
}
