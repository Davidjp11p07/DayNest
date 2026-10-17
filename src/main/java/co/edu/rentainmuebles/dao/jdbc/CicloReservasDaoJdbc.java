package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.dao.*;
import co.edu.rentainmuebles.config.ConexionBD;
import java.sql.*;
/** Tarea local: expira pendientes y finaliza estancias cobradas; no llama proveedores externos. */
public final class CicloReservasDaoJdbc implements CicloReservasDao {
 public void ejecutar(){
  try(Connection c=ConexionBD.abrir()){
   c.setAutoCommit(false);
   try(Statement s=c.createStatement()){
    s.executeUpdate("UPDATE reserva SET estado='EXPIRADA' WHERE estado='PENDIENTE_PAGO' AND expira_en<=clock_timestamp()");
    s.executeUpdate("UPDATE reserva r SET estado='FINALIZADA' FROM ocupacion o WHERE r.id_ocupacion=o.id_ocupacion AND r.estado='CONFIRMADA' AND o.fecha_fin<=CURRENT_DATE");
    // Cada devolución tiene clave estable y referencia al cobro original.
    s.executeUpdate("INSERT INTO movimiento_pago(id_reserva,clase,monto,id_cobro_origen,clave_idempotencia) SELECT m.id_reserva,'DEVOLUCION',m.monto,m.id_movimiento,'expiracion-cobro-'||m.id_movimiento FROM movimiento_pago m JOIN reserva r ON r.id_ocupacion=m.id_reserva WHERE r.estado='EXPIRADA' AND m.clase='COBRO' AND m.estado='APROBADO' AND NOT EXISTS(SELECT 1 FROM movimiento_pago d WHERE d.id_cobro_origen=m.id_movimiento)");
    s.executeUpdate("UPDATE movimiento_pago SET estado='APROBADO',resuelto_en=clock_timestamp() WHERE clase='DEVOLUCION' AND estado='PENDIENTE' AND clave_idempotencia LIKE 'expiracion-cobro-%'");
    c.commit();
    c.setAutoCommit(true);
    // Una transaccion por reserva: un fallo no revierte otras liquidaciones.
    try(ResultSet disponible=s.executeQuery("SELECT to_regprocedure('liquidar_reserva_automatica(bigint)') IS NOT NULL")){
     disponible.next();if(!disponible.getBoolean(1))return;
    }
    java.util.List<Long> pendientes=new java.util.ArrayList<>();
    try(ResultSet r=s.executeQuery("SELECT r.id_ocupacion FROM renta r WHERE r.estado='FINALIZADA' AND r.fch_s<=CURRENT_DATE AND NOT EXISTS(SELECT 1 FROM giro g WHERE g.id_reserva=r.id_ocupacion AND g.estado='EJECUTADO') ORDER BY r.id_ocupacion")){while(r.next())pendientes.add(r.getLong(1));}
    try(PreparedStatement t=c.prepareStatement("SELECT liquidar_reserva_automatica(?)")){for(long id:pendientes){t.setLong(1,id);t.executeQuery().close();}}

   }catch(SQLException|RuntimeException e){if(!c.getAutoCommit())c.rollback();throw e;}
  }catch(SQLException e){throw new DaoException("No pudo ejecutarse el ciclo de reservas.",e);}
 }
}
