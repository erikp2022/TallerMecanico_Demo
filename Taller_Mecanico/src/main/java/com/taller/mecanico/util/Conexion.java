package com.taller.mecanico.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexión JDBC a Microsoft Access (.accdb / .mdb).
 * <p>
 * Se usa <strong>UCanAccess</strong> (driver JDBC en Java). Oracle eliminó el puente
 * {@code jdbc:odbc:...} en JDK 8+, por eso esa URL ya no encuentra ningún driver.
 * Las consultas SQL del proyecto siguen siendo las mismas para Access.
 * </p>
 * <p>
 * Configure la ruta absoluta al archivo mediante {@link #setRutaBaseDatos(String)}
 * o el parámetro de contexto {@code taller.accdb.path} en {@code web.xml}.
 * Use barras normales ({@code /}) o {@code \\} en la ruta.
 * </p>
 */
public final class Conexion {

    private static volatile String rutaBaseDatos;

    private Conexion() {
    }

    public static void setRutaBaseDatos(String ruta) {
        rutaBaseDatos = ruta;
    }

    public static String getRutaBaseDatos() {
        return rutaBaseDatos;
    }

    private static String normalizarRutaParaJdbc(String ruta) {
        if (ruta == null) {
            return "";
        }
        return ruta.trim().replace('\\', '/');
    }

    /**
     * Obtiene una conexión nueva. El llamador debe cerrarla (try-with-resources).
     */
    public static Connection obtenerConexion() throws SQLException {
        if (rutaBaseDatos == null || rutaBaseDatos.isBlank()) {
            throw new SQLException(
                    "Ruta de la base de datos Access no configurada. Defina el parámetro de contexto 'taller.accdb.path' o Conexion.setRutaBaseDatos(...).");
        }
        String ruta = normalizarRutaParaJdbc(rutaBaseDatos);
        String url = "jdbc:ucanaccess://" + ruta;
        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Driver UCanAccess no encontrado. Verifique la dependencia 'ucanaccess' en pom.xml y que el WAR incluya las JAR.", e);
        }
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new SQLException("Error al conectar con Access (UCanAccess): " + e.getMessage(), e);
        }
    }

    public static void cerrar(AutoCloseable... recursos) {
        if (recursos == null) {
            return;
        }
        for (AutoCloseable r : recursos) {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception ignored) {
                    // registro opcional en producción
                }
            }
        }
    }
}
