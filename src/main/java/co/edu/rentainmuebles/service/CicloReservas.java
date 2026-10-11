package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.dao.*;import co.edu.rentainmuebles.dao.jdbc.CicloReservasDaoJdbc;
/** Expira pendientes y finaliza estancias; tarea local, sin proveedor externo. */
public final class CicloReservas {
 private final CicloReservasDao dao=new CicloReservasDaoJdbc();
 public void ejecutar(){try{dao.ejecutar();}catch(DaoException e){throw new NegocioException("No pudo ejecutar el ciclo de reservas.",e);}}
}