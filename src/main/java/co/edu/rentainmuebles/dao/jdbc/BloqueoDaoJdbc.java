package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.dao.*;
import co.edu.rentainmuebles.config.ConexionBD;import co.edu.rentainmuebles.dao.jdbc.UsuarioDaoJdbc;import co.edu.rentainmuebles.model.*;import java.sql.*;import java.time.*;import java.util.*;
public final class BloqueoDaoJdbc implements BloqueoDao {
 public List<Bloqueo> listar(long propietarioId){try(Connection c=ConexionBD.abrir();PreparedStatement s=c.prepareStatement("SELECT b.*,o.* FROM bloqueo b JOIN ocupacion o USING(id_ocupacion) WHERE b.id_propietario_solicitante=? ORDER BY fecha_inicio")){s.setLong(1,propietarioId);try(ResultSet r=s.executeQuery()){List<Bloqueo> a=new ArrayList<>();while(r.next())a.add(new Bloqueo(new Ocupacion(r.getLong("id_ocupacion"),r.getLong("id_inmueble"),r.getObject("fecha_inicio",LocalDate.class),r.getObject("fecha_fin",LocalDate.class),"BLOQUEO",r.getBoolean("activa")),propietarioId,r.getString("motivo")));return a;}}catch(SQLException e){throw new DaoException("No pudo consultar los bloqueos.",e);}}
 public void crear(long propietarioId,long inmuebleId,LocalDate inicio,LocalDate fin,String motivo){
  
  try(Connection c=ConexionBD.abrir()){c.setAutoCommit(false);try{
   long id;
   try(PreparedStatement s=c.prepareStatement("INSERT INTO ocupacion(id_inmueble,fecha_inicio,fecha_fin,tipo) SELECT id_inmueble,?,?,'BLOQUEO' FROM inmueble WHERE id_inmueble=? AND id_propietario=? RETURNING id_ocupacion")){s.setObject(1,inicio);s.setObject(2,fin);s.setLong(3,inmuebleId);s.setLong(4,propietarioId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new DaoException("El inmueble no pertenece al propietario.");id=r.getLong(1);}}
   try(PreparedStatement s=c.prepareStatement("INSERT INTO bloqueo(id_ocupacion,id_propietario_solicitante,motivo) VALUES(?,?,?)")){s.setLong(1,id);s.setLong(2,propietarioId);s.setString(3,motivo.trim());s.executeUpdate();}c.commit();
  }catch(SQLException|RuntimeException e){c.rollback();throw e;}}catch(SQLException e){throw new DaoException("Las fechas están ocupadas o los datos no son válidos.",e);}
 }
 public void liberar(long propietarioId,long id){try(Connection c=ConexionBD.abrir();PreparedStatement s=c.prepareStatement("UPDATE ocupacion o SET activa=false FROM bloqueo b WHERE o.id_ocupacion=b.id_ocupacion AND b.id_propietario_solicitante=? AND o.id_ocupacion=?")){s.setLong(1,propietarioId);s.setLong(2,id);if(s.executeUpdate()!=1)throw new DaoException("Bloqueo inexistente o ajeno.");}catch(SQLException e){throw new DaoException("No pudo liberar el bloqueo.",e);}}
}
