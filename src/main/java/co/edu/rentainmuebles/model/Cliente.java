package co.edu.rentainmuebles.model;

public final class Cliente implements PerfilUsuario {
    private final Usuario usuario;
    Cliente(Usuario usuario) { this.usuario = java.util.Objects.requireNonNull(usuario); }
    @Override public Usuario getUsuario() { return usuario; }

    @Override public Rol getRol() { return Rol.CLIENTE; }
}
