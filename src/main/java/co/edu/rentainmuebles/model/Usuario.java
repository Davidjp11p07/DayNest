package co.edu.rentainmuebles.model;

import java.util.Locale;

/** Identidad común. Los perfiles solapados se asocian por composición. */
public final class Usuario implements Identificable {
    private final Long id;
    private final String nombre;
    private final String correo;
    private final String hashContrasena;
    private final boolean activo;
    private final String telefono;
    private final Administrador administrador;
    private final Cliente cliente;
    private final Propietario propietario;

    public Usuario(Long id, String nombre, String correo, String hashContrasena, String telefono,
                   boolean activo, boolean administrador, boolean cliente, boolean propietario) {
        this.id = ValidacionModelo.identificador(id);
        this.nombre = ValidacionModelo.texto(nombre, "El nombre", 160);
        String correoNormalizado = ValidacionModelo.texto(correo, "El correo", 254).toLowerCase(Locale.ROOT);
        if (!correoNormalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido.");
        }
        this.correo = correoNormalizado;
        // Recibe un hash ya calculado; nunca una contraseña en texto plano.
        this.hashContrasena = ValidacionModelo.texto(hashContrasena, "El hash de contraseña", 512);
        this.activo = activo;
        this.telefono = telefono == null || telefono.isBlank() ? null : ValidacionModelo.texto(telefono,"El teléfono",30);
        this.administrador = administrador ? new Administrador(this) : null;
        this.cliente = cliente ? new Cliente(this) : null;
        this.propietario = propietario ? new Propietario(this) : null;
    }

    public boolean tieneRol(Rol rol) {
        return rol != null && switch (rol) {
            case ADMINISTRADOR -> administrador != null;
            case CLIENTE -> cliente != null;
            case PROPIETARIO -> propietario != null;
        };
    }
    public boolean isEsAdministrador() { return administrador != null; }
    public boolean isCliente() { return cliente != null; }
    public boolean isPropietario() { return propietario != null; }
    public Administrador getAdministrador() { return administrador; }
    public Cliente getCliente() { return cliente; }
    public Propietario getPropietario() { return propietario; }
    public String getTelefono() { return telefono; }
    public java.util.List<PerfilUsuario> getPerfiles() {
        java.util.List<PerfilUsuario> perfiles = new java.util.ArrayList<>();
        if (administrador != null) perfiles.add(administrador);
        if (cliente != null) perfiles.add(cliente);
        if (propietario != null) perfiles.add(propietario);
        return java.util.List.copyOf(perfiles);
    }
    @Override public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getHashContrasena() { return hashContrasena; }
    public boolean isActivo() { return activo; }
}
