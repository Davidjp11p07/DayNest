package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import org.junit.jupiter.api.Test;
import java.sql.*;import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class MigracionComisionLegadoTest extends SoportePostgres {
 @Override protected String esquemaSql(){return "src/test/resources/schema-pre-profesor.sql";}
 @Test void migracionReejecutableConservaGiros() throws Exception {
  Reserva r=reservaPagada();alFinalizar(r);Giro g=servicios.giros.generar(admin.getId(),r.getId(),null);
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()) {
   s.execute("ALTER TABLE giro DROP COLUMN porcentaje_comision, DROP COLUMN monto_comision");
   s.execute(Files.readString(Path.of("migrar-comision.sql")));
   s.execute(Files.readString(Path.of("migrar-comision.sql")));
  }
  assertEquals(g.getMonto(),giros.buscarPorId(g.getId()).orElseThrow().getMonto());
 }
}
