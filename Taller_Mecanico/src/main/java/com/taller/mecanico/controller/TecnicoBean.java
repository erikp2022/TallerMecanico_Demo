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

@Named("tecnicoBean")
@ViewScoped
public class TecnicoBean implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String PASSWORD_DEFECTO = "1234";

    // 777
    private static final String GMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    // Regex estricta para correos
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    private final TecnicoDAO dao = new TecnicoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private List<Tecnico> lista;
    private Tecnico seleccionado;
    private Tecnico nuevo;

    public void cargarLista() {
        try { lista = dao.listarTodos(); } catch (Exception e) { error("Error al cargar: " + e.getMessage()); }
    }

    public void prepararNuevo() {
        nuevo = new Tecnico();
        nuevo.setEstado("Activo");
    }

    public void guardar() {
        try {
            if (validar(nuevo, null)) {
                // Forzar minúsculas para evitar problemas de login
                nuevo.setCorreo(nuevo.getCorreo().trim().toLowerCase());

                int idTec = dao.insertarRetornaId(nuevo);

                Usuario u = new Usuario();
                u.setNombre(nuevo.getNombre().trim());
                u.setCorreo(nuevo.getCorreo());
                u.setContrasena(PASSWORD_DEFECTO);
                u.setRol("tecnico");
                u.setEstado(nuevo.getEstado());
                u.setIdTecnico(idTec);
                usuarioDAO.insertar(u);

                cargarLista();
                info("Técnico creado. Acceso con clave: " + PASSWORD_DEFECTO);
            }
        } catch (Exception e) { error(e.getMessage()); }
    }

    public void actualizar() {
        try {
            if (seleccionado != null && validar(seleccionado, seleccionado.getIdTecnico())) {
                seleccionado.setCorreo(seleccionado.getCorreo().trim().toLowerCase());
                dao.actualizar(seleccionado);

                Usuario vinculado = usuarioDAO.buscarPorIdTecnico(seleccionado.getIdTecnico());
                if (vinculado != null) {
                    vinculado.setNombre(seleccionado.getNombre());
                    vinculado.setCorreo(seleccionado.getCorreo());
                    vinculado.setEstado(seleccionado.getEstado());
                    usuarioDAO.actualizar(vinculado);
                }
                cargarLista();
                info("Técnico actualizado correctamente.");
            }
        } catch (Exception e) { error(e.getMessage()); }
    }

    private boolean validar(Tecnico t, Integer idActual) throws Exception {
        if (t.getNombre() == null || t.getNombre().isBlank()) {
            warn("El nombre es requerido.");
            return false;
        }

        String correo = (t.getCorreo() != null) ? t.getCorreo().trim().toLowerCase() : "";

        // Tiene que cumplir la validacion si no pasa
        if (!correo.matches(GMAIL_REGEX)) {
            warn("Error: El correo '" + correo + "' no tiene un formato válido (ejemplo@gmail.com).");
            return false;
        }

        // Validación de duplicados
        if (dao.existeCorreo(correo, idActual)) {
            warn("Este correo ya pertenece a otro técnico.");
            return false;
        }

        return true;
    }

    // Métodos de utilidad para mensajes
    private void info(String m) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", m)); }
    private void warn(String m) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", m)); }
    private void error(String m) { FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", m)); }

    // Getters y Setters...
    public List<Tecnico> getLista() { if (lista == null) cargarLista(); return lista; }
    public Tecnico getSeleccionado() { return seleccionado; }
    public void setSeleccionado(Tecnico seleccionado) { this.seleccionado = seleccionado; }
    public Tecnico getNuevo() { return nuevo; }
    public void setNuevo(Tecnico nuevo) { this.nuevo = nuevo; }
    public List<String> getEstadosRegistro() { return List.of("Activo", "Inactivo"); }
    public void prepararEditar(Tecnico t) { this.seleccionado = t; }
}