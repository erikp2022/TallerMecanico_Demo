package com.taller.mecanico.controller;

import com.taller.mecanico.dao.*;
import com.taller.mecanico.model.*;
import com.taller.mecanico.util.EstadosOrden;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Named("ordenBean")
@ViewScoped
public class OrdenBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    private final OrdenTrabajoDAO ordenDAO = new OrdenTrabajoDAO();
    private final TecnicoDAO tecnicoDAO = new TecnicoDAO();
    private final VehiculoDAO vehiculoDAO = new VehiculoDAO();
    private final OrdenRepuestoDAO ordenRepuestoDAO = new OrdenRepuestoDAO();
    private final RepuestoDAO repuestoDAO = new RepuestoDAO();

    private List<OrdenTrabajo> lista;
    private String filtroPlaca;
    private String filtroEstado;
    private Integer filtroIdTecnico;

    private OrdenTrabajo seleccionado;
    private OrdenTrabajo nuevo;

    private List<OrdenRepuesto> detallesRepuesto;
    private Integer idRepuestoLinea;
    private Integer cantidadLinea;

    private final ReparacionDAO reparacionDAO = new ReparacionDAO();
    private List<Reparacion> reparacionesOrden;
    private Integer idRepuestoReparacion;
    private Integer cantidadReparacion;
    private String descripcionReparacion;

    public void cargarLista() {
        try {
            if (loginBean == null || !loginBean.isAdmin()) {
                lista = List.of();
                return;
            }
            Integer idTec = filtroIdTecnico;
            lista = ordenDAO.listarConDetalleFiltrado(filtroPlaca, filtroEstado, idTec);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void limpiarFiltros() {
        filtroPlaca = null;
        filtroEstado = null;
        filtroIdTecnico = null;
        cargarLista();
    }

    public void prepararNuevo() {
        nuevo = new OrdenTrabajo();
        nuevo.setFechaIngreso(LocalDate.now());
        nuevo.setEstado(EstadosOrden.PENDIENTE);
        if (loginBean != null && loginBean.getUsuario() != null) {
            nuevo.setIdUsuario(loginBean.getUsuario().getIdUsuario());
        }
    }

    public void guardar() {
        try {
            if (nuevo.getIdVehiculo() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Seleccione un vehículo."));
                return;
            }
            if (nuevo.getIdTecnico() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Debe asignar un técnico a la orden."));
                return;
            }
            if (nuevo.getIdUsuario() == null && loginBean != null && loginBean.getUsuario() != null) {
                nuevo.setIdUsuario(loginBean.getUsuario().getIdUsuario());
            }
            ordenDAO.insertar(nuevo);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Orden creada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditar(OrdenTrabajo o) {
        try {
            seleccionado = ordenDAO.buscarPorId(o.getIdOrden());
            if (seleccionado == null) {
                seleccionado = o;
            }
        } catch (Exception e) {
            seleccionado = o;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void actualizar() {
        try {
            if (seleccionado == null) {
                return;
            }
            if (seleccionado.getIdTecnico() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Debe asignar un técnico."));
                return;
            }
            ordenDAO.actualizar(seleccionado);
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Actualizado", "Orden modificada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    // Intenta eliminar — valida estado y si tiene reparaciones
    public void eliminar(OrdenTrabajo o) {
        try {
            // Solo se pueden eliminar órdenes Finalizadas
            if (!"Finalizado".equalsIgnoreCase(o.getEstado())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "No se puede eliminar",
                                "Solo se pueden eliminar órdenes con estado Finalizado."));
                return;
            }

            // Guardar la orden seleccionada para el diálogo secundario
            seleccionado = o;

            // ¿Tiene reparaciones?
            if (ordenDAO.tieneReparaciones(o.getIdOrden())) {
                // Mostrar diálogo secundario desde el cliente
                FacesContext.getCurrentInstance()
                        .getPartialViewContext()
                        .getRenderIds()
                        .add("formDlgConfirmRep:panelConfirmRep");
                // Enviamos señal al cliente para abrir el diálogo
                PrimeFaces.current().executeScript("PF('dlgConfirmRep').show()");
                return;
            }

            // Sin reparaciones → elimina directo
            ordenDAO.eliminarOrdenRepuestos(o.getIdOrden());
            ordenDAO.eliminar(o.getIdOrden());
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Eliminado", "Orden eliminada correctamente."));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    // Elimina reparaciones + orden (llamado desde el diálogo secundario)
    public void eliminarConReparaciones() {
        try {
            if (seleccionado == null) return;

            ordenDAO.eliminarReparacionesDeOrden(seleccionado.getIdOrden());
            ordenDAO.eliminarOrdenRepuestos(seleccionado.getIdOrden());
            ordenDAO.eliminar(seleccionado.getIdOrden());
            seleccionado = null;
            cargarLista();

            PrimeFaces.current().executeScript("PF('dlgConfirmRep').hide()");
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Eliminado", "Orden y reparaciones eliminadas correctamente."));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void cargarDetalleRepuestos(OrdenTrabajo o) {
        seleccionado = o;
        try {
            detallesRepuesto = ordenRepuestoDAO.listarPorOrden(o.getIdOrden());
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void agregarLineaRepuesto() {
        if (seleccionado == null) {
            return;
        }
        try {
            if (idRepuestoLinea == null || cantidadLinea == null || cantidadLinea <= 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Repuesto y cantidad válidos requeridos."));
                return;
            }
            ordenRepuestoDAO.insertarLinea(seleccionado.getIdOrden(), idRepuestoLinea, cantidadLinea, repuestoDAO);
            detallesRepuesto = ordenRepuestoDAO.listarPorOrden(seleccionado.getIdOrden());
            idRepuestoLinea = null;
            cantidadLinea = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Repuesto", "Línea agregada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void eliminarLineaRepuesto(OrdenRepuesto linea) {
        try {
            ordenRepuestoDAO.eliminarLinea(linea.getIdDetalle(), repuestoDAO);
            if (seleccionado != null) {
                detallesRepuesto = ordenRepuestoDAO.listarPorOrden(seleccionado.getIdOrden());
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    // Carga las reparaciones de la orden seleccionada
    public void cargarReparacionesOrden(OrdenTrabajo o) {
        seleccionado = o;
        try {
            reparacionesOrden = reparacionDAO.listarPorOrden(o.getIdOrden());
            idRepuestoReparacion = null;
            cantidadReparacion = 1;
            descripcionReparacion = null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    // Agrega una reparación a la orden, descontando stock si hay repuesto
    public void agregarReparacion() {
        try {
            if (seleccionado == null) return;

            if (descripcionReparacion == null || descripcionReparacion.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Validación", "La descripción es obligatoria."));
                return;
            }

            Reparacion r = new Reparacion();
            r.setIdOrden(seleccionado.getIdOrden());
            r.setEstado("Pendiente");

            // Si se seleccionó un repuesto, descontar stock y agregar a descripción
            if (idRepuestoReparacion != null && cantidadReparacion != null && cantidadReparacion > 0) {
                int stock = repuestoDAO.obtenerStock(idRepuestoReparacion);
                if (stock < cantidadReparacion) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                    "Stock insuficiente", "Solo hay " + stock + " unidades disponibles."));
                    return;
                }
                repuestoDAO.ajustarStock(idRepuestoReparacion, -cantidadReparacion);
                Repuesto rep = repuestoDAO.buscarPorId(idRepuestoReparacion);
                r.setDescripcion(descripcionReparacion.trim() +
                        " (Repuesto: " + rep.getNombre() + " x" + cantidadReparacion + ")");
            } else {
                r.setDescripcion(descripcionReparacion.trim());
            }

            reparacionDAO.insertar(r);
            reparacionesOrden = reparacionDAO.listarPorOrden(seleccionado.getIdOrden());

            // Limpiar campos
            idRepuestoReparacion = null;
            cantidadReparacion = 1;
            descripcionReparacion = null;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Guardado", "Reparación registrada correctamente."));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    // Elimina una reparación de la orden
    public void eliminarReparacion(Reparacion r) {
        try {
            reparacionDAO.eliminar(r.getIdReparacion());
            if (seleccionado != null) {
                reparacionesOrden = reparacionDAO.listarPorOrden(seleccionado.getIdOrden());
            }
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Eliminado", "Reparación eliminada."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public String initPaginaAdmin() {
        if (loginBean == null) {
            return null;
        }
        String r = loginBean.exigirAdmin();
        if (r != null) {
            return r;
        }
        cargarLista();
        return null;
    }

    public List<OrdenTrabajo> getLista() {
        if (loginBean != null && !loginBean.isAdmin()) {
            return List.of();
        }
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public List<Tecnico> getTecnicos() {
        try {
            return tecnicoDAO.listarTodos();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Vehiculo> getVehiculos() {
        try {
            return vehiculoDAO.listarTodos();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<String> getEstadosOrden() {
        return Arrays.asList(EstadosOrden.PENDIENTE, EstadosOrden.EN_PROCESO, EstadosOrden.FINALIZADO);
    }

    public List<Repuesto> getRepuestos() {
        try {
            return repuestoDAO.listarTodos();
        } catch (Exception e) {
            return List.of();
        }
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

    public Integer getFiltroIdTecnico() {
        return filtroIdTecnico;
    }

    public void setFiltroIdTecnico(Integer filtroIdTecnico) {
        this.filtroIdTecnico = filtroIdTecnico;
    }

    public OrdenTrabajo getSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(OrdenTrabajo seleccionado) {
        this.seleccionado = seleccionado;
    }

    public OrdenTrabajo getNuevo() {
        return nuevo;
    }

    public void setNuevo(OrdenTrabajo nuevo) {
        this.nuevo = nuevo;
    }

    public List<OrdenRepuesto> getDetallesRepuesto() {
        return detallesRepuesto;
    }

    public Integer getIdRepuestoLinea() {
        return idRepuestoLinea;
    }

    public void setIdRepuestoLinea(Integer idRepuestoLinea) {
        this.idRepuestoLinea = idRepuestoLinea;
    }

    public Integer getCantidadLinea() {
        return cantidadLinea;
    }

    public void setCantidadLinea(Integer cantidadLinea) {
        this.cantidadLinea = cantidadLinea;
    }


    public List<Reparacion> getReparacionesOrden() {
        return reparacionesOrden;
    }

    public Integer getIdRepuestoReparacion() {
        return idRepuestoReparacion;
    }

    public void setIdRepuestoReparacion(Integer v) {
        this.idRepuestoReparacion = v;
    }

    public Integer getCantidadReparacion() {
        return cantidadReparacion;
    }

    public void setCantidadReparacion(Integer v) {
        this.cantidadReparacion = v;
    }

    public String getDescripcionReparacion() {
        return descripcionReparacion;
    }

    public void setDescripcionReparacion(String v) {
        this.descripcionReparacion = v;
    }


    /**
     * Clase CSS para badge de estado en tablas.
     */
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