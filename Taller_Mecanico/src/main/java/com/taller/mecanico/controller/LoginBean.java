package com.taller.mecanico.controller;

import com.taller.mecanico.dao.UsuarioDAO;
import com.taller.mecanico.model.Usuario;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * validamos que sea admin o tecnico segun el rol .
 */
@Named("loginBean")
@SessionScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String correo;
    private String contrasena;
    private Usuario usuario;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public String iniciarSesion() {
        try {
            Usuario u = usuarioDAO.autenticar(correo, contrasena);
            if (u == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Acceso denegado",
                                "Correo o contraseña incorrectos, cuenta o técnico inactivo, o rol no permitido."));
                return null;
            }
            this.usuario = u;
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("usuario", u);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("idTecnicoAsociado", u.getIdTecnico());
            if (isAdmin()) {
                return "/views/dashboard.xhtml?faces-redirect=true";
            }
            if (isTecnico()) {
                return "/views/mis_ordenes.xhtml?faces-redirect=true";
            }
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Acceso denegado", "Rol no reconocido."));
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }

    public String cerrarSesion() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        usuario = null;
        return "/views/login.xhtml?faces-redirect=true";
    }

    /**
     * Si ya hay sesión, evita mostrar el login de nuevo.
     */
    public String comprobarYaLogueado() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        restaurarDesdeSesion(ctx);
        if (usuario == null) {
            return null;
        }
        if (isAdmin()) {
            return "/views/dashboard.xhtml?faces-redirect=true";
        }
        if (isTecnico()) {
            return "/views/mis_ordenes.xhtml?faces-redirect=true";
        }
        return null;
    }

    /**
     * Uso en f:viewAction desde plantillas: redirige a login si no hay sesión.
     */
    public String verificarSesion() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        String viewId = ctx.getViewRoot().getViewId();
        if (viewId != null && viewId.contains("login")) {
            return null;
        }
        restaurarDesdeSesion(ctx);
        if (usuario == null) {
            return "/views/login.xhtml?faces-redirect=true";
        }
        try {
            Usuario fresh = usuarioDAO.recargarSiSesionValida(usuario.getIdUsuario());
            if (fresh == null) {
                ctx.getExternalContext().invalidateSession();
                usuario = null;
                return "/views/login.xhtml?faces-redirect=true";
            }
            fresh.setContrasena(null);
            usuario = fresh;
            ctx.getExternalContext().getSessionMap().put("usuario", fresh);
            ctx.getExternalContext().getSessionMap().put("idTecnicoAsociado", fresh.getIdTecnico());
        } catch (SQLException ignored) {
            // Si Access falla momentáneamente, no expulsar al usuario de la sesión
        }
        return null;
    }

    /** Vistas solo administrador (desde f:viewAction en páginas restringidas). */
    public String exigirAdmin() {
        if (!isAdmin()) {
            return isTecnico() ? "/views/mis_ordenes.xhtml?faces-redirect=true" : "/views/dashboard.xhtml?faces-redirect=true";
        }
        return null;
    }

    /** Vista exclusiva de técnico. */
    public String exigirTecnico() {
        if (!isTecnico()) {
            return "/views/dashboard.xhtml?faces-redirect=true";
        }
        return null;
    }

    private void restaurarDesdeSesion(FacesContext ctx) {
        if (usuario != null) {
            return;
        }
        Object u = ctx.getExternalContext().getSessionMap().get("usuario");
        if (u instanceof Usuario) {
            usuario = (Usuario) u;
        }
    }

    public boolean isLogueado() {
        return usuario != null;
    }

    public boolean isAdmin() {
        return usuario != null && usuario.getRol() != null
                && "admin".equalsIgnoreCase(usuario.getRol().trim());
    }

    public boolean isTecnico() {
        return usuario != null && usuario.getRol() != null
                && "tecnico".equalsIgnoreCase(usuario.getRol().trim());
    }

    /**
     * Identificador del técnico en tabla técnicos (solo usuarios con rol tecnico).
     */
    public Integer getIdTecnicoAsociado() {
        return usuario != null ? usuario.getIdTecnico() : null;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * Tras cambiar la contraseña en BD, recarga el usuario en sesión sin guardar la clave en memoria.
     */
    public void refrescarUsuarioEnSesion() {
        if (usuario == null || usuario.getIdUsuario() == null) {
            return;
        }
        try {
            Usuario fresh = usuarioDAO.buscarPorId(usuario.getIdUsuario());
            if (fresh != null) {
                fresh.setContrasena(null);
                this.usuario = fresh;
                FacesContext ctx = FacesContext.getCurrentInstance();
                if (ctx != null) {
                    ctx.getExternalContext().getSessionMap().put("usuario", fresh);
                    ctx.getExternalContext().getSessionMap().put("idTecnicoAsociado", fresh.getIdTecnico());
                }
            }
        } catch (SQLException ignored) {
            // sesión sigue con el usuario anterior
        }
    }
}
