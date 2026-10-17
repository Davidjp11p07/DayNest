package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.dao.jdbc.CiudadDaoJdbc;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.verification.MigrarFiltros;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;import java.util.*;import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
class FiltrosInmuebleTest extends SoportePostgres {
 private Inmueble crear(String tipo,Boolean rural,Boolean mar,String ciudad,int capacidad,String precio){
  var c=new CiudadDaoJdbc().resolver(ciudad);
  return inmuebles.crear(new Inmueble(null,propietario,"Alojamiento clasificado",c,"Dirección de prueba","Prueba aislada",capacidad,new BigDecimal(precio),EstadoInmueble.PUBLICADO,tipo,c.getMoneda(),rural,mar));
 }
 private Set<Long> buscar(String tipo,Boolean rural,Boolean mar){return ids(inmuebles.buscar(null,null,null,null,null,new FiltrosInmueble(null,null,tipo,rural,mar)));}
 private Set<Long> ids(List<Inmueble> r){return r.stream().map(Inmueble::getId).collect(java.util.stream.Collectors.toSet());}
 @Test void cincoFiltrosExclusivosConSolapamientoYClasificacionPendiente(){
  var apto=crear("APARTAMENTO",false,true,"Bogotá",4,"120000");var casa=crear("CASA",false,true,"Bogotá",6,"250000");
  var cabana=crear("CABAÑA",true,false,"Bogotá",4,"180000");var ambos=crear("CASA",true,true,"Bogotá",5,"200000");
  var pendiente=crear(null,null,null,"Bogotá",3,"100000");
  assertEquals(Set.of(apto.getId()),buscar("APARTAMENTO",null,null));
  assertEquals(Set.of(casa.getId(),ambos.getId()),buscar("CASA",null,null));
  assertEquals(Set.of(cabana.getId()),buscar("CABAÑA",null,null));
  assertEquals(Set.of(cabana.getId(),ambos.getId()),buscar(null,true,null));
  assertEquals(Set.of(apto.getId(),casa.getId(),ambos.getId()),buscar(null,null,true));
  assertEquals(Set.of(cabana.getId()),buscar("CABAÑA",true,null));
  assertEquals(Set.of(casa.getId(),ambos.getId()),buscar("CASA",null,true));
  assertEquals(Set.of(ambos.getId()),buscar(null,true,true));
  assertTrue(buscar(null,null,null).contains(pendiente.getId()));
  assertFalse(buscar(null,false,null).contains(pendiente.getId()));
  assertEquals(Set.of(cabana.getId()),buscar(null,null,false));
 }
 @Test void combinaPaisRegionCiudadHomonimaPrecioCapacidadYOrden(){
  var dao=new CiudadDaoJdbc();var co=dao.listar().stream().filter(c->c.nombre().equals("Cartagena")&&c.division().pais().codigoIso2().equals("CO")).findFirst().orElseThrow();
  var es=dao.listar().stream().filter(c->c.nombre().equals("Cartagena")&&c.division().pais().codigoIso2().equals("ES")).findFirst().orElseThrow();
  var colombia=crear("CABAÑA",true,false,"id:"+co.id(),4,"180000");var espana=crear("CABAÑA",true,false,"id:"+es.id(),4,"100");
  assertEquals(Set.of(colombia.getId()),ids(inmuebles.buscar("id:"+co.id(),new BigDecimal("100000"),new BigDecimal("190000"),4,null,new FiltrosInmueble(co.division().pais().id(),co.division().id(),"CABAÑA",true,null))));
  assertEquals(Set.of(espana.getId()),ids(inmuebles.buscar("Cartagena",null,null,4,null,new FiltrosInmueble(es.division().pais().id(),null,"CABAÑA",null,null))));
  assertTrue(inmuebles.buscar("id:"+es.id(),null,null,null,null,new FiltrosInmueble(co.division().pais().id(),null,null,null,null)).isEmpty());
  assertEquals(List.of(colombia.getId(),espana.getId()),inmuebles.buscar("Cartagena",null,null,null,null,new FiltrosInmueble(null,null,"CABAÑA",null,null)).stream().map(Inmueble::getId).toList());
  assertEquals(34,dao.divisiones().size());assertEquals(2,dao.paises().size());
 }
 @Test void disponibilidadMantieneReservasPendientesBloqueosYFechasContiguas(){
  var a=crear("CABAÑA",true,null,"Bogotá",4,"170000");var b=crear("CABAÑA",true,null,"Bogotá",4,"160000");
  var inicio=HOY.plusDays(10);var fin=HOY.plusDays(13);servicios.reservas.crear(cliente.getId(),a.getId(),inicio,fin,2);
  new BloqueoService().crear(propietario.getId(),b.getId(),inicio,fin,"Prueba de filtros");
  var f=new FiltrosInmueble(null,null,"CABAÑA",true,null);
  assertTrue(servicios.inmuebles.buscar("Bogota",null,null,4,inicio,fin,f).isEmpty());
  assertEquals(Set.of(a.getId(),b.getId()),ids(servicios.inmuebles.buscar("Bogota",null,null,4,fin,fin.plusDays(2),f)));
  assertThrows(NegocioException.class,()->servicios.reservas.crear(otroCliente.getId(),a.getId(),inicio,fin,2));
 }
 @Test void validaTiposSinSQLYResultadoVacio(){
  assertThrows(IllegalArgumentException.class,()->new FiltrosInmueble(null,null,"CAMPO",null,null));
  assertThrows(IllegalArgumentException.class,()->new FiltrosInmueble(null,null,"CASA' OR 1=1--",null,null));
  assertThrows(IllegalArgumentException.class,()->new FiltrosInmueble(-1L,null,null,null,null));
  assertTrue(buscar("CABAÑA",null,null).isEmpty());
  assertEquals("CABAÑA",new FiltrosInmueble(null,null,"cabaña",null,null).tipo());
 }
 @Test void propietarioClasificaYEdicionAnteriorConservaClasificacion(){
  var i=servicios.inmuebles.publicar(propietario.getId(),"Pendiente","Bogotá","Calle","Texto",4,new BigDecimal("100000"));
  assertNull(i.getTipo());assertNull(i.getEsRural());assertNull(inmueble.getTipo());
  i=servicios.inmuebles.editar(propietario.getId(),i.getId(),i.getTitulo(),"Bogotá",i.getDireccion(),i.getDescripcion(),4,i.getPrecioPorNoche(),"CABAÑA",true,false);
  assertEquals("CABAÑA",i.getTipo());assertEquals(true,i.getEsRural());assertEquals(false,i.getCercaMar());
  var j=servicios.inmuebles.editar(propietario.getId(),i.getId(),"Nuevo título","Bogotá",i.getDireccion(),i.getDescripcion(),4,i.getPrecioPorNoche());
  assertEquals("CABAÑA",j.getTipo());assertEquals(true,j.getEsRural());assertEquals(false,j.getCercaMar());
  long id=j.getId();assertThrows(NegocioException.class,()->servicios.inmuebles.editar(cliente.getId(),id,"Título","Bogotá","Calle","Texto",4,new BigDecimal("100000"),"CASA",true,true));
 }
 @Test void consultasNoAlteranFinanzasYClasificacionNoCambiaTarifaPactada(){
  var r=reservaPagada();alFinalizar(r);var g=servicios.giros.generar(admin.getId(),r.getId(),"FILTROS-PRUEBA");
  var monto=g.getMonto();var total=r.getTotal();var cantidadPagos=pagos.listarPorReserva(r.getId()).size();
  servicios.inmuebles.editar(propietario.getId(),inmueble.getId(),inmueble.getTitulo(),"Bogotá",inmueble.getDireccion(),inmueble.getDescripcion(),4,new BigDecimal("200000"),"CASA",true,true);
  buscar("CASA",true,true);buscar("CABAÑA",true,null);
  var historica=reservas.buscarPorId(r.getId()).orElseThrow();assertEquals(total,historica.getTotal());assertEquals(cantidadPagos,pagos.listarPorReserva(r.getId()).size());assertEquals(monto,giros.buscarPorId(g.getId()).orElseThrow().getMonto());
 }
}
