package com.taller.mecanico.dao;

import com.taller.mecanico.model.OrdenRepuesto;
import com.taller.mecanico.model.Repuesto;
import com.taller.mecanico.util.Conexion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrdenRepuestoDAO {

    private static OrdenRepuesto mapear(ResultSet rs) throws SQLException {
        OrdenRepuesto d = new OrdenRepuesto();
        d.setIdDetalle(rs.getInt("id_detalle"));
        d.setIdOrden(rs.getInt("id_orden"));
        d.setIdRepuesto(rs.getInt("id_repuesto"));
        d.setCantidad(rs.getInt("cantidad"));
        d.setSubtotal(rs.getBigDecimal("subtotal"));
        d.setNombreRepuesto(rs.getString("nombre_rep"));
        d.setPrecioUnitario(rs.getBigDecimal("precio"));
        return d;
    }

    public List<OrdenRepuesto> listarPorOrden(int idOrden) throws SQLException {
        List<OrdenRepuesto> lista = new ArrayList<>();
        String sql = "SELECT d.id_detalle, d.id_orden, d.id_repuesto, d.cantidad, d.subtotal, r.nombre AS nombre_rep, r.precio "
                + "FROM orden_repuestos d INNER JOIN repuestos r ON d.id_repuesto = r.id_repuesto WHERE d.id_orden = ? ORDER BY d.id_detalle";
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

    /**
     * Inserta línea: calcula subtotal = cantidad * precio actual del repuesto y descuenta stock.
     */
    public void insertarLinea(int idOrden, int idRepuesto, int cantidad, RepuestoDAO repuestoDAO) throws SQLException {
        if (cantidad <= 0) {
            throw new SQLException("La cantidad debe ser mayor que cero.");
        }
        Repuesto r = repuestoDAO.buscarPorId(idRepuesto);
        if (r == null) {
            throw new SQLException("Repuesto no encontrado.");
        }
        int stock = r.getStock() != null ? r.getStock() : 0;
        if (stock < cantidad) {
            throw new SQLException("Stock insuficiente. Disponible: " + stock);
        }
        BigDecimal precio = r.getPrecio() != null ? r.getPrecio() : BigDecimal.ZERO;
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
        String sql = "INSERT INTO orden_repuestos (id_orden, id_repuesto, cantidad, subtotal) VALUES (?,?,?,?)";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idOrden);
            ps.setInt(2, idRepuesto);
            ps.setInt(3, cantidad);
            ps.setBigDecimal(4, subtotal);
            ps.executeUpdate();
        }
        repuestoDAO.ajustarStock(idRepuesto, -cantidad);
    }

    public void eliminarLinea(int idDetalle, RepuestoDAO repuestoDAO) throws SQLException {
        String sel = "SELECT id_repuesto, cantidad FROM orden_repuestos WHERE id_detalle = ?";
        int idRep = 0;
        int cant = 0;
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                idRep = rs.getInt("id_repuesto");
                cant = rs.getInt("cantidad");
            }
        }
        String del = "DELETE FROM orden_repuestos WHERE id_detalle = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(del)) {
            ps.setInt(1, idDetalle);
            ps.executeUpdate();
        }
        repuestoDAO.ajustarStock(idRep, cant);
    }

    public void actualizarCantidad(int idDetalle, int nuevaCantidad, RepuestoDAO repuestoDAO) throws SQLException {
        if (nuevaCantidad <= 0) {
            throw new SQLException("La cantidad debe ser mayor que cero.");
        }
        String sel = "SELECT d.id_repuesto, d.cantidad, r.precio FROM orden_repuestos d "
                + "INNER JOIN repuestos r ON d.id_repuesto = r.id_repuesto WHERE d.id_detalle = ?";
        int idRep;
        int cantAnt;
        BigDecimal precio;
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Detalle no encontrado.");
                }
                idRep = rs.getInt("id_repuesto");
                cantAnt = rs.getInt("cantidad");
                precio = rs.getBigDecimal("precio");
            }
        }
        int delta = nuevaCantidad - cantAnt;
        int stockActual = repuestoDAO.obtenerStock(idRep);
        if (delta > 0 && stockActual < delta) {
            throw new SQLException("Stock insuficiente para el incremento. Disponible: " + stockActual);
        }
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(nuevaCantidad)).setScale(2, RoundingMode.HALF_UP);
        String upd = "UPDATE orden_repuestos SET cantidad = ?, subtotal = ? WHERE id_detalle = ?";
        try (Connection c = Conexion.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setInt(1, nuevaCantidad);
            ps.setBigDecimal(2, subtotal);
            ps.setInt(3, idDetalle);
            ps.executeUpdate();
        }
        repuestoDAO.ajustarStock(idRep, -delta);
    }
}
