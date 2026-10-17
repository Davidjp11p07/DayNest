package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class IngresosServiceTest extends SoportePostgres {
 final IngresosService ingresos=new IngresosService();
 @BeforeEach void tasa(){new ComisionService().cambiar(admin.getId(),new BigDecimal("30"));}
 private void liquidar(Reserva r){alFinalizar(r);new CicloReservas().ejecutar();}
 private IngresosService.Informe administrador(){return ingresos.consultar(admin.getId(),true,0,1);}
 private void importe(String esperado,BigDecimal actual){assertEquals(0,new BigDecimal(esperado).compareTo(actual));}
 private void sql(String sql)throws Exception{try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){assertTrue(c.getSchema().matches("renta_test_[a-f0-9]{32}"));s.execute(sql);}}

 @Test void inmuebleSinIngresosApareceUnaVezConCeros(){
  var informe=ingresos.consultar(propietario.getId(),false,0,1);
  assertEquals(1,informe.inmuebles().size());assertEquals(0,informe.totalRentas());
  importe("0",informe.totales().get(0).netoLiquidado());assertTrue(informe.detalle().isEmpty());
 }
 @Test void reservaFuturaPagadaEsPendienteYNoComisionRealizada(){
  reservaPagada();var t=administrador().totales().get(0);
  importe("0",t.comisionLiquidada());importe("135000",t.comisionPendiente());
  importe("315000",t.netoPendiente());importe("0",t.netoLiquidado());
 }
 @Test void giroEjecutadoSeCuentaUnaSolaVezConVariosCobros(){
  var r=reservaFutura();pagos.crear(new Pago(null,r,new BigDecimal("100000"),"PARCIAL",r.getFechaCreacion(),MetodoPago.TRANSFERENCIA));
  servicios.pagos.registrar(cliente.getId(),r.getId(),MetodoPago.TRANSFERENCIA,"RESTO");
  liquidar(r);new CicloReservas().ejecutar();var informe=administrador();var t=informe.totales().get(0);
  assertEquals(1,t.reservas());assertEquals(1,informe.detalle().size());
  importe("135000",t.comisionLiquidada());importe("315000",t.netoLiquidado());
  importe("450000",t.netoCobrado());importe("0",t.comisionPendiente());
 }
 @Test void cancelacionDevueltaNoSeCuentaComoIngreso(){
  var r=reservaPagada();servicios.reservas.cancelar(cliente.getId(),r.getId());var t=administrador().totales().get(0);
  importe("0",t.netoCobrado());importe("0",t.netoLiquidado());importe("0",t.netoPendiente());importe("0",t.comisionLiquidada());importe("0",t.comisionPendiente());
 }
 @Test void comisionHistoricaNoCambiaConLaActual(){
  var r=reservaPagada();new ComisionService().cambiar(admin.getId(),new BigDecimal("45"));liquidar(r);
  var informe=administrador();importe("135000",informe.totales().get(0).comisionLiquidada());importe("0.30",informe.detalle().get(0).porcentaje());
 }
 @Test void variasPropiedadesSeAgrupanPorIdSinPerderLasQueNoTienenRentas(){
  var otra=servicios.inmuebles.publicar(propietario.getId(),"Otra propiedad","Bogota","Calle 2","Prueba",2,new BigDecimal("100000"));
  var r=reservaPagada();liquidar(r);
  var informe=ingresos.consultar(propietario.getId(),false,0,1);assertEquals(2,informe.inmuebles().size());
  var sinRentas=informe.inmuebles().stream().filter(i->i.id()==otra.getId()).findFirst().orElseThrow();assertEquals(0,sinRentas.importe().reservas());
 }
 @Test void monedasSeMantienenSeparadas()throws Exception {
  var r=reservaPagada();liquidar(r);
  var euros=servicios.inmuebles.publicar(propietario.getId(),"Casa EUR","Bogota","Calle 3","Prueba",2,new BigDecimal("100"));
  sql("UPDATE inmuebles SET moneda='EUR' WHERE id_i="+euros.getId());
  var segunda=servicios.reservas.crear(cliente.getId(),euros.getId(),HOY.plusDays(20),HOY.plusDays(22),1);
  servicios.pagos.registrar(cliente.getId(),segunda.getId(),MetodoPago.TRANSFERENCIA,"EUR");liquidar(segunda);
  var totales=administrador().totales();assertEquals(2,totales.size());
  importe("60",totales.stream().filter(t->t.moneda().equals("EUR")).findFirst().orElseThrow().comisionLiquidada());
  importe("135000",totales.stream().filter(t->t.moneda().equals("COP")).findFirst().orElseThrow().comisionLiquidada());
 }
 @Test void propietarioNoVeNiFiltraDatosAjenos(){
  var otro=usuarios.crear(new Usuario(null,"Otro propietario","otro.prop@junit.test",cliente.getHashContrasena(),null,true,false,false,true)).getPropietario();
  reservaPagada();var informe=ingresos.consultar(otro.getId(),false,inmueble.getId(),1);
  assertTrue(informe.inmuebles().isEmpty());assertTrue(informe.detalle().isEmpty());
  assertThrows(NegocioException.class,()->ingresos.consultar(propietario.getId(),true,0,1));
  assertThrows(NegocioException.class,()->ingresos.consultar(cliente.getId(),false,0,1));
 }
 @Test void cambioDePropietarioNoTrasladaIngresosHistoricos()throws Exception {
  var r=reservaPagada();liquidar(r);
  var otro=usuarios.crear(new Usuario(null,"Comprador","comprador@junit.test",cliente.getHashContrasena(),null,true,false,false,true)).getPropietario();
  sql("UPDATE asocia SET id_p="+otro.getId()+" WHERE id_i="+inmueble.getId());
  importe("315000",ingresos.consultar(propietario.getId(),false,0,1).totales().get(0).netoLiquidado());
  var nuevo=ingresos.consultar(otro.getId(),false,0,1);assertEquals(1,nuevo.inmuebles().size());assertEquals(0,nuevo.totalRentas());importe("0",nuevo.totales().get(0).netoLiquidado());
 }
 @Test void girosFallidosNoDuplicanElIngreso() {
  var r=reservaPagada();giros.crear(new Giro(null,r,propietario,new BigDecimal("315000"),"FALLO",r.getFechaCreacion(),EstadoGiro.FALLIDO,null,"fallo-ingresos",new BigDecimal("0.30"),new BigDecimal("135000")));
  liquidar(r);var t=administrador().totales().get(0);assertEquals(1,t.reservas());importe("135000",t.comisionLiquidada());importe("315000",t.netoLiquidado());
 }
 @Test void pagoParcialNoSePresentaComoGananciaPendiente(){
  var r=reservaFutura();pagos.crear(new Pago(null,r,new BigDecimal("10000"),null,r.getFechaCreacion(),MetodoPago.TRANSFERENCIA));
  var t=administrador().totales().get(0);importe("10000",t.netoCobrado());importe("0",t.comisionPendiente());importe("0",t.netoPendiente());
 }
 @Test void paginaFueraDeRangoYFiltroSonCoherentes(){
  reservaPagada();var informe=ingresos.consultar(admin.getId(),true,inmueble.getId(),Integer.MAX_VALUE);
  assertEquals(1,informe.pagina());assertEquals(1,informe.paginas());assertEquals(1,informe.detalle().size());
  assertThrows(NegocioException.class,()->ingresos.consultar(admin.getId(),true,-1,1));
 }
 @Test void historialPaginaSinRecortarLosTotales(){
  for(int n=0;n<21;n++)servicios.reservas.crear(cliente.getId(),inmueble.getId(),HOY.plusDays(30+3*n),HOY.plusDays(32+3*n),1);
  var primera=ingresos.consultar(propietario.getId(),false,0,1);
  var segunda=ingresos.consultar(propietario.getId(),false,0,2);
  assertEquals(21,primera.totalRentas());assertEquals(21,segunda.totales().get(0).reservas());
  assertEquals(20,primera.detalle().size());assertEquals(1,segunda.detalle().size());assertEquals(2,segunda.paginas());
  assertNotEquals(primera.detalle().get(0).reservaId(),segunda.detalle().get(0).reservaId());
 }
 @Test void usuarioInactivoNoPuedeConsultar(){
  usuarios.desactivar(propietario.getId());assertThrows(NegocioException.class,()->ingresos.consultar(propietario.getId(),false,0,1));
  usuarios.desactivar(admin.getId());assertThrows(NegocioException.class,()->ingresos.consultar(admin.getId(),true,0,1));
 }
}
