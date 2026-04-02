package com.taller.mecanico.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Repuesto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idRepuesto;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;

    public Integer getIdRepuesto() {
        return idRepuesto;
    }

    public void setIdRepuesto(Integer idRepuesto) {
        this.idRepuesto = idRepuesto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
