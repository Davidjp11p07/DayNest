package co.edu.rentainmuebles.model;

/** Comportamiento polimórfico de perfiles; la identidad pertenece al usuario. */
public interface PerfilUsuario extends Identificable {
    Usuario getUsuario();
    Rol getRol();
    @Override default Long getId() { return getUsuario().getId(); }
    default String getNombre() { return getUsuario().getNombre(); }
    default String getCorreo() { return getUsuario().getCorreo(); }
    default String getHashContrasena() { return getUsuario().getHashContrasena(); }
    default boolean isActivo() { return getUsuario().isActivo(); }
}
