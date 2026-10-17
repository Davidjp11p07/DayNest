package co.edu.rentainmuebles.model;
/** Entidad de negocio separada de las credenciales de acceso. */
public record DatosPerfil(long id,Rol perfil,String nombre,String correo,String telefono,Long ciudadId,CuentaBancaria cuenta) {
 public long getId(){return id;}public Rol getPerfil(){return perfil;}public String getNombre(){return nombre;}
 public String getCorreo(){return correo;}public String getTelefono(){return telefono;}public Long getCiudadId(){return ciudadId;}public CuentaBancaria getCuenta(){return cuenta;}
}
