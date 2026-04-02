package com.taller.mecanico.controller;

import com.taller.mecanico.dao.TecnicoDAO;
import com.taller.mecanico.dao.UsuarioDAO;
import com.taller.mecanico.model.Tecnico;
import com.taller.mecanico.model.Usuario;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Named("usuarioBean")
@ViewScoped
public class UsuarioBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UsuarioDAO dao = new UsuarioDAO();
    private final TecnicoDAO tecnicoDAO = new TecnicoDAO();

    private List<Usuario> lista;
    private Usuario seleccionado;
    private Usuario nuevo;

    public void cargarLista() {
        try {
            lista = dao.listarTodos();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararNuevo() {
        nuevo = new Usuario();
        nuevo.setEstado("Activo");
        nuevo.setRol("admin");
    }

    public void guardar() {
        try {
            if (nuevo == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Abra primero «Nuevo usuario»."));
                return;
            }
            if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()
                    || nuevo.getCorreo() == null || nuevo.getCorreo().isBlank()
                    || nuevo.getContrasena() == null || nuevo.getContrasena().isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Complete los campos obligatorios."));
                return;
            }
            if (!EMAIL.matcher(nuevo.getCorreo().trim()).matches()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Formato de correo no válido."));
                return;
            }
            if (!"admin".equalsIgnoreCase(nuevo.getRol())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                                "Las cuentas con rol técnico se crean desde el módulo Técnicos."));
                return;
            }
            if (dao.existeCorreo(nuevo.getCorreo().trim(), null)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Ya existe un usuario con ese correo."));
                return;
            }
            nuevo.setCorreo(nuevo.getCorreo().trim().toLowerCase());
            nuevo.setIdTecnico(null);
            dao.insertar(nuevo);
            if (nuevo.getIdTecnico() != null && nuevo.getIdTecnico() == 0) {
                nuevo.setIdTecnico(null);
            }
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado", "Usuario administrador creado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditar(Usuario u) {
        try {
            seleccionado = dao.buscarPorId(u.getIdUsuario());
            if (seleccionado == null) {
                seleccionado = u;
            } else {
                seleccionado.setContrasena(null);
            }
        } catch (Exception e) {
            seleccionado = u;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
        if (seleccionado != null) {
            normalizarEstadoCuenta(seleccionado);
        }
    }

    public void actualizar() {
        try {
            if (seleccionado == null) {
                return;
            }
            if (seleccionado.getNombre() == null || seleccionado.getNombre().isBlank()
                    || seleccionado.getCorreo() == null || seleccionado.getCorreo().isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Complete los campos obligatorios."));
                return;
            }
            String passNueva = seleccionado.getContrasena() != null ? seleccionado.getContrasena().trim() : "";
            if (passNueva.isEmpty()) {
                Usuario existente = dao.buscarPorId(seleccionado.getIdUsuario());
                if (existente == null || existente.getContrasena() == null
                        || existente.getContrasena().isBlank()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación",
                                    "Indique una contraseña o vuelva a cargar el usuario."));
                    return;
                }
                seleccionado.setContrasena(existente.getContrasena());
            } else {
                seleccionado.setContrasena(passNueva);
            }
            if (!EMAIL.matcher(seleccionado.getCorreo().trim()).matches()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Formato de correo no válido."));
                return;
            }
            String rol = seleccionado.getRol() != null ? seleccionado.getRol().trim() : "";
            if (!"admin".equalsIgnoreCase(rol) && !"tecnico".equalsIgnoreCase(rol)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Rol no válido."));
                return;
            }
            if ("tecnico".equalsIgnoreCase(rol) && seleccionado.getIdTecnico() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Debe vincular un técnico a la cuenta."));
                return;
            }
            if ("admin".equalsIgnoreCase(rol)) {
                seleccionado.setIdTecnico(null);
            }
            if (dao.existeCorreo(seleccionado.getCorreo().trim(), seleccionado.getIdUsuario())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Ya existe otro usuario con ese correo."));
                return;
            }
            seleccionado.setCorreo(seleccionado.getCorreo().trim().toLowerCase());
            dao.actualizar(seleccionado);
            if ("tecnico".equalsIgnoreCase(rol) && seleccionado.getIdTecnico() != null) {
                Tecnico t = tecnicoDAO.buscarPorId(seleccionado.getIdTecnico());
                if (t != null) {
                    t.setEstado(seleccionado.getEstado());
                    tecnicoDAO.actualizar(t);
                }
            }
            cargarLista();
            seleccionado.setContrasena(null);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Actualizado", "Usuario modificado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    /**
     * Técnicos que pueden vincularse a un usuario (excluye ids ya usados por otro usuario).
     */
    public List<Tecnico> getTecnicosDisponiblesVinculacion() {
        try {
            List<Tecnico> todos = tecnicoDAO.listarTodos();
            List<Usuario> usus = dao.listarTodos();
            Set<Integer> ocupados = new HashSet<>();
            for (Usuario u : usus) {
                if (u.getIdTecnico() != null
                        && (seleccionado == null || !u.getIdUsuario().equals(seleccionado.getIdUsuario()))) {
                    ocupados.add(u.getIdTecnico());
                }
            }
            List<Tecnico> out = new ArrayList<>();
            for (Tecnico t : todos) {
                if (!ocupados.contains(t.getIdTecnico())
                        || (seleccionado != null && seleccionado.getIdTecnico() != null
                        && seleccionado.getIdTecnico().equals(t.getIdTecnico()))) {
                    out.add(t);
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    public void eliminar(Usuario u) {
        try {
            dao.eliminar(u.getIdUsuario());
            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Eliminado", "Usuario eliminado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public List<String> getEstadosCuenta() {
        return List.of("Activo", "Inactivo");
    }

    private static void normalizarEstadoCuenta(Usuario x) {
        if (x.getEstado() == null) {
            return;
        }
        String e = x.getEstado().trim();
        if ("activo".equalsIgnoreCase(e) || "1".equals(e)) {
            x.setEstado("Activo");
        } else if ("inactivo".equalsIgnoreCase(e) || "0".equals(e)) {
            x.setEstado("Inactivo");
        }
    }

    public boolean isMostrarSelectTecnico() {
        return seleccionado != null && seleccionado.getRol() != null
                && "tecnico".equalsIgnoreCase(seleccionado.getRol().trim());
    }

    public List<Usuario> getLista() {
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public void setLista(List<Usuario> lista) {
        this.lista = lista;
    }

    public Usuario getSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(Usuario seleccionado) {
        this.seleccionado = seleccionado;
    }

    public Usuario getNuevo() {
        return nuevo;
    }

    public void setNuevo(Usuario nuevo) {
        this.nuevo = nuevo;
    }
}
