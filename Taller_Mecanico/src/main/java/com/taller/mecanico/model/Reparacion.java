package com.taller.mecanico.model;

import java.io.Serializable;

public class Reparacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idReparacion;
    private String descripcion;
    private String estado;
    private Integer idOrden;
    // Agrega este campo y sus getter/setter
    private String nombreCliente;

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }



    public Integer getIdReparacion() {
        return idReparacion;
    }

    public void setIdReparacion(Integer idReparacion) {
        this.idReparacion = idReparacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(Integer idOrden) {
        this.idOrden = idOrden;
    }
}
