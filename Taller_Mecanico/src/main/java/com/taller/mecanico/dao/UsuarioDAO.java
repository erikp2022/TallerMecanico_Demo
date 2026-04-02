package com.taller.mecanico.dao;

import com.taller.mecanico.model.Usuario;
import com.taller.mecanico.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private static Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setCorreo(rs.getString("correo"));
        u.setContrasena(rs.getString("contraseña"));
        u.setRol(rs.getString("rol"));
        u.setEstado(rs.getString("estado"));
        int idTec = rs.getInt("id_tecnico");
        if (rs.wasNull()) {
            u.setIdTecnico(null);
        } else {
            u.setIdTecnico(idTec);
        }
        return u;
    }

    /**
     * Autenticación: solo roles {@code admin} y {@code tecnico}, usuario activo;
     * si hay técnico vinculado, el registro en {@code tecnicos} también debe estar activo.
     */
    public Usuario autenticar(String correo, String contrasena) throws SQLException {
        String sql = "SELECT u.id_usuario, u.nombre, u.correo, u.[contraseña], u.rol, u.estado, u.id_tecnico FROM usuarios u "
                + "LEFT JOIN tecnicos t ON u.id_tecnico = t.id_tecnico "
                + "WHERE u.correo = ? AND u.[contraseña] = ? "
                + "AND (LCase(u.estado) = 'activo' OR u.estado = '1') "
                + "AND (u.id_tecnico IS NULL OR (t.id_tecnico IS NOT NULL AND (LCase(t.estado) = 'activo' OR t.estado = '1')))";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, contrasena);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = mapear(rs);
                    if (u.getRol() == null) {
                        return null;
                    }
                    String r = u.getRol().trim();
                    if (!"admin".equalsIgnoreCase(r) && !"tecnico".equalsIgnoreCase(r)) {
                        return null;
                    }
                    return u;
                }
            }
        }
        return null;
    }

    /**
     * Recarga el usuario solo si la cuenta (y el técnico vinculado, si aplica) sigue activa en BD.
     */
    public Usuario recargarSiSesionValida(int idUsuario) throws SQLException {
        String sql = "SELECT u.id_usuario, u.nombre, u.correo, u.[contraseña], u.rol, u.estado, u.id_tecnico FROM usuarios u "
                + "LEFT JOIN tecnicos t ON u.id_tecnico = t.id_tecnico "
                + "WHERE u.id_usuario = ? "
                + "AND (LCase(u.estado) = 'activo' OR u.estado = '1') "
                + "AND (u.id_tecnico IS NULL OR (t.id_tecnico IS NOT NULL AND (LCase(t.estado) = 'activo' OR t.estado = '1')))";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public boolean existeCorreo(String correo, Integer exceptIdUsuario) throws SQLException {
        String sql = "SELECT COUNT(*) AS n FROM usuarios WHERE correo = ?"
                + (exceptIdUsuario != null ? " AND id_usuario <> ?" : "");
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, correo.trim());
            if (exceptIdUsuario != null) {
                ps.setInt(2, exceptIdUsuario);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("n") > 0;
                }
            }
        }
        return false;
    }

    public Usuario buscarPorIdTecnico(int idTecnico) throws SQLException {
        String sql = "SELECT id_usuario, nombre, correo, [contraseña], rol, estado, id_tecnico FROM usuarios WHERE id_tecnico = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTecnico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre, correo, [contraseña], rol, estado, id_tecnico FROM usuarios ORDER BY id_usuario";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_usuario, nombre, correo, [contraseña], rol, estado, id_tecnico FROM usuarios WHERE id_usuario = ?";
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

    public void insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, correo, [contraseña], rol, estado, id_tecnico) VALUES (?,?,?,?,?,?)";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getCorreo());
            ps.setString(3, u.getContrasena());
            ps.setString(4, u.getRol());
            ps.setString(5, u.getEstado());
            if (u.getIdTecnico() != null && u.getIdTecnico() > 0) {
                ps.setInt(6, u.getIdTecnico());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    public void actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE usuarios SET nombre = ?, correo = ?, [contraseña] = ?, rol = ?, estado = ?, id_tecnico = ? WHERE id_usuario = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getCorreo());
            ps.setString(3, u.getContrasena());
            ps.setString(4, u.getRol());
            ps.setString(5, u.getEstado());
            if (u.getIdTecnico() != null) {
                ps.setInt(6, u.getIdTecnico());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setInt(7, u.getIdUsuario());
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza solo la contraseña del usuario indicado (cambio por el propio usuario u operación interna).
     */
    public void actualizarContrasena(int idUsuario, String nuevaContrasena) throws SQLException {
        String sql = "UPDATE usuarios SET [contraseña] = ? WHERE id_usuario = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevaContrasena);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public void eliminar(int idUsuario) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id_usuario = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    public int contar() throws SQLException {
        String sql = "SELECT COUNT(*) AS n FROM usuarios";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("n");
            }
        }
        return 0;
    }
}
