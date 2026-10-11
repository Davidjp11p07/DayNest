package co.edu.rentainmuebles.model;
import java.time.LocalDate;
public record Ocupacion(long id,long inmuebleId,LocalDate inicio,LocalDate fin,String tipo,boolean activa) {
 public long getId(){return id;}public LocalDate getInicio(){return inicio;}public LocalDate getFin(){return fin;}public boolean isActiva(){return activa;}
}
