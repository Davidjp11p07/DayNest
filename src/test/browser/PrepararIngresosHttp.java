import co.edu.rentainmuebles.config.*;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.verification.PrepararVerificacionHttp;
import java.nio.file.*;
import java.sql.*;
import java.math.BigDecimal;

/** Ejecutar por Java source launcher con target/classes y target/lib en classpath.
 * Crea su propio esquema HTTP UUID; nunca modifica los datos operativos.
 */
class PrepararIngresosHttp {
 static long id(String correo)throws Exception {
  try(Connection c=ConexionBD.abrir();PreparedStatement s=c.prepareStatement("SELECT id_usuario FROM usuario WHERE correo_normalizado=?")){
   s.setString(1,correo);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}
  }
 }
 static Reserva pagar(Servicios s,long cliente,Inmueble inmueble,int dias,int noches){
  var r=s.reservas.crear(cliente,inmueble.getId(),s.hoy().plusDays(dias),s.hoy().plusDays(dias+noches),1);
  return s.pagos.registrar(cliente,r.getId(),MetodoPago.TRANSFERENCIA,"INGRESOS-"+r.getId()).getReserva();
 }
 static void terminar(Reserva r)throws Exception {
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   if(!c.getSchema().matches("renta_http_[a-f0-9]{32}"))throw new IllegalStateException();
   c.setAutoCommit(false);
   try {
    s.execute("ALTER TABLE ocupacion DISABLE TRIGGER tg_ocupacion_inmutable");
    s.execute("ALTER TABLE renta DISABLE TRIGGER ab_asignar_intento");
    s.execute("UPDATE ocupacion SET fecha_inicio=CURRENT_DATE-"+r.getPeriodo().getNoches()+",fecha_fin=CURRENT_DATE WHERE id_ocupacion="+r.getId());
    s.execute("SET CONSTRAINTS ALL IMMEDIATE");
    s.execute("ALTER TABLE ocupacion ENABLE TRIGGER tg_ocupacion_inmutable");
    s.execute("ALTER TABLE renta ENABLE TRIGGER ab_asignar_intento");c.commit();
   }catch(Exception e){c.rollback();throw e;}
  }
 }
 public static void main(String[] args)throws Exception {
  try {
   PrepararVerificacionHttp.main(new String[]{"--crear"});
   String esquema=Files.readString(Path.of(".tools/http-modelo-schema.txt")).trim();
   if(!esquema.matches("renta_http_[a-f0-9]{32}"))throw new IllegalStateException();
   System.setProperty("renta.db.schema",esquema);
   try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
    if(!esquema.equals(c.getSchema()))throw new IllegalStateException();
    s.execute(Files.readString(Path.of("sql/demostracion/datos-demo-profesor.sql")));
   }
   var s=Servicios.crear();long propietario=id("propietario1@renta.test"),otro=id("propietario2@renta.test"),cliente=id("cliente1@renta.test");
   var a=s.inmuebles.publicar(propietario,"Casa ingresos COP","Bogota","Calle 1","Prueba ingresos",2,new BigDecimal("150000"));
   var b=s.inmuebles.publicar(propietario,"Casa pendiente COP","Bogota","Calle 2","Prueba ingresos",2,new BigDecimal("100000"));
   var cero=s.inmuebles.publicar(propietario,"Casa sin rentas","Bogota","Calle 3","Prueba ingresos",2,new BigDecimal("80000"));
   var eur=s.inmuebles.publicar(propietario,"Casa ingresos EUR","Bogota","Calle 4","Prueba ingresos",2,new BigDecimal("100"));
   try(Connection c=ConexionBD.abrir();Statement q=c.createStatement()){q.execute("UPDATE inmuebles SET moneda='EUR' WHERE id_i="+eur.getId());}
   var ajena=s.inmuebles.publicar(otro,"Casa AJENA privada","Bogota","Calle 5","Prueba ingresos",2,new BigDecimal("90000"));
   terminar(pagar(s,cliente,a,60,3));pagar(s,cliente,b,60,2);terminar(pagar(s,cliente,eur,60,2));terminar(pagar(s,cliente,ajena,60,2));
   var cancelada=pagar(s,cliente,b,70,2);s.reservas.cancelar(cliente,cancelada.getId());
   for(int n=0;n<21;n++)s.reservas.crear(cliente,a.getId(),s.hoy().plusDays(100+3*n),s.hoy().plusDays(102+3*n),1);
   new CicloReservas().ejecutar();
   Files.writeString(Path.of(".tools/ingresos-http-schema.txt"),esquema);
   Files.writeString(Path.of(".tools/ingresos-http.json"),"{\"schema\":\""+esquema+"\",\"liquidado\":"+a.getId()+",\"pendiente\":"+b.getId()+",\"sinRentas\":"+cero.getId()+",\"eur\":"+eur.getId()+",\"ajeno\":"+ajena.getId()+"}");
   System.out.println("Fixture de ingresos preparado: "+esquema);
  }finally{ConexionBD.cerrar();}
 }
}
