package com.taller.mecanico.controller;

import com.taller.mecanico.dao.OrdenTrabajoDAO;
import com.taller.mecanico.dao.ReparacionDAO;
import com.taller.mecanico.dao.RepuestoDAO; // Agregado
import com.taller.mecanico.model.OrdenTrabajo;
import com.taller.mecanico.model.Reparacion;
import com.taller.mecanico.model.Repuesto; // Agregado
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
    private final RepuestoDAO repuestoDAO = new RepuestoDAO(); // Instancia del DAO de repuestos

    private List<Reparacion> lista;
    private Reparacion seleccionado;
    private Reparacion nuevo;
    private List<OrdenTrabajo> ordenesDisponibles;
    private List<Repuesto> repuestosDisponibles; // Para el combo de repuestos

    // Variables para la "línea de repuesto" en la reparación
    private Integer idRepuestoSeleccionado;
    private Integer cantidadRepuesto;

    public void cargarLista() {
        try {
            repuestosDisponibles = repuestoDAO.listarTodos(); // Cargar repuestos siempre
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

    public void prepararNuevo() {
        nuevo = new Reparacion();
        nuevo.setEstado(EstadosOrden.PENDIENTE);
        idRepuestoSeleccionado = null;
        cantidadRepuesto = 1;
        cargarOrdenesParaSelect();
    }

    public void guardar() {
        try {
            if (nuevo.getDescripcion() == null || nuevo.getDescripcion().isBlank() || nuevo.getIdOrden() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Descripción y orden son obligatorios."));
                return;
            }

            // Lógica para descontar stock si se seleccionó un repuesto
            if (idRepuestoSeleccionado != null && cantidadRepuesto != null && cantidadRepuesto > 0) {
                int stockActual = repuestoDAO.obtenerStock(idRepuestoSeleccionado);
                if (stockActual < cantidadRepuesto) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Stock Insuficiente", "Solo hay " + stockActual + " unidades."));
                    return;
                }
               //descontamos el estok
                repuestoDAO.ajustarStock(idRepuestoSeleccionado, -cantidadRepuesto);

                // aqui este se agrega una descripcion auto
                Repuesto rep = repuestoDAO.buscarPorId(idRepuestoSeleccionado);
                nuevo.setDescripcion(nuevo.getDescripcion() + " (Repuesto: " + rep.getNombre() + " x" + cantidadRepuesto + ")");
            }

            dao.insertar(nuevo);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Reparación registrada y stock actualizado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    private void cargarOrdenesParaSelect() {
        try {
            if (loginBean != null && loginBean.isTecnico()) {
                ordenesDisponibles = ordenDAO.listarConDetalleFiltrado(null, null, loginBean.getIdTecnicoAsociado());
            } else {
                ordenesDisponibles = ordenDAO.listarConDetalle();
            }
        } catch (Exception e) {
            ordenesDisponibles = List.of();
        }
    }

    // Getters y Setters para las vistas
    public List<Repuesto> getRepuestosDisponibles() { return repuestosDisponibles; }
    public Integer getIdRepuestoSeleccionado() { return idRepuestoSeleccionado; }
    public void setIdRepuestoSeleccionado(Integer idRepuestoSeleccionado) { this.idRepuestoSeleccionado = idRepuestoSeleccionado; }
    public Integer getCantidadRepuesto() { return cantidadRepuesto; }
    public void setCantidadRepuesto(Integer cantidadRepuesto) { this.cantidadRepuesto = cantidadRepuesto; }
    public List<Reparacion> getLista() { if (lista == null) cargarLista(); return lista; }
    public List<String> getEstados() { return Arrays.asList(EstadosOrden.PENDIENTE, EstadosOrden.EN_PROCESO, EstadosOrden.FINALIZADO); }
    public List<OrdenTrabajo> getOrdenesDisponibles() { if (ordenesDisponibles == null) cargarOrdenesParaSelect(); return ordenesDisponibles; }
    public Reparacion getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Reparacion seleccionado) { this.seleccionado = seleccionado; }
    public Reparacion getNuevo() { return nuevo; }
    public void setNuevo(Reparacion nuevo) { this.nuevo = nuevo; }
    public void prepararEditar(Reparacion r) { seleccionado = r; cargarOrdenesParaSelect(); }
    public void actualizar() { /* lógica similar al guardar */ }
    public void eliminar(Reparacion r) { /* lógica de eliminar */ }
}