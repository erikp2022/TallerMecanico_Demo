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
            enviarError("Error al cargar lista", e.getMessage());
        }
    }

    public void prepararNuevo() {
        nuevo = new Vehiculo();
    }

    public void guardar() {
        try {
            if (nuevo.getPlaca() == null || nuevo.getPlaca().isBlank() || nuevo.getIdCliente() == null) {
                enviarAviso("Validación", "Placa y cliente son obligatorios.");
                return;
            }
            String pl = nuevo.getPlaca().trim().toUpperCase();
            if (!PLACA.matcher(pl).matches()) {
                enviarAviso("Validación", "Formato inválido (ej. HBU2574).");
                return;
            }
            if (dao.existePlaca(pl, null)) {
                enviarAviso("Validación", "La placa ya existe en el sistema.");
                return;
            }
            nuevo.setPlaca(pl);
            dao.insertar(nuevo);
            cargarLista();
            enviarInfo("Guardado", "Vehículo registrado correctamente.");
        } catch (Exception e) {
            enviarError("Error al guardar", e.getMessage());
        }
    }

    public void prepararEditar(Vehiculo v) {
        seleccionado = v;
    }

    public void actualizar() {
        try {
            if (seleccionado == null) return;
            String pl = seleccionado.getPlaca().trim().toUpperCase();
            if (!PLACA.matcher(pl).matches()) {
                enviarAviso("Validación", "Formato de placa inválido.");
                return;
            }
            if (dao.existePlaca(pl, seleccionado.getIdVehiculo())) {
                enviarAviso("Validación", "Otro vehículo ya usa esta placa.");
                return;
            }
            seleccionado.setPlaca(pl);
            dao.actualizar(seleccionado);
            cargarLista();
            enviarInfo("Actualizado", "Datos del vehículo modificados.");
        } catch (Exception e) {
            enviarError("Error al actualizar", e.getMessage());
        }
    }

    public void eliminar(Vehiculo v) {
        try {
            // VALIDACIÓN DE INTEGRIDAD
            if (dao.tieneOrdenesAsociadas(v.getIdVehiculo())) {
                enviarAviso("No se puede eliminar",
                        "Este vehículo tiene órdenes de trabajo registradas. Elimine primero las órdenes.");
                return;
            }

            dao.eliminar(v.getIdVehiculo());
            cargarLista();
            enviarInfo("Eliminado", "Vehículo borrado exitosamente.");
        } catch (Exception e) {
            enviarError("Error al eliminar", e.getMessage());
        }
    }

    // Métodos de utilidad para mensajes
    private void enviarInfo(String tit, String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, tit, msg));
    }
    private void enviarAviso(String tit, String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, tit, msg));
    }
    private void enviarError(String tit, String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, tit, msg));
    }

    // Getters y Setters
    public List<Vehiculo> getLista() { if (lista == null) cargarLista(); return lista; }
    public List<Cliente> getClientes() { if (clientes == null) cargarLista(); return clientes; }
    public Vehiculo getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Vehiculo seleccionado) { this.seleccionado = seleccionado; }
    public Vehiculo getNuevo() { return nuevo; }
    public void setNuevo(Vehiculo nuevo) { this.nuevo = nuevo; }
}