package com.taller.mecanico.dao;

import com.taller.mecanico.model.Tecnico;
import com.taller.mecanico.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TecnicoDAO {

    private static Tecnico mapear(ResultSet rs) throws SQLException {
        Tecnico t = new Tecnico();
        t.setIdTecnico(rs.getInt("id_tecnico"));
        t.setNombre(rs.getString("nombre"));
        t.setCorreo(rs.getString("correo"));
        t.setEspecialidad(rs.getString("especialidad"));
        t.setEstado(rs.getString("estado"));
        return t;
    }

    public List<Tecnico> listarTodos() throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        String sql = "SELECT id_tecnico, nombre, correo, especialidad, estado FROM tecnicos ORDER BY id_tecnico";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Tecnico buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_tecnico, nombre, correo, especialidad, estado FROM tecnicos WHERE id_tecnico = ?";
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

    /**
     * Inserta técnico y devuelve {@code id_tecnico} (Access/JDBC: claves generadas o MAX como respaldo).
     */
    public int insertarRetornaId(Tecnico t) throws SQLException {
        String sql = "INSERT INTO tecnicos (nombre, correo, especialidad, estado) VALUES (?,?,?,?)";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getNombre());
            ps.setString(2, t.getCorreo());
            ps.setString(3, t.getEspecialidad());
            ps.setString(4, t.getEstado());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return obtenerUltimoIdTecnico();
    }

    private int obtenerUltimoIdTecnico() throws SQLException {
        String sql = "SELECT MAX(id_tecnico) AS m FROM tecnicos";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("m");
            }
        }
        throw new SQLException("No se pudo obtener id_tecnico tras insertar.");
    }

    public void actualizar(Tecnico t) throws SQLException {
        String sql = "UPDATE tecnicos SET nombre = ?, correo = ?, especialidad = ?, estado = ? WHERE id_tecnico = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.setString(2, t.getCorreo());
            ps.setString(3, t.getEspecialidad());
            ps.setString(4, t.getEstado());
            ps.setInt(5, t.getIdTecnico());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM tecnicos WHERE id_tecnico = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean existeCorreo(String correo, Integer exceptIdTecnico) throws SQLException {
        String sql = "SELECT COUNT(*) AS n FROM tecnicos WHERE correo = ?"
                + (exceptIdTecnico != null ? " AND id_tecnico <> ?" : "");
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, correo.trim());
            if (exceptIdTecnico != null) {
                ps.setInt(2, exceptIdTecnico);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("n") > 0;
                }
            }
        }
        return false;
    }
}
