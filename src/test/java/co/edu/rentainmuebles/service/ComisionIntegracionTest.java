package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.*;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.impl.PoliticaComisionPorcentaje;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ComisionIntegracionTest extends SoportePostgres {
 @Test void comisionPersistidaHistoricaEInmutable() throws Exception {
  new ComisionService().cambiar(admin.getId(),new BigDecimal("10"));
  Reserva r=reservaPagada();alFinalizar(r);
  Servicios diez=new Servicios(unidad,RELOJ,new PoliticaComisionPorcentaje(new BigDecimal("0.1000")));
  Giro g=diez.giros.generar(admin.getId(),r.getId(),"COMISION-TEST");
  Giro leido=giros.buscarPorId(g.getId()).orElseThrow();
  assertEquals(new BigDecimal("45000.00"),leido.getMontoComision());
  assertEquals(new BigDecimal("405000.00"),leido.getMonto());
  assertEquals(new BigDecimal("0.1000"),leido.getPorcentajeComision());
  assertEquals(leido.getMontoComision(),servicios.giros.listarPropios(propietario.getId()).get(0).getMontoComision());
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()) {
   assertThrows(SQLException.class,()->s.executeUpdate("UPDATE giro SET porcentaje_comision=0 WHERE id_giro="+g.getId()));
   assertThrows(SQLException.class,()->s.executeUpdate("UPDATE giro SET monto_comision=0 WHERE id_giro="+g.getId()));
  }
 }
 @Test void configuracionRealSeLee() {
  assertNotNull(Servicios.crear());
 }
 @Test void consultaPropietarioUsaBeneficiarioHistorico() {
  Reserva r=reservaFutura();
  assertEquals(r.getId(),reservas.buscarPorPropietario(propietario.getId()).get(0).getId());
  assertTrue(reservas.buscarPorPropietario(admin.getId()).isEmpty());
 }
 @Test void poolReutilizaConexionYRevierteTransaccion() throws Exception {
  long pid;
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT pg_backend_pid()")) {rs.next();pid=rs.getLong(1);c.setAutoCommit(false);s.executeUpdate("UPDATE usuario SET nombre_completo='NO CONSERVAR' WHERE id_usuario="+cliente.getId());}
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT pg_backend_pid()")) {rs.next();assertEquals(pid,rs.getLong(1));assertTrue(c.getAutoCommit());}
  assertEquals(cliente.getNombre(),usuarios.buscarPorId(cliente.getId()).orElseThrow().getNombre());
 }
 @Test void poolCierraYPuedeReiniciarse() throws Exception {
  ConexionBD.cerrar();
  try(Connection c=ConexionBD.abrir()){assertTrue(c.isValid(2));assertTrue(c.getSchema().startsWith("renta_test_"));}
 }
 @Test void poolRestableceEsquema() throws Exception {
  String actual;
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){actual=c.getSchema();s.execute("SET search_path TO public");}
  try(Connection c=ConexionBD.abrir()){assertEquals(actual,c.getSchema());}
 }
}
