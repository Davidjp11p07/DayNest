package co.edu.rentainmuebles.model;
/** Banco del catalogo y pais al que pertenece. */
public record Banco(long id, String nombre, long paisId, String pais) {
 public long getId(){return id;} public String getNombre(){return nombre;}
 public long getPaisId(){return paisId;} public String getPais(){return pais;}
 public String getEtiqueta(){return nombre+", "+pais;}
}
