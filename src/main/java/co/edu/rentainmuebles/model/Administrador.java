package co.edu.rentainmuebles.model;
public final class Administrador implements PerfilUsuario {
 private final Usuario usuario;
 public Administrador(Usuario u){usuario=java.util.Objects.requireNonNull(u);}
 public Usuario getUsuario(){return usuario;}
 public Rol getRol(){return Rol.ADMINISTRADOR;}
}
