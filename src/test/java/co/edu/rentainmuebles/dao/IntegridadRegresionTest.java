package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.NegocioException;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class IntegridadRegresionTest extends SoportePostgres {
 @Test void recargaNoRecreaAdministradorPersonalizado()throws Exception {
  try(Connection c=ConexionBD.abrir();Statement t=c.createStatement()) {
   t.execute(Files.readString(Path.of("sql/instalacion/data.sql")));t.execute(Files.readString(Path.of("sql/instalacion/data.sql")));
   try(ResultSet r=t.executeQuery("SELECT count(*) FROM administrador")){r.next();assertEquals(1,r.getInt(1));}
   try(ResultSet r=t.executeQuery("SELECT count(*) FROM usuario WHERE correo_normalizado='admin@renta.test'")){r.next();assertEquals(0,r.getInt(1));}
   assertThrows(SQLException.class,()->t.execute(Files.readString(Path.of("sql/demostracion/admin-demo.sql"))));
  }
 }
 @Test void giroPendienteInconsistenteRechazadoEnSQL()throws Exception {
  Reserva r=reservaPagada();
  try(Connection c=ConexionBD.abrir();Statement t=c.createStatement()) {
   assertThrows(SQLException.class,()->t.execute("INSERT INTO giro(id_reserva,monto,clave_idempotencia) VALUES("+r.getId()+",1,'inconsistente')"));
  }
  assertTrue(giros.listar().isEmpty());
 }
 @Test void vencimientoLoImponeBase() {
  Reserva r=reservas.crear(new Reserva(null,cliente,inmueble,new PeriodoEstadia(HOY.plusDays(10),HOY.plusDays(13)),2,inmueble.getPrecioPorNoche(),EstadoReserva.PENDIENTE_PAGO,LocalDateTime.now(ZONA),propietario,"COP",OffsetDateTime.now(ZONA).plusHours(2)));
  assertEquals(Duration.ofMinutes(30),Duration.between(r.getFechaCreacion(),r.getExpiraEn().atZoneSameInstant(ZONA).toLocalDateTime()));
 }
 @Test void vencidaNoSeCobraPorDAO()throws Exception {
  Reserva r=reservaFutura();vencerReservaParaPrueba(r.getId());
  assertThrows(DaoException.class,()->pagos.crear(new Pago(null,r,r.getTotal(),null,r.getFechaCreacion(),MetodoPago.TRANSFERENCIA)));
  assertThrows(NegocioException.class,()->servicios.pagos.registrar(cliente.getId(),r.getId(),MetodoPago.TRANSFERENCIA,null));
 }
 @Test void vencidaPagadaNoSeConfirmaPorSQL()throws Exception {
  Reserva r=reservaFutura();pagos.crear(new Pago(null,r,r.getTotal(),null,r.getFechaCreacion(),MetodoPago.TRANSFERENCIA));vencerReservaParaPrueba(r.getId());
  assertThrows(DaoException.class,()->reservas.actualizarEstado(r.getId(),EstadoReserva.CONFIRMADA));
  new co.edu.rentainmuebles.service.CicloReservas().ejecutar();
  assertEquals(EstadoReserva.EXPIRADA,reservas.buscarPorId(r.getId()).orElseThrow().getEstado());
  assertEquals(0,pagos.totalPagado(r.getId()).signum());
 }
 @Test void scriptsAnterioresNoReescribenModeloProfesor()throws Exception {
  try(Connection c=ConexionBD.abrir();Statement t=c.createStatement()) {
   c.setAutoCommit(false);
   for(String archivo:java.util.List.of("sql/migraciones/corregir-integridad.sql","sql/mantenimiento/revertir-svg.sql","sql/migraciones/migrar-svg.sql")) {
    String script=Files.readString(Path.of(archivo)).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$", "");
    assertThrows(SQLException.class,()->t.execute(script));c.rollback();
   }
  }
 }
}
