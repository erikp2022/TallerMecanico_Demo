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

@Named("reparacionBean")
@ViewScoped
public class ReparacionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    private final ReparacionDAO dao = new ReparacionDAO();
    private final OrdenTrabajoDAO ordenDAO = new OrdenTrabajoDAO();

    private List<Reparacion> lista;
    private Reparacion seleccionado;
    private Reparacion nuevo;
    private List<OrdenTrabajo> ordenesDisponibles;

    public void cargarLista() {
        try {
            if (loginBean != null && loginBean.isTecnico()) {
                if (loginBean.getIdTecnicoAsociado() == null) {
                    lista = List.of();
                    return;
                }
                lista = dao.listarPorTecnico(loginBean.getIdTecnicoAsociado());
            } else if (loginBean != null && loginBean.isAdmin()) {
                lista = dao.listarTodas();
            } else {
                lista = List.of();
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    private void cargarOrdenesParaSelect() {
        try {
            if (loginBean != null && loginBean.isTecnico()) {
                if (loginBean.getIdTecnicoAsociado() == null) {
                    ordenesDisponibles = List.of();
                    return;
                }
                ordenesDisponibles = ordenDAO.listarConDetalleFiltrado(null, null, loginBean.getIdTecnicoAsociado());
            } else if (loginBean != null && loginBean.isAdmin()) {
                ordenesDisponibles = ordenDAO.listarConDetalle();
            } else {
                ordenesDisponibles = List.of();
            }
        } catch (Exception e) {
            ordenesDisponibles = List.of();
        }
    }

    public void prepararNuevo() {
        nuevo = new Reparacion();
        nuevo.setEstado(EstadosOrden.PENDIENTE);
        cargarOrdenesParaSelect();
    }

    public void guardar() {
        try {
            if (nuevo.getDescripcion() == null || nuevo.getDescripcion().isBlank() || nuevo.getIdOrden() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Descripción y orden son obligatorios."));
                return;
            }
            if (loginBean.isTecnico()) {
                Integer idTec = loginBean.getIdTecnicoAsociado();
                if (idTec == null || !ordenDAO.ordenAsignadaATecnico(nuevo.getIdOrden(), idTec)) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Seguridad", "No puede registrar reparaciones en esa orden."));
                    return;
                }
            }
            dao.insertar(nuevo);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Reparación registrada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditar(Reparacion r) {
        seleccionado = r;
        cargarOrdenesParaSelect();
    }

    public void actualizar() {
        try {
            if (seleccionado == null) {
                return;
            }
            if (loginBean.isTecnico()) {
                Integer idTec = loginBean.getIdTecnicoAsociado();
                if (idTec == null || !ordenDAO.ordenAsignadaATecnico(seleccionado.getIdOrden(), idTec)) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Seguridad", "No puede modificar esta reparación."));
                    return;
                }
            }
            dao.actualizar(seleccionado);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Actualizado", "Reparación modificada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void eliminar(Reparacion r) {
        try {
            if (!loginBean.isAdmin()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Permisos", "Solo el administrador puede eliminar reparaciones."));
                return;
            }
            dao.eliminar(r.getIdReparacion());
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Eliminado", "Reparación eliminada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public List<Reparacion> getLista() {
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public List<String> getEstados() {
        return Arrays.asList(EstadosOrden.PENDIENTE, EstadosOrden.EN_PROCESO, EstadosOrden.FINALIZADO);
    }

    public List<OrdenTrabajo> getOrdenesDisponibles() {
        if (ordenesDisponibles == null) {
            cargarOrdenesParaSelect();
        }
        return ordenesDisponibles;
    }

    public Reparacion getSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(Reparacion seleccionado) {
        this.seleccionado = seleccionado;
    }

    public Reparacion getNuevo() {
        return nuevo;
    }

    public void setNuevo(Reparacion nuevo) {
        this.nuevo = nuevo;
    }
}
