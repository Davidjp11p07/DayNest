package co.edu.rentainmuebles.model;
import java.util.*;
/** Criterios de consulta, no entidad persistente. NULL significa sin filtro. */
public record FiltrosInmueble(Long paisId,Long divisionId,String tipo,Boolean rural,Boolean mar) {
 public static final Set<String> TIPOS=Set.of("APARTAMENTO","CASA","CABAÑA","HABITACION","OTRO");
 public FiltrosInmueble {
  if((paisId!=null&&paisId<=0)||(divisionId!=null&&divisionId<=0))throw new IllegalArgumentException("Ubicación inválida.");
  tipo=validarTipo(tipo);
 }
 public static String validarTipo(String tipo){if(tipo==null||tipo.isBlank())return null;tipo=tipo.trim().toUpperCase(Locale.ROOT);if(!TIPOS.contains(tipo))throw new IllegalArgumentException("Tipo de inmueble inválido.");return tipo;}
 public static FiltrosInmueble ninguno(){return new FiltrosInmueble(null,null,null,null,null);}
}
