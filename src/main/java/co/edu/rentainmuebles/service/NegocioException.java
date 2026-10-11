package co.edu.rentainmuebles.service;

/** Incumplimiento de una regla de negocio o fallo al completar la operación. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) { super(mensaje); }
    public NegocioException(String mensaje, Throwable causa) { super(mensaje,causa); }
}
