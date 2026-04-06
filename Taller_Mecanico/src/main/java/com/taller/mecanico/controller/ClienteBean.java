package com.taller.mecanico.controller;

import com.taller.mecanico.dao.ClienteDAO;
import com.taller.mecanico.model.Cliente;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("clienteBean")
@ViewScoped
public class ClienteBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ClienteDAO dao = new ClienteDAO();
    private List<Cliente> lista;
    private Cliente seleccionado;
    private Cliente nuevo;

    public void cargarLista() {
        try {
            lista = dao.listarTodos();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararNuevo() {
        nuevo = new Cliente();
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
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Cliente creado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditar(Cliente c) {
        seleccionado = c;
    }

    public void actualizar() {
        try {
            if (seleccionado == null) {
                return;
            }
            dao.actualizar(seleccionado);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Actualizado", "Cliente modificado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void eliminar(Cliente c) {
        try {
            // Verificar si tiene vehículos registrados
            if (dao.tieneVehiculos(c.getIdCliente())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "No se puede eliminar",
                                "Este cliente tiene vehículos registrados. " +
                                        "Para eliminarlo debe eliminar primero sus vehículos, " +
                                        "luego las órdenes de trabajo y reparaciones asociadas."));
                return;
            }

            dao.eliminar(c.getIdCliente());
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Eliminado", "Cliente eliminado correctamente."));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", e.getMessage()));
        }
    }

    public List<Cliente> getLista() {
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public void setLista(List<Cliente> lista) {
        this.lista = lista;
    }

    public Cliente getSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(Cliente seleccionado) {
        this.seleccionado = seleccionado;
    }

    public Cliente getNuevo() {
        return nuevo;
    }

    public void setNuevo(Cliente nuevo) {
        this.nuevo = nuevo;
    }
}
