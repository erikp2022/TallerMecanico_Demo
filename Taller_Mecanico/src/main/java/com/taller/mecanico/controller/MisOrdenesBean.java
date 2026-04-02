package com.taller.mecanico.controller;

import com.taller.mecanico.dao.OrdenTrabajoDAO;
import com.taller.mecanico.dao.ReparacionDAO;
import com.taller.mecanico.model.OrdenTrabajo;
import com.taller.mecanico.model.Reparacion;
import com.taller.mecanico.util.EstadosOrden;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Órdenes asignadas al técnico en sesión: solo lectura de las suyas; actualización de estado,
 * descripción de la orden y reparaciones vinculadas a esas órdenes.
 */
@Named("misOrdenesBean")
@ViewScoped
public class MisOrdenesBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    private final OrdenTrabajoDAO ordenDAO = new OrdenTrabajoDAO();
    private final ReparacionDAO reparacionDAO = new ReparacionDAO();

    private List<OrdenTrabajo> lista;
    private String filtroPlaca;
    private String filtroEstado;

    private OrdenTrabajo ordenEdicion;
    /** Orden cuya lista de reparaciones se muestra en el diálogo. */
    private OrdenTrabajo ordenReparaciones;
    private List<Reparacion> reparacionesOrden;
    private Reparacion reparacionEdicion;

    public String initPagina() {
        if (loginBean == null) {
            return null;
        }
        String r = loginBean.exigirTecnico();
        if (r != null) {
            return r;
        }
        cargarLista();
        return null;
    }

    public void cargarLista() {
        try {
            Integer idTec = loginBean != null ? loginBean.getIdTecnicoAsociado() : null;
            if (idTec == null) {
                lista = List.of();
                return;
            }
            lista = ordenDAO.listarConDetalleFiltrado(filtroPlaca, filtroEstado, idTec);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void limpiarFiltros() {
        filtroPlaca = null;
        filtroEstado = null;
        cargarLista();
    }

    public void prepararEditarOrden(OrdenTrabajo fila) {
        try {
            Integer idTec = loginBean.getIdTecnicoAsociado();
            if (idTec == null) {
                return;
            }
            OrdenTrabajo db = ordenDAO.buscarPorId(fila.getIdOrden());
            if (db == null || db.getIdTecnico() == null || !db.getIdTecnico().equals(idTec)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Seguridad", "No puede modificar esta orden."));
                return;
            }
            ordenEdicion = db;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void guardarOrdenTecnico() {
        try {
            Integer idTec = loginBean.getIdTecnicoAsociado();
            if (ordenEdicion == null || idTec == null) {
                return;
            }
            OrdenTrabajo db = ordenDAO.buscarPorId(ordenEdicion.getIdOrden());
            if (db == null || db.getIdTecnico() == null || !db.getIdTecnico().equals(idTec)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Seguridad", "No puede modificar esta orden."));
                return;
            }
            db.setEstado(ordenEdicion.getEstado());
            db.setDescripcion(ordenEdicion.getDescripcion());
            ordenDAO.actualizar(db);
            cargarLista();
            ordenEdicion = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Orden actualizada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void cargarReparaciones(OrdenTrabajo fila) {
        try {
            Integer idTec = loginBean.getIdTecnicoAsociado();
            if (idTec == null || !ordenDAO.ordenAsignadaATecnico(fila.getIdOrden(), idTec)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Seguridad", "Orden no válida."));
                reparacionesOrden = List.of();
                ordenReparaciones = null;
                return;
            }
            ordenReparaciones = fila;
            reparacionesOrden = reparacionDAO.listarPorOrden(fila.getIdOrden());
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditarReparacion(Reparacion r) {
        try {
            Integer idTec = loginBean.getIdTecnicoAsociado();
            if (!ordenDAO.ordenAsignadaATecnico(r.getIdOrden(), idTec)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Seguridad", "No puede editar esta reparación."));
                reparacionEdicion = null;
                return;
            }
            Reparacion db = reparacionDAO.buscarPorId(r.getIdReparacion());
            reparacionEdicion = db != null ? db : r;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void guardarReparacionTecnico() {
        try {
            Integer idTec = loginBean.getIdTecnicoAsociado();
            if (reparacionEdicion == null || idTec == null) {
                return;
            }
            if (!ordenDAO.ordenAsignadaATecnico(reparacionEdicion.getIdOrden(), idTec)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Seguridad", "No puede modificar esta reparación."));
                return;
            }
            if (reparacionEdicion.getDescripcion() == null || reparacionEdicion.getDescripcion().isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "La descripción es obligatoria."));
                return;
            }
            reparacionDAO.actualizar(reparacionEdicion);
            reparacionesOrden = reparacionDAO.listarPorOrden(reparacionEdicion.getIdOrden());
            reparacionEdicion = null;
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Reparación actualizada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public List<OrdenTrabajo> getLista() {
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public List<String> getEstadosOrden() {
        return Arrays.asList(EstadosOrden.PENDIENTE, EstadosOrden.EN_PROCESO, EstadosOrden.FINALIZADO);
    }

    public String getFiltroPlaca() {
        return filtroPlaca;
    }

    public void setFiltroPlaca(String filtroPlaca) {
        this.filtroPlaca = filtroPlaca;
    }

    public String getFiltroEstado() {
        return filtroEstado;
    }

    public void setFiltroEstado(String filtroEstado) {
        this.filtroEstado = filtroEstado;
    }

    public OrdenTrabajo getOrdenEdicion() {
        return ordenEdicion;
    }

    public void setOrdenEdicion(OrdenTrabajo ordenEdicion) {
        this.ordenEdicion = ordenEdicion;
    }

    public List<Reparacion> getReparacionesOrden() {
        return reparacionesOrden;
    }

    public Reparacion getReparacionEdicion() {
        return reparacionEdicion;
    }

    public void setReparacionEdicion(Reparacion reparacionEdicion) {
        this.reparacionEdicion = reparacionEdicion;
    }

    public OrdenTrabajo getOrdenReparaciones() {
        return ordenReparaciones;
    }

    public String claseBadgeEstado(String estado) {
        if (estado == null) {
            return "badge-estado badge-pendiente";
        }
        String e = estado.toLowerCase();
        if (e.contains("pendiente")) {
            return "badge-estado badge-pendiente";
        }
        if (e.contains("proceso")) {
            return "badge-estado badge-proceso";
        }
        if (e.contains("finaliz")) {
            return "badge-estado badge-finalizado";
        }
        return "badge-estado badge-pendiente";
    }
}
