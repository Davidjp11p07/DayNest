package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import java.sql.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IntegridadLegadoTest extends SoportePostgres {
 @Override protected String esquemaSql(){return "src/test/resources/schema-pre-profesor.sql";}
 @Test void migracionIntegridadReejecutable()throws Exception {
  Reserva r=reservaPagada();alFinalizar(r);Giro g=servicios.giros.generar(admin.getId(),r.getId(),null);
  try(Connection c=ConexionBD.abrir();Statement t=c.createStatement()){t.execute(Files.readString(Path.of("sql/migraciones/corregir-integridad.sql")));t.execute(Files.readString(Path.of("sql/migraciones/corregir-integridad.sql")));}
  assertEquals(g.getMonto(),giros.buscarPorId(g.getId()).orElseThrow().getMonto());
 }
}
