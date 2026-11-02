package co.edu.rentainmuebles.model;

import java.time.OffsetDateTime;

/** Propiedades JavaBean para que JSP/EL pueda mostrar reseñas en todas las versiones de Jetty. */
public final class Calificacion {
    private final long reservaId;
    private final String autor;
    private final int puntuacion;
    private final String comentario;
    private final OffsetDateTime creadaEn;

    public Calificacion(long reservaId,String autor,int puntuacion,String comentario,OffsetDateTime creadaEn) {
        this.reservaId=reservaId;this.autor=autor;this.puntuacion=puntuacion;this.comentario=comentario;this.creadaEn=creadaEn;
    }
    public long getReservaId(){return reservaId;}
    public String getAutor(){return autor;}
    public int getPuntuacion(){return puntuacion;}
    public String getComentario(){return comentario;}
    public OffsetDateTime getCreadaEn(){return creadaEn;}
}
