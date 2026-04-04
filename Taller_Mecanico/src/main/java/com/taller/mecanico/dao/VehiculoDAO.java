package com.taller.mecanico.dao;

import com.taller.mecanico.model.Vehiculo;
import com.taller.mecanico.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VehiculoDAO {

    private static Vehiculo mapear(ResultSet rs) throws SQLException {
        Vehiculo v = new Vehiculo();
        v.setIdVehiculo(rs.getInt("id_vehiculo"));
        v.setPlaca(rs.getString("placa"));
        v.setMarca(rs.getString("marca"));
        v.setModelo(rs.getString("modelo"));
        Object anio = rs.getObject("anio_veh");
        if (anio != null && !rs.wasNull()) {
            v.setAnio(((Number) anio).intValue());
        }
        v.setIdCliente(rs.getInt("id_cliente"));
        return v;
    }

    private static Vehiculo mapearConCliente(ResultSet rs) throws SQLException {
        Vehiculo v = mapear(rs);
        v.setNombreCliente(rs.getString("nombre_cliente"));
        return v;
    }

    public List<Vehiculo> listarTodos() throws SQLException {
        List<Vehiculo> lista = new ArrayList<>();
        String sql = "SELECT v.id_vehiculo, v.placa, v.marca, v.modelo, v.anio AS anio_veh, v.id_cliente, c.nombre AS nombre_cliente "
                + "FROM vehiculos v INNER JOIN clientes c ON v.id_cliente = c.id_cliente ORDER BY v.id_vehiculo";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearConCliente(rs));
            }
        }
        return lista;
    }

    public Vehiculo buscarPorId(int id) throws SQLException {
        String sql = "SELECT v.id_vehiculo, v.placa, v.marca, v.modelo, v.anio AS anio_veh, v.id_cliente, c.nombre AS nombre_cliente "
                + "FROM vehiculos v INNER JOIN clientes c ON v.id_cliente = c.id_cliente WHERE v.id_vehiculo = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearConCliente(rs);
                }
            }
        }
        return null;
    }

    public void insertar(Vehiculo v) throws SQLException {
        String sql = "INSERT INTO vehiculos (placa, marca, modelo, anio, id_cliente) VALUES (?,?,?,?,?)";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getPlaca());
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            if (v.getAnio() != null) {
                ps.setInt(4, v.getAnio());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setInt(5, v.getIdCliente());
            ps.executeUpdate();
        }
    }

    public void actualizar(Vehiculo v) throws SQLException {
        String sql = "UPDATE vehiculos SET placa = ?, marca = ?, modelo = ?, anio = ?, id_cliente = ? WHERE id_vehiculo = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getPlaca());
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            if (v.getAnio() != null) {
                ps.setInt(4, v.getAnio());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setInt(5, v.getIdCliente());
            ps.setInt(6, v.getIdVehiculo());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM vehiculos WHERE id_vehiculo = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Verifica si el vehículo tiene órdenes de trabajo registradas */
    public boolean tieneOrdenesAsociadas(int idVehiculo) throws SQLException {
        String sql = "SELECT COUNT(*) AS n FROM ordenes_trabajo WHERE id_vehiculo = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idVehiculo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("n") > 0;
                }
            }
        }
        return false;
    }

    public boolean existePlaca(String placa, Integer exceptIdVehiculo) throws SQLException {
        String normalizada = placa != null ? placa.trim().toUpperCase() : "";
        if (normalizada.isEmpty()) return false;
        String sql = "SELECT COUNT(*) AS n FROM vehiculos WHERE UCase(Trim(placa)) = ?"
                + (exceptIdVehiculo != null ? " AND id_vehiculo <> ?" : "");
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalizada);
            if (exceptIdVehiculo != null) ps.setInt(2, exceptIdVehiculo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("n") > 0;
            }
        }
        return false;
    }
}