package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.config.ConexionBD;import co.edu.rentainmuebles.dao.HistorialAnteriorDao;import java.sql.*;import java.util.*;
public final class HistorialAnteriorDaoJdbc implements HistorialAnteriorDao {
 public List<Map<String,Object>> consultar(){
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   try(ResultSet x=s.executeQuery("SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='reserva' AND column_name='id_reserva')")){x.next();if(!x.getBoolean(1))return List.of();}
   try(ResultSet r=s.executeQuery("SELECT r.id_reserva id,u.nombre_completo cliente,i.titulo inmueble,r.fecha_entrada entrada,r.fecha_salida salida,r.estado,COALESCE((SELECT sum(p.monto) FROM public.pago p WHERE p.id_reserva=r.id_reserva AND p.estado='APROBADO'),0) cobrado,COALESCE((SELECT sum(g.monto) FROM public.giro g WHERE g.id_reserva=r.id_reserva AND g.estado='GENERADO'),0) girado FROM public.reserva r JOIN public.usuario u ON u.id_usuario=r.id_cliente JOIN public.inmueble i USING(id_inmueble) ORDER BY r.id_reserva")){
    List<Map<String,Object>> a=new ArrayList<>();while(r.next()){Map<String,Object> fila=new LinkedHashMap<>();for(String k:List.of("id","cliente","inmueble","entrada","salida","estado","cobrado","girado"))fila.put(k,r.getObject(k));a.add(fila);}return a;
   }
  }catch(SQLException e){throw new co.edu.rentainmuebles.dao.DaoException("No pudo consultar el historial anterior.",e);}
 }
}
