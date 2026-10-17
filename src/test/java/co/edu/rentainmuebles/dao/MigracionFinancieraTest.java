package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.*;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.service.impl.PoliticaComisionPorcentaje;
import co.edu.rentainmuebles.verification.MigrarGirosAutomaticos;
import org.junit.jupiter.api.Test;
import java.math.*;import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class MigracionFinancieraTest extends SoportePostgres {
 @Override protected String esquemaSql(){return "src/test/resources/schema-antes-giros.sql";}
 @Test void conservaGiroHumanoDiezPorCientoYAsignaTreintaAPagoAntiguoSinGiro()throws Exception {
  var antigua=reservaPagada();alFinalizar(antigua);
  var diez=new Servicios(unidad,RELOJ,new PoliticaComisionPorcentaje(new BigDecimal("0.1000")));
  var humano=diez.giros.generar(admin.getId(),antigua.getId(),"HISTORICO-HUMANO");
  var otroInmueble=servicios.inmuebles.publicar(propietario.getId(),"Historico dos","Bogota","Calle 2","Prueba",4,new BigDecimal("150000"));
  var sinGiro=servicios.reservas.crear(cliente.getId(),otroInmueble.getId(),HOY.plusDays(10),HOY.plusDays(13),2);servicios.pagos.registrar(cliente.getId(),sinGiro.getId(),MetodoPago.TRANSFERENCIA,"HISTORICO-SIN-GIRO");
  try(Connection c=ConexionBD.abrir()){MigrarGirosAutomaticos.migrar(c);MigrarGirosAutomaticos.migrar(c);}
  var leido=giros.buscarPorId(humano.getId()).orElseThrow();
  assertEquals(new BigDecimal("405000.00"),leido.getMonto());assertEquals(new BigDecimal("0.1000"),leido.getPorcentajeComision());assertEquals(admin.getId(),leido.getAdministradorId());assertEquals("ADMINISTRADOR",leido.getOrigenAutorizacion());
  assertEquals(new BigDecimal("0.1000"),reservas.comisionHistorica(antigua.getId()).orElseThrow());
  assertEquals(new BigDecimal("0.3000"),reservas.comisionHistorica(sinGiro.getId()).orElseThrow());
  new ComisionService().cambiar(admin.getId(),new BigDecimal("50"));
  long pais;try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT min(id_ps) FROM paises")){r.next();pais=r.getLong(1);}
  var datos=new DatosCuentaService();datos.crearBanco(admin.getId(),"Banco de migracion",pais);datos.guardar(propietario.getId(),Rol.PROPIETARIO,new CuentaBancaria(datos.bancos().get(0).id(),"0000456789","AHORROS"),false);
  alFinalizar(sinGiro);new CicloReservas().ejecutar();new CicloReservas().ejecutar();
  assertEquals(2,giros.listar().size());assertEquals(new BigDecimal("315000.00"),giros.buscarPorReserva(sinGiro.getId()).orElseThrow().getMonto());
  assertEquals(new BigDecimal("405000.00"),giros.buscarPorId(humano.getId()).orElseThrow().getMonto());
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT cuenta_bancaria_pactada FROM giro WHERE id_giro="+humano.getId())){r.next();assertNull(r.getObject(1));}
 }
}
