package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.model.Usuario;
import java.io.Serializable;
/** Identidad de sesión sin hashes ni contraseñas. */
public final class UsuarioSesion implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String nombre;
    private final boolean cliente,propietario,esAdministrador;
    public UsuarioSesion(Usuario u) { id=u.getId(); nombre=u.getNombre(); cliente=u.isCliente(); propietario=u.isPropietario(); esAdministrador=u.isEsAdministrador(); }
    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isCliente() { return cliente; }
    public boolean isPropietario() { return propietario; }
    public boolean isEsAdministrador() { return esAdministrador; }
    public boolean tienePermiso(String permiso) {
        return switch(permiso) { case "ADMINISTRADOR" -> esAdministrador; case "CLIENTE" -> cliente; case "PROPIETARIO" -> propietario; default -> false; };
    }
}
