package com.taller.mecanico.controller;

import com.taller.mecanico.dao.OrdenTrabajoDAO;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    private final OrdenTrabajoDAO ordenDAO = new OrdenTrabajoDAO();
    private int totalOrdenes;
    /** Estado → cantidad (orden estable para la vista). */
    private Map<String, Long> ordenesPorEstado = new LinkedHashMap<>();

    public void cargar() {
        try {
            if (loginBean != null && loginBean.isTecnico()) {
                if (loginBean.getIdTecnicoAsociado() == null) {
                    totalOrdenes = 0;
                    ordenesPorEstado = new LinkedHashMap<>();
                    return;
                }
                int idT = loginBean.getIdTecnicoAsociado();
                totalOrdenes = ordenDAO.contarPorTecnico(idT);
                ordenesPorEstado = ordenDAO.contarPorEstadoPorTecnico(idT);
            } else {
                totalOrdenes = ordenDAO.contarTotal();
                ordenesPorEstado = ordenDAO.contarPorEstado();
            }
        } catch (Exception ex) {
            totalOrdenes = 0;
            ordenesPorEstado = new LinkedHashMap<>();
        }
    }

    /** Porcentaje para barra CSS (0–100). */
    public int porcentajeBarra(String estado) {
        if (totalOrdenes <= 0) {
            return 0;
        }
        Long n = ordenesPorEstado.get(estado);
        if (n == null) {
            return 0;
        }
        return (int) Math.round((n * 100.0) / totalOrdenes);
    }

    public int getTotalOrdenes() {
        return totalOrdenes;
    }

    public Map<String, Long> getOrdenesPorEstado() {
        if (ordenesPorEstado == null || ordenesPorEstado.isEmpty()) {
            cargar();
        }
        return ordenesPorEstado;
    }

    public List<String> getClavesEstados() {
        getOrdenesPorEstado();
        return new ArrayList<>(ordenesPorEstado.keySet());
    }

    public Long cantidadEstado(String estado) {
        if (ordenesPorEstado == null) {
            return 0L;
        }
        Long n = ordenesPorEstado.get(estado);
        return n != null ? n : 0L;
    }

    public String estiloBarra(String estado) {
        if (estado == null) {
            return "fill-proceso";
        }
        String e = estado.toLowerCase();
        if (e.contains("pendiente")) {
            return "fill-pendiente";
        }
        if (e.contains("proceso")) {
            return "fill-proceso";
        }
        if (e.contains("finaliz")) {
            return "fill-finalizado";
        }
        return "fill-proceso";
    }
}
