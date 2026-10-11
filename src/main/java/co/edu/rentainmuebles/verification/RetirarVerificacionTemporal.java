package co.edu.rentainmuebles.verification;
import co.edu.rentainmuebles.config.ConexionBD;import java.sql.*;
/** Limpieza limitada al esquema temporal de esta verificación; nunca acepta daynest/public. */
public final class RetirarVerificacionTemporal {
 public static void main(String[] args)throws Exception{
  if(args.length!=1||!args[0].equals("daynest_webtest_20261010"))throw new IllegalArgumentException("Solo se acepta el esquema temporal de verificación.");
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   s.execute("DROP SCHEMA IF EXISTS daynest_webtest_20261010 CASCADE");
   try(ResultSet r=s.executeQuery("SELECT (SELECT count(*) FROM daynest.reserva),(SELECT count(*) FROM daynest.movimiento_pago),(SELECT count(*) FROM public.reserva),(SELECT count(*) FROM public.pago),(SELECT count(*) FROM public.giro)")){r.next();System.out.println("Base activa: "+r.getInt(1)+" reservas, "+r.getInt(2)+" movimientos nuevos. Archivo intacto: "+r.getInt(3)+" reservas, "+r.getInt(4)+" pagos, "+r.getInt(5)+" giros.");}
  }
 }
}
