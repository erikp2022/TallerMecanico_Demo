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
import java.util.List;
import java.util.regex.Pattern;


@Named("usuarioBean")
@ViewScoped
public class UsuarioBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String GMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    private final UsuarioDAO dao = new UsuarioDAO();
    private List<Usuario> lista;
    private Usuario seleccionado;
    private Usuario nuevo;

    public void cargarLista() {
        try {
            lista = dao.listarTodos();
        } catch (Exception e) {
            error("Error al cargar: " + e.getMessage());
        }
    }

    public void prepararNuevo() {
        nuevo = new Usuario();
        nuevo.setEstado("Activo");
        nuevo.setRol("admin"); // Por defecto admin, ya que técnicos se crean en su módulo
    }

    public void guardar() {
        try {
            if (validar(nuevo, null)) {
                nuevo.setCorreo(nuevo.getCorreo().trim().toLowerCase());
                nuevo.setIdTecnico(null);
                dao.insertar(nuevo);
                cargarLista();
                info("Usuario administrador creado correctamente.");
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    public void actualizar() {
        try {
            if (seleccionado != null && validar(seleccionado, seleccionado.getIdUsuario())) {
                seleccionado.setCorreo(seleccionado.getCorreo().trim().toLowerCase());

                // Lógica de contraseña: si viene vacía, mantenemos la anterior
                if (seleccionado.getContrasena() == null || seleccionado.getContrasena().isBlank()) {
                    Usuario existente = dao.buscarPorId(seleccionado.getIdUsuario());
                    seleccionado.setContrasena(existente.getContrasena());
                }

                dao.actualizar(seleccionado);
                cargarLista();
                info("Usuario actualizado correctamente.");
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    public void eliminar(Usuario u) {
        try {
            // Regla 1: No borrarse a sí mismo
            Usuario actual = (Usuario) FacesContext.getCurrentInstance()
                    .getExternalContext().getSessionMap().get("usuario");
            if (actual != null && actual.getIdUsuario().equals(u.getIdUsuario())) {
                warn("No puedes eliminar tu propia cuenta.");
                return;
            }

            // Regla 2: Si tiene órdenes como creador → advertir pero permitir con confirmación
            if (dao.tieneOrdenesComoCreador(u.getIdUsuario())) {
                // Se muestra aviso; el ConfirmDialog ya pidió confirmación antes de llegar aquí
                // Si llegamos acá, el usuario confirmó → proceder
            }

            // Regla 3: Si es técnico con órdenes activas → NO se puede
            if (u.getIdTecnico() != null) {
                if (dao.tieneOrdenesPendientes(u.getIdTecnico())) {
                    warn("No se puede eliminar: el técnico tiene órdenes en 'Pendiente' o 'En proceso'.");
                    return;
                }
            }

            // Ejecutar eliminación en cascada (orden correcto para Access)
            dao.eliminarUsuarioYDatosTecnico(u.getIdUsuario(), u.getIdTecnico());
            cargarLista();
            info("Usuario" + (u.getIdTecnico() != null ? " y técnico asociado" : "") + " eliminados correctamente.");

        } catch (Exception e) {
            error("Error al eliminar: " + e.getMessage());
        }
    }

    private boolean validar(Usuario u, Integer idActual) throws Exception {
        if (u.getNombre() == null || u.getNombre().isBlank()) {
            warn("El nombre es requerido.");
            return false;
        }

        String correo = (u.getCorreo() != null) ? u.getCorreo().trim().toLowerCase() : "";
        if (!correo.matches(GMAIL_REGEX)) {
            warn("Error: El correo '" + correo + "' no tiene un formato válido.");
            return false;
        }

        if (dao.existeCorreo(correo, idActual)) {
            warn("Este correo ya pertenece a otro usuario.");
            return false;
        }

        // Validación extra: contraseña obligatoria solo si es nuevo
        if (idActual == null && (u.getContrasena() == null || u.getContrasena().isBlank())) {
            warn("La contraseña es obligatoria para nuevos usuarios.");
            return false;
        }

        return true;
    }


    // Métodos de utilidad que ya tienes en TecnicoBean
    private void info(String m) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", m));
    }

    private void warn(String m) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", m));
    }

    private void error(String m) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", m));
    }


    // Getters, Setters y utilidades de vista
    public List<Usuario> getLista() {
        if (lista == null) cargarLista();
        return lista;
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

    public List<String> getEstadosRegistro() {
        return List.of("Activo", "Inactivo");
    }

    public void prepararEditar(Usuario u) {
        this.seleccionado = u;
        this.seleccionado.setContrasena("");
    }
}