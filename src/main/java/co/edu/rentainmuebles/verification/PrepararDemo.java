package co.edu.rentainmuebles.verification;
/** El modelo definitivo no permite inventar estancias pasadas desde el flujo normal. */
public final class PrepararDemo {
 public static void main(String[] args){throw new IllegalStateException("El modelo definitivo exige crear y cobrar una reserva futura y finalizarla tras su salida. Para una demostración de giro inmediata use las pruebas aisladas de ModeloProfesorTest; el historial previo se consulta en /admin/historial-anterior.");}
}