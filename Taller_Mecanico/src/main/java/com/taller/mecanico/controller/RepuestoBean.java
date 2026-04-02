package com.taller.mecanico.controller;

import com.taller.mecanico.dao.RepuestoDAO;
import com.taller.mecanico.model.Repuesto;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Named("repuestoBean")
@ViewScoped
public class RepuestoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final RepuestoDAO dao = new RepuestoDAO();
    private List<Repuesto> lista;
    private Repuesto seleccionado;
    private Repuesto nuevo;

    public void cargarLista() {
        try {
            lista = dao.listarTodos();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararNuevo() {
        nuevo = new Repuesto();
        nuevo.setPrecio(BigDecimal.ZERO);
        nuevo.setStock(0);
    }

    public void guardar() {
        try {
            if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "El nombre es obligatorio."));
                return;
            }
            dao.insertar(nuevo);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Repuesto creado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditar(Repuesto r) {
        seleccionado = r;
    }

    public void actualizar() {
        try {
            if (seleccionado == null) {
                return;
            }
            dao.actualizar(seleccionado);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Actualizado", "Repuesto modificado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void eliminar(Repuesto r) {
        try {
            dao.eliminar(r.getIdRepuesto());
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Eliminado", "Repuesto eliminado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public List<Repuesto> getLista() {
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public Repuesto getSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(Repuesto seleccionado) {
        this.seleccionado = seleccionado;
    }

    public Repuesto getNuevo() {
        return nuevo;
    }

    public void setNuevo(Repuesto nuevo) {
        this.nuevo = nuevo;
    }
}
