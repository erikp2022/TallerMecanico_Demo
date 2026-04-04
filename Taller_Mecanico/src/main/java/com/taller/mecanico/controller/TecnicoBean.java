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

    public static final String PASSWORD_USUARIO_TECNICO_DEFECTO = "1234";

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final TecnicoDAO dao = new TecnicoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private List<Tecnico> lista;
    private Tecnico seleccionado;
    private Tecnico nuevo;

    public void cargarLista() {
        try {
            lista = dao.listarTodos();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararNuevo() {
        nuevo = new Tecnico();
        nuevo.setEstado("Activo");
    }

    public void guardar() {
        try {
            if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "El nombre es obligatorio."));
                return;
            }
            if (nuevo.getCorreo() == null || nuevo.getCorreo().isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "El correo es obligatorio."));
                return;
            }
            if (!EMAIL.matcher(nuevo.getCorreo().trim()).matches()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Formato de correo no válido."));
                return;
            }
            if (dao.existeCorreo(nuevo.getCorreo(), null)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Ya existe un técnico con ese correo."));
                return;
            }
            if (usuarioDAO.existeCorreo(nuevo.getCorreo().trim(), null)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Ya existe un usuario con ese correo."));
                return;
            }

            int idTec = dao.insertarRetornaId(nuevo);

            Usuario u = new Usuario();
            u.setNombre(nuevo.getNombre().trim());
            u.setCorreo(nuevo.getCorreo().trim().toLowerCase());
            u.setContrasena(PASSWORD_USUARIO_TECNICO_DEFECTO);
            u.setRol("tecnico");
            u.setEstado("Activo");
            u.setIdTecnico(idTec);
            usuarioDAO.insertar(u);

            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Guardado",
                            "Técnico creado. Usuario de acceso con correo y contraseña por defecto: " + PASSWORD_USUARIO_TECNICO_DEFECTO));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    public void prepararEditar(Tecnico t) {
        seleccionado = t;
    }

    public void actualizar() {
        try {
            if (seleccionado == null) {
                return;
            }
            if (seleccionado.getCorreo() == null || seleccionado.getCorreo().isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "El correo es obligatorio."));
                return;
            }
            if (!EMAIL.matcher(seleccionado.getCorreo().trim()).matches()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Formato de correo no válido."));
                return;
            }
            if (dao.existeCorreo(seleccionado.getCorreo(), seleccionado.getIdTecnico())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "Ya existe otro técnico con ese correo."));
                return;
            }

            Usuario vinculado = usuarioDAO.buscarPorIdTecnico(seleccionado.getIdTecnico());
            if (vinculado != null
                    && usuarioDAO.existeCorreo(seleccionado.getCorreo().trim(), vinculado.getIdUsuario())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Validación", "El correo ya está en uso por otro usuario."));
                return;
            }

            dao.actualizar(seleccionado);

            if (vinculado != null) {
                vinculado.setNombre(seleccionado.getNombre());
                vinculado.setCorreo(seleccionado.getCorreo().trim().toLowerCase());
                vinculado.setEstado(seleccionado.getEstado());
                usuarioDAO.actualizar(vinculado);
            }

            cargarLista();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Actualizado", "Técnico modificado."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }
    //cambios solo finalizado se elimina del tecnico y usuario panel ordenes tambien sevan

    public void eliminar(Tecnico t) {
        try {
            // Bloquear si tiene órdenes pendientes o en proceso
            if (dao.tieneOrdenesActivas(t.getIdTecnico())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "No se puede eliminar",
                                "El técnico tiene órdenes pendientes o en proceso. " +
                                        "Finalice todas las órdenes antes de eliminar."));
                return;
            }

            // 1. Eliminar reparaciones de las órdenes finalizadas
            dao.eliminarReparacionesDeOrdenes(t.getIdTecnico());

            // 2. Eliminar órdenes finalizadas
            dao.eliminarOrdenesFinalizada(t.getIdTecnico());

            // 3. Eliminar usuario vinculado
            Usuario u = usuarioDAO.buscarPorIdTecnico(t.getIdTecnico());
            if (u != null) {
                usuarioDAO.eliminar(u.getIdUsuario());
            }

            // 4. Eliminar técnico
            dao.eliminar(t.getIdTecnico());
            cargarLista();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Eliminado",
                            "Técnico, usuario, órdenes y reparaciones eliminados correctamente."));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error", e.getMessage()));
        }
    }


    public List<Tecnico> getLista() {
        if (lista == null) {
            cargarLista();
        }
        return lista;
    }

    public void setLista(List<Tecnico> lista) {
        this.lista = lista;
    }

    public Tecnico getSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(Tecnico seleccionado) {
        this.seleccionado = seleccionado;
    }

    public Tecnico getNuevo() {
        return nuevo;
    }

    public void setNuevo(Tecnico nuevo) {
        this.nuevo = nuevo;
    }

    public List<String> getEstadosRegistro() {
        return List.of("Activo", "Inactivo");
    }
}
