package co.edu.rentainmuebles.model;

public final class Propietario implements PerfilUsuario {
    private final Usuario usuario;
    private final Long id;
    private final String nombre;
    private final String correo;
    private final String telefono;
    Propietario(Usuario usuario) { this.usuario = java.util.Objects.requireNonNull(usuario); this.id=usuario.getId();this.nombre=usuario.getNombre();this.correo=usuario.getCorreo();this.telefono=usuario.getTelefono(); }
    @Override public Long getId(){return id;}
    @Override public String getNombre(){return nombre;}
    @Override public String getCorreo(){return correo;}
    public String getTelefono(){return telefono;}
    @Override public Usuario getUsuario() { return usuario; }

    @Override public Rol getRol() { return Rol.PROPIETARIO; }
}
