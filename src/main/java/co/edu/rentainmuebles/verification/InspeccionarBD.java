package co.edu.rentainmuebles.verification;
import co.edu.rentainmuebles.config.ConexionBD;import java.sql.*;
public final class InspeccionarBD {
 public static void main(String[] args)throws Exception{try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
  System.out.println("Esquema activo: "+c.getSchema());
  for(String tabla:new String[]{"usuario","cliente","propietario","administrador","pais","division_administrativa","ciudad","inmueble","ocupacion","reserva","bloqueo","movimiento_pago","giro"})try(ResultSet r=s.executeQuery("SELECT count(*) FROM "+tabla)){r.next();System.out.println(tabla+": "+r.getLong(1));}
  try(ResultSet r=s.executeQuery("SELECT c.nombre,d.nombre,p.nombre,count(i.id_inmueble) FROM ciudad c JOIN division_administrativa d USING(id_division) JOIN pais p USING(id_pais) LEFT JOIN inmueble i USING(id_ciudad) GROUP BY c.id_ciudad,c.nombre,d.nombre,p.nombre ORDER BY c.nombre,p.nombre")){while(r.next())System.out.println(r.getString(1)+", "+r.getString(2)+", "+r.getString(3)+": "+r.getInt(4)+" alojamientos");}
 }}
}