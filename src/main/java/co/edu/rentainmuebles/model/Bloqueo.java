package co.edu.rentainmuebles.model;
public record Bloqueo(Ocupacion ocupacion,long propietarioId,String motivo) {
 public Ocupacion getOcupacion(){return ocupacion;}public String getMotivo(){return motivo;}
}
