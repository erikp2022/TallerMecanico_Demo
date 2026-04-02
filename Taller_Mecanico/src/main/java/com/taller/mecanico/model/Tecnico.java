package com.taller.mecanico.model;

import java.io.Serializable;

public class Tecnico implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idTecnico;
    private String nombre;
    /** Correo único; coincide con usuarios.correo para el login del técnico. */
    private String correo;
    private String especialidad;
    private String estado;

    public Integer getIdTecnico() {
        return idTecnico;
    }

    public void setIdTecnico(Integer idTecnico) {
        this.idTecnico = idTecnico;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
