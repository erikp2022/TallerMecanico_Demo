package com.taller.mecanico.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Inicializa la ruta de la base de datos Access desde web.xml (parámetro taller.accdb.path).
 */
@WebListener
public class TallerContextListener implements ServletContextListener {

    public static final String PARAM_DB_PATH = "taller.accdb.path";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String path = sce.getServletContext().getInitParameter(PARAM_DB_PATH);
        if (path != null && !path.isBlank()) {
            Conexion.setRutaBaseDatos(path.trim());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nada
    }
}
