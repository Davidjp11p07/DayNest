package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.verification.MigrarPagosTransferencia;
import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class TransferenciasTest extends SoportePostgres {
 @Test void instalacionNuevaTieneComisionInicialDelTreintaPorCiento()throws Exception {
  // El soporte reinicia el catalogo e inserta tasa cero antes de cada prueba.
  // Verificar la instalacion real en otro esquema creado dentro de una transaccion reversible.
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   String esquema="renta_test_"+java.util.UUID.randomUUID().toString().replace("-","");
   c.setAutoCommit(false);
   try {
    s.execute("CREATE SCHEMA "+esquema);s.execute("SET LOCAL search_path TO "+esquema+",public");
    s.execute(java.nio.file.Files.readString(java.nio.file.Path.of("sql/instalacion/schema.sql")).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$",""));
    try(ResultSet r=s.executeQuery("SELECT porcentaje FROM configuracion_comision ORDER BY id_configuracion LIMIT 1")){
     assertTrue(r.next());assertEquals(new java.math.BigDecimal("0.3000"),r.getBigDecimal(1));
    }
   }finally{c.rollback();}
  }
 }
 private long banco(){return new DatosCuentaService().bancos().get(0).id();}
 private CuentaBancaria otra(){return new CuentaBancaria(banco(),"000055556666","CORRIENTE");}
 private String dato(long pago,String clave)throws Exception {
  try(Connection c=ConexionBD.abrir();PreparedStatement s=c.prepareStatement("SELECT cuenta_bancaria_pactada->>? FROM movimiento_pago WHERE id_movimiento=?")){
   s.setString(1,clave);s.setLong(2,pago);try(ResultSet r=s.executeQuery()){r.next();return r.getString(1);}
  }
 }
 private void sql(String texto)throws Exception{try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute(texto);}}
 @Test void registradaConservaMetodoYCerosYNoCambiaAlEditarPerfil()throws Exception {
  var r=reservaFutura();var p=servicios.pagos.registrarTransferencia(cliente.getId(),r.getId(),"PRUEBA",null);
  assertEquals("TRANSFERENCIA",dato(p.getId(),"metodo"));assertEquals("REGISTRADA",dato(p.getId(),"uso"));
  new DatosCuentaService().guardar(cliente.getId(),Rol.CLIENTE,otra(),false);
  assertEquals("0000123456",dato(p.getId(),"numero_cuenta"));assertEquals("****3456",pagos.buscarPorId(p.getId()).orElseThrow().getNumeroCuentaPago());
  assertEquals(MetodoPago.TRANSFERENCIA,pagos.buscarPorId(p.getId()).orElseThrow().getMetodo());
 }
 @Test void ocasionalNoModificaNingunoDeLosDosPerfiles()throws Exception {
  servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO);
  var datos=new DatosCuentaService();datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco(),"000011112222","AHORROS"),true);
  var p=servicios.pagos.registrarTransferencia(cliente.getId(),reservaFutura().getId(),null,otra());
  assertEquals("000011112222",datos.consultar(cliente.getId(),Rol.CLIENTE).cuenta().numeroCuenta());
  assertEquals("000011112222",datos.consultar(cliente.getId(),Rol.PROPIETARIO).cuenta().numeroCuenta());
  assertEquals("000055556666",dato(p.getId(),"numero_cuenta"));assertEquals("OCASIONAL",dato(p.getId(),"uso"));
 }
 @Test void ocasionalFuncionaSinPredeterminadaYNoCreaUna()throws Exception {
  sql("DELETE FROM vincula");var r=reservaFutura();
  var p=servicios.pagos.registrarTransferencia(cliente.getId(),r.getId(),null,otra());
  assertEquals(EstadoReserva.CONFIRMADA,p.getReserva().getEstado());
  assertNull(new DatosCuentaService().consultar(cliente.getId(),Rol.CLIENTE).cuenta());
 }
 @Test void bancoInexistenteRevierteCobroYConfirmacion() {
  var r=reservaFutura();assertThrows(NegocioException.class,()->servicios.pagos.registrarTransferencia(cliente.getId(),r.getId(),null,new CuentaBancaria(99999999,"12345678","AHORROS")));
  assertTrue(pagos.listarPorReserva(r.getId()).isEmpty());assertEquals(EstadoReserva.PENDIENTE_PAGO,reservas.buscarPorId(r.getId()).orElseThrow().getEstado());
  assertTrue(reservas.comisionHistorica(r.getId()).isEmpty());
 }
 @Test void rechazaTarjetaYEfectivoSinCrearCobros() {
  var r=reservaFutura();
  for(var metodo:new MetodoPago[]{MetodoPago.TARJETA,MetodoPago.EFECTIVO})assertThrows(NegocioException.class,()->servicios.pagos.registrar(cliente.getId(),r.getId(),metodo,null));
  assertTrue(pagos.listarPorReserva(r.getId()).isEmpty());
 }
 @Test void noPermitePagarReservaAjena() {
  var r=reservaFutura();assertThrows(NegocioException.class,()->servicios.pagos.registrarTransferencia(otroCliente.getId(),r.getId(),null,otra()));assertTrue(pagos.listar().isEmpty());
 }
 @Test void devolucionUtilizaCuentaOcasionalAunqueCambieLaRegistrada()throws Exception {
  var r=reservaFutura();var p=servicios.pagos.registrarTransferencia(cliente.getId(),r.getId(),null,otra());
  new DatosCuentaService().guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco(),"000077778888","AHORROS"),false);
  servicios.reservas.cancelar(cliente.getId(),r.getId());
  var dev=pagos.listarPorReserva(r.getId()).stream().filter(x->x.getClase().equals("DEVOLUCION")).findFirst().orElseThrow();
  assertEquals(dato(p.getId(),"numero_cuenta"),dato(dev.getId(),"numero_cuenta"));assertEquals("TRANSFERENCIA",dato(dev.getId(),"metodo"));
 }
 @Test void cuentaHistoricaEsInmutable()throws Exception {
  var p=servicios.pagos.registrarTransferencia(cliente.getId(),reservaFutura().getId(),null,otra());
  assertThrows(SQLException.class,()->sql("UPDATE movimiento_pago SET cuenta_bancaria_pactada='{}' WHERE id_movimiento="+p.getId()));
 }
 @Test void migracionNoInventaMetodoHistoricoYEsReejecutable()throws Exception {
  // Reponer las dos funciones anteriores a esta mejora SOLO en el esquema UUID aislado.
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   assertTrue(c.getSchema().matches("renta_test_[a-f0-9]{32}"));
   String anterior=java.nio.file.Files.readString(java.nio.file.Path.of("sql/instalacion/schema.sql"));
   for(String funcion:new String[]{"guardar_cuenta_financiera","fijar_comision_cobro"}){
    var patron=java.util.regex.Pattern.compile("CREATE FUNCTION "+funcion+"\\(\\).*?\\$\\$;",java.util.regex.Pattern.DOTALL).matcher(anterior);
    assertTrue(patron.find());s.execute(patron.group().replaceFirst("CREATE FUNCTION","CREATE OR REPLACE FUNCTION"));
   }
  }
  var p=servicios.pagos.registrarTransferencia(cliente.getId(),reservaFutura().getId(),null,null);
  try(Connection c=ConexionBD.abrir()){
   MigrarPagosTransferencia.migrar(c);MigrarPagosTransferencia.migrar(c);
  }
  assertNull(dato(p.getId(),"metodo"));assertNull(pagos.buscarPorId(p.getId()).orElseThrow().getMetodo());
  assertEquals("No registrado (historico)",pagos.buscarPorId(p.getId()).orElseThrow().getMetodoRegistrado());
 }
 @Test void dobleEnvioNoDuplicaCobro() {
  var r=reservaFutura();servicios.pagos.registrarTransferencia(cliente.getId(),r.getId(),null,otra());
  assertThrows(NegocioException.class,()->servicios.pagos.registrarTransferencia(cliente.getId(),r.getId(),null,otra()));
  assertEquals(1,pagos.listarPorReserva(r.getId()).size());
 }
}
