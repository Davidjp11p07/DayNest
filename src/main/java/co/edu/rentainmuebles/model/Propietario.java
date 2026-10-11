package co.edu.rentainmuebles.model;

public final class Propietario implements PerfilUsuario {
    private final Usuario usuario;
    Propietario(Usuario usuario) { this.usuario = java.util.Objects.requireNonNull(usuario); }
    @Override public Usuario getUsuario() { return usuario; }

    @Override public Rol getRol() { return Rol.PROPIETARIO; }
}
