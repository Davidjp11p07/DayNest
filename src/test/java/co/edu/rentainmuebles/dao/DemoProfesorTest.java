package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import org.junit.jupiter.api.Test;
import java.sql.*;import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class DemoProfesorTest extends SoportePostgres {
 @Test void demoReejecutableSoloCompletaPerfilesYPropiedadesDeEjemplo()throws Exception {
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   s.execute(Files.readString(Path.of("sql/instalacion/data.sql")));
   s.execute(Files.readString(Path.of("sql/demostracion/datos-demo-profesor.sql")));
   s.execute(Files.readString(Path.of("sql/demostracion/datos-demo-profesor.sql")));
   try(ResultSet r=s.executeQuery("SELECT count(*) FROM clientes c LEFT JOIN vincula v USING(id_cl) WHERE c.emai_cl LIKE '%@renta.test' AND (v.id_b IS NULL OR c.id_cd IS NULL)")){r.next();assertEquals(0,r.getInt(1));}
   try(ResultSet r=s.executeQuery("SELECT count(*) FROM inmuebles i JOIN asocia a USING(id_i) JOIN propietarios p USING(id_p) WHERE p.emai_p LIKE '%@renta.test' AND (i.area IS NULL OR i.n_hab IS NULL)")){r.next();assertEquals(0,r.getInt(1));}
   try(ResultSet r=s.executeQuery("SELECT num_cta FROM vincula WHERE id_cl="+cliente.getId())){r.next();assertEquals("0000123456",r.getString(1));}
   try(ResultSet r=s.executeQuery("SELECT area FROM inmuebles WHERE id_i="+inmueble.getId())){r.next();assertNull(r.getObject(1));}
  }
 }
}
