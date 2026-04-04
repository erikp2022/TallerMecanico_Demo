package com.taller.mecanico.controller;

import com.taller.mecanico.dao.UsuarioDAO;
import com.taller.mecanico.model.Usuario;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Permitimos que el usuario cambie su propia contra.
 */
@Named("cambiarContrasenaBean")
@ViewScoped
public class CambiarContrasenaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MIN_LONGITUD = 4;

    @Inject
    private LoginBean loginBean;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private String contrasenaActual;
    private String contrasenaNueva;
    private String contrasenaNuevaRepetir;

    public void guardar() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (loginBean == null || !loginBean.isLogueado()) {
            return;
        }
        Usuario u = loginBean.getUsuario();
        if (u.getCorreo() == null || u.getIdUsuario() == null) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Sesión", "Datos de usuario incompletos."));
            return;
        }
        if (contrasenaActual == null || contrasenaActual.isBlank()) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Indique su contraseña actual."));
            return;
        }
        if (contrasenaNueva == null || contrasenaNueva.isBlank()) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Indique la nueva contraseña."));
            return;
        }
        if (contrasenaNueva.length() < MIN_LONGITUD) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                    "La nueva contraseña debe tener al menos " + MIN_LONGITUD + " caracteres."));
            return;
        }
        if (!contrasenaNueva.equals(contrasenaNuevaRepetir)) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                    "La nueva contraseña y la repetición no coinciden."));
            return;
        }
        if (contrasenaNueva.equals(contrasenaActual)) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                    "La nueva contraseña debe ser distinta de la actual."));
            return;
        }
        try {
            Usuario verificado = usuarioDAO.autenticar(u.getCorreo().trim(), contrasenaActual);
            if (verificado == null || !verificado.getIdUsuario().equals(u.getIdUsuario())) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Contraseña actual",
                        "La contraseña actual no es correcta."));
                return;
            }
            usuarioDAO.actualizarContrasena(u.getIdUsuario(), contrasenaNueva);
            loginBean.refrescarUsuarioEnSesion();
            contrasenaActual = null;
            contrasenaNueva = null;
            contrasenaNuevaRepetir = null;
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Listo",
                    "Su contraseña se ha actualizado correctamente."));
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void limpiar() {
        contrasenaActual = null;
        contrasenaNueva = null;
        contrasenaNuevaRepetir = null;
    }

    public String getContrasenaActual() {
        return contrasenaActual;
    }

    public void setContrasenaActual(String contrasenaActual) {
        this.contrasenaActual = contrasenaActual;
    }

    public String getContrasenaNueva() {
        return contrasenaNueva;
    }

    public void setContrasenaNueva(String contrasenaNueva) {
        this.contrasenaNueva = contrasenaNueva;
    }

    public String getContrasenaNuevaRepetir() {
        return contrasenaNuevaRepetir;
    }

    public void setContrasenaNuevaRepetir(String contrasenaNuevaRepetir) {
        this.contrasenaNuevaRepetir = contrasenaNuevaRepetir;
    }
}
