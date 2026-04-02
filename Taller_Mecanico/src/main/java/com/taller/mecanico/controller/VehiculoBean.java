package com.taller.mecanico.controller;

import com.taller.mecanico.dao.ClienteDAO;
import com.taller.mecanico.dao.VehiculoDAO;
import com.taller.mecanico.model.Cliente;
import com.taller.mecanico.model.Vehiculo;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

@Named("vehiculoBean")
@ViewScoped
public class VehiculoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern PLACA = Pattern.compile("^[A-Za-z]{3}[0-9]{4}$");

    private final VehiculoDAO dao = new VehiculoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private List<Vehiculo> lista;
    private List<Cliente> clientes;
    private Vehiculo seleccionado;
    private Vehiculo nuevo;

    public void cargarLista() {
        try {
            lista = dao.listarTodos();
            clientes = clienteDAO.listarTodos();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararNuevo() {
        nuevo = new Vehiculo();
        if (clientes == null) {
            try {
                clientes = clienteDAO.listarTodos();
            } catch (Exception ignored) {
            }
        }
    }

    public void guardar() {
        try {
            if (nuevo.getPlaca() == null || nuevo.getPlaca().isBlank() || nuevo.getIdCliente() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Placa y cliente son obligatorios."));
                return;
            }
            String pl = nuevo.getPlaca().trim().toUpperCase();
            if (!PLACA.matcher(pl).matches()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                                "La placa debe tener 3 letras y 4 números (ej. HBU2574)."));
                return;
            }
            nuevo.setPlaca(pl);
            if (dao.existePlaca(pl, null)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                                "Ya existe un vehículo con esa placa."));
                return;
            }
            dao.insertar(nuevo);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Vehículo creado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditar(Vehiculo v) {
        seleccionado = v;
        if (clientes == null) {
            try {
                clientes = clienteDAO.listarTodos();
            } catch (Exception ignored) {
            }
        }
    }

    public void actualizar() {
        try {
            if (seleccionado == null) {
                return;
            }
            if (seleccionado.getPlaca() != null) {
                String pl = seleccionado.getPlaca().trim().toUpperCase();
                if (!PLACA.matcher(pl).matches()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                                    "La placa debe tener 3 letras y 4 números (ej. HBU2574)."));
                    return;
                }
                seleccionado.setPlaca(pl);
            }
            if (dao.existePlaca(seleccionado.getPlaca(), seleccionado.getIdVehiculo())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                                "Ya existe otro vehículo con esa placa."));
                return;
            }
            dao.actualizar(seleccionado);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Actualizado", "Vehículo modificado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void eliminar(Vehiculo v) {
        try {
            dao.eliminar(v.getIdVehiculo());
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Eliminado", "Vehículo eliminado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public List<Vehiculo> getLista() {
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public void setLista(List<Vehiculo> lista) {
        this.lista = lista;
    }

    public List<Cliente> getClientes() {
        if (clientes == null) {
            try {
                clientes = clienteDAO.listarTodos();
            } catch (Exception ignored) {
            }
        }
        return clientes;
    }

    public Vehiculo getSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(Vehiculo seleccionado) {
        this.seleccionado = seleccionado;
    }

    public Vehiculo getNuevo() {
        return nuevo;
    }

    public void setNuevo(Vehiculo nuevo) {
        this.nuevo = nuevo;
    }
}
