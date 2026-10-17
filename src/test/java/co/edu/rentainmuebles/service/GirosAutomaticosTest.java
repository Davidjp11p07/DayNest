package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import org.junit.jupiter.api.*;
import java.math.*;import java.sql.*;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class GirosAutomaticosTest extends SoportePostgres {
 final ComisionService comisiones=new ComisionService();
 @BeforeEach void tasa(){comisiones.cambiar(admin.getId(),new BigDecimal("30"));}
 private void ciclo(){new CicloReservas().ejecutar();}
 private long numero(String sql)throws Exception {try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 private void sql(String sql)throws Exception {try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){assertTrue(c.getSchema().matches("renta_test_[a-f0-9]{32}"));s.execute(sql);}}
 private String texto(String sql)throws Exception {try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getString(1);}}
 @Test void pagadaFuturaNoGeneraGiro(){reservaPagada();ciclo();assertTrue(giros.listar().isEmpty());}
 @Test void finalizadaCobraTreintaPorCientoYAutorSistema()throws Exception {
  var r=reservaPagada();alFinalizar(r);ciclo();var g=giros.buscarPorReserva(r.getId()).orElseThrow();
  assertEquals(new BigDecimal("315000.00"),g.getMonto());assertEquals(new BigDecimal("135000.00"),g.getMontoComision());
  assertEquals("SISTEMA",g.getOrigenAutorizacion());assertNull(g.getAdministradorId());assertEquals(EstadoGiro.EJECUTADO,g.getEstado());
  assertEquals(EstadoReserva.FINALIZADA,reservas.buscarPorId(r.getId()).orElseThrow().getEstado());
 }
 @Test void sinPagoCompletoNoEjecuta()throws Exception {
  var r=reservaFutura();pagos.crear(new Pago(null,r,new BigDecimal("1000"),null,r.getFechaCreacion(),EstadoPago.APROBADO,"COBRO",null,"parcial"));
  alFinalizar(r);sql("ALTER TABLE renta DISABLE TRIGGER tg_validar_reserva;UPDATE reserva SET estado='FINALIZADA' WHERE id_ocupacion="+r.getId()+";SET CONSTRAINTS ALL IMMEDIATE;ALTER TABLE renta ENABLE TRIGGER tg_validar_reserva");
  ciclo();assertTrue(giros.listar().isEmpty());assertTrue(texto("SELECT motivo FROM liquidacion_giro").contains("incompleto"));
 }
 @Test void canceladaNoEjecuta(){var r=reservaPagada();servicios.reservas.cancelar(cliente.getId(),r.getId());alFinalizar(r);ciclo();assertTrue(giros.listar().isEmpty());}
 @Test void ciclosRepetidosSonIdempotentes()throws Exception{var r=reservaPagada();alFinalizar(r);ciclo();ciclo();ciclo();assertEquals(1,numero("SELECT count(*) FROM giro WHERE estado='EJECUTADO'"));}
 @Test void concurrenciaNoDuplica()throws Exception {
  var r=reservaPagada();alFinalizar(r);var pool=Executors.newFixedThreadPool(2);
  try{var a=pool.submit(this::ciclo);var b=pool.submit(this::ciclo);a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  assertEquals(1,numero("SELECT count(*) FROM giro"));
 }
 @Test void cambiarTasaNoModificaOperacionPagada(){
  var r=reservaPagada();comisiones.cambiar(admin.getId(),new BigDecimal("45"));alFinalizar(r);ciclo();assertEquals(new BigDecimal("0.3000"),giros.buscarPorReserva(r.getId()).orElseThrow().getPorcentajeComision());
  var nueva=nuevaReserva(HOY.plusDays(20),HOY.plusDays(22));servicios.pagos.registrar(cliente.getId(),nueva.getId(),MetodoPago.TARJETA,null);assertEquals(new BigDecimal("0.4500"),reservas.comisionHistorica(nueva.getId()).orElseThrow());
 }
 @Test void cambiarCuentaNoModificaGiroHistorico()throws Exception {
  var r=reservaPagada();alFinalizar(r);ciclo();long banco=numero("SELECT min(id_b) FROM banco");new DatosCuentaService().guardar(propietario.getId(),Rol.PROPIETARIO,new CuentaBancaria(banco,"0000777788","AHORROS"),false);
  assertEquals("0000987654",texto("SELECT cuenta_bancaria_pactada->>'numero_cuenta' FROM giro"));
 }
 @Test void sinCuentaRegistraMotivoYLuegoReintenta()throws Exception {
  var r=reservaPagada();alFinalizar(r);long banco=numero("SELECT min(id_b) FROM banco");sql("DELETE FROM relaciona");ciclo();assertTrue(giros.listar().isEmpty());assertTrue(texto("SELECT motivo FROM liquidacion_giro").contains("cuenta bancaria"));
  new DatosCuentaService().guardar(propietario.getId(),Rol.PROPIETARIO,new CuentaBancaria(banco,"0000666699","AHORROS"),false);comisiones.reintentar(admin.getId(),r.getId());assertEquals(EstadoGiro.EJECUTADO,giros.buscarPorReserva(r.getId()).orElseThrow().getEstado());assertTrue(texto("SELECT historial::text FROM liquidacion_giro").contains("REVISION_ADMIN"));
 }
 @Test void nuevaInstanciaProcesaEstanciaFinalizadaDuranteApagado(){var r=reservaPagada();alFinalizar(r);new CicloReservas().ejecutar();assertTrue(giros.buscarPorReserva(r.getId()).isPresent());}
 @Test void falloTransaccionalYReintentoNoDuplican()throws Exception {
  var r=reservaPagada();alFinalizar(r);
  sql("CREATE FUNCTION fallo_simulado() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.estado='EJECUTADO' THEN RAISE EXCEPTION 'Fallo de prueba';END IF; RETURN NEW;END;$$;CREATE TRIGGER zz_fallo_simulado BEFORE UPDATE ON giro FOR EACH ROW EXECUTE FUNCTION fallo_simulado()");
  ciclo();assertEquals(0,numero("SELECT count(*) FROM giro"));assertEquals("FALLIDA",texto("SELECT estado FROM liquidacion_giro"));
  sql("DROP TRIGGER zz_fallo_simulado ON giro");ciclo();ciclo();assertEquals(1,numero("SELECT count(*) FROM giro"));assertEquals("EJECUTADA",texto("SELECT estado FROM liquidacion_giro"));assertEquals(2,numero("SELECT jsonb_array_length(historial) FROM liquidacion_giro"));
 }
 @Test void cuentaSeCapturaAlEjecutarNoAlCrearPendiente()throws Exception {
  var r=reservaPagada();var tasa=new BigDecimal("0.3000");giros.crear(new Giro(null,r,propietario,new BigDecimal("315000"),null,r.getFechaCreacion(),EstadoGiro.PENDIENTE,null,"pendiente",tasa,new BigDecimal("135000")));
  long banco=numero("SELECT min(id_b) FROM banco");new DatosCuentaService().guardar(propietario.getId(),Rol.PROPIETARIO,new CuentaBancaria(banco,"0000222233","AHORROS"),false);alFinalizar(r);ciclo();assertEquals("0000222233",texto("SELECT cuenta_bancaria_pactada->>'numero_cuenta' FROM giro"));
 }
 @Test void propietarioPactadoSeConservaAunqueCambieElInmueble()throws Exception {
  var r=reservaPagada();var otro=usuarios.crear(new Usuario(null,"Otro propietario","otro-prop@prueba.test",cliente.getHashContrasena(),null,true,false,false,true));
  sql("UPDATE inmueble_operativo SET id_propietario="+otro.getId()+" WHERE id_inmueble="+inmueble.getId());alFinalizar(r);ciclo();assertEquals(propietario.getId(),giros.buscarPorReserva(r.getId()).orElseThrow().getPropietario().getId());
 }
 @Test void permisosRangoYVersionesDeComision()throws Exception {
  assertThrows(NegocioException.class,()->comisiones.cambiar(cliente.getId(),BigDecimal.TEN));assertThrows(NegocioException.class,()->comisiones.reintentar(cliente.getId(),999));assertThrows(NegocioException.class,()->comisiones.cambiar(admin.getId(),new BigDecimal("100")));assertThrows(NegocioException.class,()->comisiones.cambiar(admin.getId(),new BigDecimal("0.001")));
  comisiones.cambiar(admin.getId(),new BigDecimal("12.34"));assertEquals(new BigDecimal("0.1234"),comisiones.porcentaje(admin.getId()));assertThrows(SQLException.class,()->sql("UPDATE configuracion_comision SET porcentaje=0"));
 }
 @Test void sinCuentaClienteNoSeApruebaPago()throws Exception{sql("DELETE FROM vincula");var r=reservaFutura();assertThrows(NegocioException.class,()->servicios.pagos.registrar(cliente.getId(),r.getId(),MetodoPago.TARJETA,null));assertTrue(pagos.listar().isEmpty());}
 @Test void giroFallidoHistoricoNoBloqueaUnaLiquidacionNueva()throws Exception {
  var r=reservaPagada();
  var previo=giros.crear(new Giro(null,r,propietario,new BigDecimal("315000"),"SIM-AUTO-"+r.getId(),r.getFechaCreacion(),EstadoGiro.FALLIDO,null,"liquidacion-reserva-"+r.getId(),new BigDecimal("0.3000"),new BigDecimal("135000")));
  alFinalizar(r);ciclo();ciclo();
  assertEquals(2,numero("SELECT count(*) FROM giro"));assertEquals(1,numero("SELECT count(*) FROM giro WHERE estado='EJECUTADO'"));assertEquals(EstadoGiro.FALLIDO,giros.buscarPorId(previo.getId()).orElseThrow().getEstado());
 }
}
