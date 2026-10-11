package co.edu.rentainmuebles.model;
public record Ciudad(long id, DivisionAdministrativa division, String nombre, String codigoOficial) {
 public long getId(){return id;}
 public String getNombre(){return nombre;}
 public String getEtiqueta(){return nombre+", "+division.nombre()+", "+division.pais().nombre();}
 public String getMoneda(){return division.pais().codigoIso2().equals("ES")?"EUR":"COP";}
}