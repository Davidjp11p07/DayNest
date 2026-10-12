package co.edu.rentainmuebles.config;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.model.*;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class ConfiguracionServiciosTest extends SoportePostgres {
 @Test void porcentajeConfiguradoSeAplicaAlGiro() {
  Reserva r=reservaPagada();alFinalizar(r);
  Properties p=new Properties();p.setProperty("negocio.comision","0.1000");p.setProperty("negocio.zonaHoraria","America/Bogota");
  Giro g=Servicios.crear(p).giros.generar(admin.getId(),r.getId(),null);
  assertEquals(new BigDecimal("0.1000"),g.getPorcentajeComision());
  assertEquals(new BigDecimal("405000.00"),g.getMonto());
 }
}
