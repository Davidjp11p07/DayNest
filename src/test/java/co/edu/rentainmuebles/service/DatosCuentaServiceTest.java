package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class DatosCuentaServiceTest extends SoportePostgres {
 @Override protected boolean sembrarCuentas(){return false;}
 final DatosCuentaService datos=new DatosCuentaService();long banco,banco2;
 @BeforeEach void bancos()throws Exception {
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id_pais FROM pais ORDER BY id_pais LIMIT 1")){r.next();long pais=r.getLong(1);datos.crearBanco(admin.getId(),"Banco prueba A",pais);datos.crearBanco(admin.getId(),"Banco prueba B",pais);}
  banco=datos.bancos().get(0).id();banco2=datos.bancos().get(1).id();
 }
 @Test void cuentasIndependientesYCopiaExplicita(){
  servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO);
  datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco,"0000123456","AHORROS"),true);
  assertEquals(datos.consultar(cliente.getId(),Rol.CLIENTE).cuenta(),datos.consultar(cliente.getId(),Rol.PROPIETARIO).cuenta());
  datos.guardar(cliente.getId(),Rol.PROPIETARIO,new CuentaBancaria(banco2,"0000998877","CORRIENTE"),false);
  assertEquals("0000123456",datos.consultar(cliente.getId(),Rol.CLIENTE).cuenta().numeroCuenta());
  assertEquals(banco2,datos.consultar(cliente.getId(),Rol.PROPIETARIO).cuenta().bancoId());
 }
 @Test void copiaSinSegundoPerfilEsAtomica(){
  assertThrows(NegocioException.class,()->datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco,"0000123456","AHORROS"),true));
  assertNull(datos.consultar(cliente.getId(),Rol.CLIENTE).cuenta());
 }
 @Test void activacionNoBorraCuentaOriginal(){
  datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco,"0000123456","AHORROS"),false);
  servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO);
  assertNotNull(datos.consultar(cliente.getId(),Rol.CLIENTE).cuenta());assertNull(datos.consultar(cliente.getId(),Rol.PROPIETARIO).cuenta());
 }
 @Test void bancoInexistenteNoSobrescribe(){
  var original=new CuentaBancaria(banco,"0000123456","AHORROS");datos.guardar(cliente.getId(),Rol.CLIENTE,original,false);
  assertThrows(NegocioException.class,()->datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(999999,"00001111","CORRIENTE"),false));
  assertEquals(original,datos.consultar(cliente.getId(),Rol.CLIENTE).cuenta());
 }
 @Test void cuentasInvalidasRechazadas(){assertThrows(IllegalArgumentException.class,()->new CuentaBancaria(banco,"1e10","AHORROS"));assertThrows(IllegalArgumentException.class,()->new CuentaBancaria(banco,"123456","OTRO"));}
 @Test void residenciaSeGuardaSoloCliente()throws Exception {
  long ciudad=servicios.catalogo.ciudades().get(0).id();datos.residencia(cliente.getId(),ciudad);assertEquals(ciudad,datos.consultar(cliente.getId(),Rol.CLIENTE).ciudadId());
  assertThrows(NegocioException.class,()->datos.residencia(propietario.getId(),ciudad));
  assertThrows(NegocioException.class,()->datos.residencia(cliente.getId(),999999));
  assertEquals(ciudad,datos.consultar(cliente.getId(),Rol.CLIENTE).ciudadId());
 }
 @Test void cuentaInactivaNoPuedeLeerNiGuardar(){usuarios.desactivar(cliente.getId());assertThrows(NegocioException.class,()->datos.consultar(cliente.getId(),Rol.CLIENTE));assertThrows(NegocioException.class,()->datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco,"123456","AHORROS"),false));}
 @Test void soloAdministradorCreaBanco(){assertThrows(NegocioException.class,()->datos.crearBanco(cliente.getId(),"No autorizado",1));assertEquals(2,datos.bancos().size());}
 @Test void historicoFinancieroNoCambiaConLaCuenta()throws Exception {
  datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco,"0000123456","AHORROS"),false);
  datos.guardar(propietario.getId(),Rol.PROPIETARIO,new CuentaBancaria(banco,"0000765432","CORRIENTE"),false);
  var r=reservaPagada();alFinalizar(r);var g=servicios.giros.generar(admin.getId(),r.getId(),null);
  datos.guardar(propietario.getId(),Rol.PROPIETARIO,new CuentaBancaria(banco2,"0000999999","AHORROS"),false);
  datos.guardar(cliente.getId(),Rol.CLIENTE,new CuentaBancaria(banco2,"0000888888","CORRIENTE"),false);
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()) {
   try(ResultSet x=s.executeQuery("SELECT cuenta_bancaria_pactada->>'numero_cuenta' FROM giro WHERE id_giro="+g.getId())){x.next();assertEquals("0000765432",x.getString(1));}
   try(ResultSet x=s.executeQuery("SELECT cuenta_bancaria_pactada->>'numero_cuenta' FROM movimiento_pago WHERE id_reserva="+r.getId())){x.next();assertEquals("0000123456",x.getString(1));}
   assertThrows(SQLException.class,()->s.execute("UPDATE giro SET cuenta_bancaria_pactada='{}' WHERE id_giro="+g.getId()));
  }
 }
 @Test void areaHabitacionesYPrecioEnAsocia()throws Exception {
  var i=servicios.inmuebles.publicar(propietario.getId(),"Casa detallada","Bogota","Calle 5","Prueba",4,new BigDecimal("100000"),"CASA",false,false,new BigDecimal("80.50"),3);
  assertEquals(new BigDecimal("80.50"),i.getAreaM2());assertEquals(3,i.getHabitaciones());
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT precio_noche FROM asocia_operativa WHERE id_inmueble="+i.getId())){r.next();assertEquals(new BigDecimal("100000.00"),r.getBigDecimal(1));}
  assertThrows(NegocioException.class,()->servicios.inmuebles.editar(propietario.getId(),i.getId(),"Casa detallada","Bogota","Calle 5","Prueba",4,new BigDecimal("100000"),"CASA",false,false,new BigDecimal("-1"),3));
 }
}
