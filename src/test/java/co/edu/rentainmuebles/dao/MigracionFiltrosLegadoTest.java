package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.EstadoGiro;
import co.edu.rentainmuebles.verification.MigrarFiltros;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MigracionFiltrosLegadoTest extends SoportePostgres {
 @Override protected String esquemaSql(){return "src/test/resources/schema-pre-profesor.sql";}
 @Test void migracionPreservaFilasFinancierasYEsReejecutable()throws Exception{
  var r=reservaPagada();alFinalizar(r);servicios.giros.generar(admin.getId(),r.getId(),"MIGRA-FILTROS");
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   // Retirar solo los atributos nuevos en este esquema UUID para reconstruir el DDL anterior.
   s.execute("UPDATE inmueble_operativo SET tipo='CASA' WHERE tipo IS NULL");
   s.execute("DROP VIEW inmueble_operativo");
   s.execute("ALTER TABLE inmueble DROP COLUMN es_rural,DROP COLUMN cerca_mar");
   s.execute("ALTER TABLE inmueble ALTER COLUMN tipo SET NOT NULL");
   var antes=MigrarFiltros.huellas(c);MigrarFiltros.migrar(c);MigrarFiltros.migrar(c);assertEquals(antes,MigrarFiltros.huellas(c));s.execute("CREATE VIEW inmueble_operativo AS SELECT * FROM inmueble");
   try(ResultSet v=s.executeQuery("SELECT es_rural,cerca_mar FROM inmueble")){assertTrue(v.next());assertNull(v.getObject(1));assertNull(v.getObject(2));}
  }
  assertEquals(EstadoGiro.EJECUTADO,giros.listar().get(0).getEstado());
 }
}
