import co.edu.rentainmuebles.config.*;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.verification.CompletarInterioresDemo;
import java.nio.file.*;
import java.sql.*;
import java.math.BigDecimal;

/** Extiende PrepararIngresosHttp en su esquema UUID; ejecutar después de ingresos.py. */
class PrepararContenidoHttp {
 static long usuario(String correo) throws Exception {
  try(Connection c=ConexionBD.abrir();PreparedStatement s=c.prepareStatement("SELECT id_usuario FROM usuario WHERE correo_normalizado=?")){
   s.setString(1,correo);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalStateException();return r.getLong(1);}
  }
 }
 static void terminar(long reserva) throws Exception {
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   if(!c.getSchema().matches("renta_http_[a-f0-9]{32}"))throw new IllegalStateException();
   c.setAutoCommit(false);
   try{
    s.execute("ALTER TABLE ocupacion DISABLE TRIGGER tg_ocupacion_inmutable");
    s.execute("ALTER TABLE renta DISABLE TRIGGER ab_asignar_intento");
    s.execute("UPDATE ocupacion SET fecha_inicio=CURRENT_DATE-2,fecha_fin=CURRENT_DATE WHERE id_ocupacion="+reserva);
    s.execute("SET CONSTRAINTS ALL IMMEDIATE");
    s.execute("ALTER TABLE ocupacion ENABLE TRIGGER tg_ocupacion_inmutable");
    s.execute("ALTER TABLE renta ENABLE TRIGGER ab_asignar_intento");c.commit();
   }catch(Exception e){c.rollback();throw e;}
  }
 }
 public static void main(String[] args) throws Exception {
  String esquema=Files.readString(Path.of(".tools/ingresos-http-schema.txt")).trim();
  if(!esquema.matches("renta_http_[a-f0-9]{32}"))throw new IllegalArgumentException();
  System.setProperty("renta.db.schema",esquema);
  try{
   long propietario=usuario("propietario1@renta.test"),otro=usuario("propietario2@renta.test"),cliente=usuario("cliente1@renta.test");
   var servicios=Servicios.crear();
   var propio=servicios.inmuebles.publicar(propietario,"Interior de prueba","Bogota","Calle 1","Prueba de fotos y calificaciones",4,new BigDecimal("100000"));
   var ajeno=servicios.inmuebles.publicar(otro,"Interior privado","Bogota","Calle 2","Prueba de permisos",4,new BigDecimal("100000"));
   var pasada=servicios.reservas.crear(cliente,propio.getId(),servicios.hoy().plusDays(30),servicios.hoy().plusDays(32),1);
   var futura=servicios.reservas.crear(cliente,propio.getId(),servicios.hoy().plusDays(40),servicios.hoy().plusDays(42),1);
   var pendiente=servicios.reservas.crear(cliente,ajeno.getId(),servicios.hoy().plusDays(30),servicios.hoy().plusDays(32),1);
   terminar(pasada.getId());terminar(pendiente.getId());
   try(Connection c=ConexionBD.abrir();PreparedStatement s=c.prepareStatement("DELETE FROM relaciona WHERE id_p=?")){s.setLong(1,otro);s.executeUpdate();}
   new CicloReservas().ejecutar();
   try(Connection c=ConexionBD.abrir()){
    try(Statement limpiar=c.createStatement()){limpiar.execute("DELETE FROM vincula WHERE id_cl=(SELECT id_cl FROM clientes WHERE emai_cl='cliente2@renta.test')");}
    System.out.println("Fotos interiores de prueba agregadas: "+CompletarInterioresDemo.completar(c));
    try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT min(id_i),(SELECT min(id_b) FROM banco) FROM inmuebles")){
     r.next();Files.writeString(Path.of(".tools/transferencias-http.json"),"{\"schema\":\""+esquema+"\",\"inmueble\":"+r.getLong(1)+",\"banco\":"+r.getLong(2)+"}");
    }
   }
   Files.writeString(Path.of(".tools/transferencias-http-schema.txt"),esquema);
   Files.writeString(Path.of(".tools/perfiles-http-schema.txt"),esquema);
   Files.writeString(Path.of(".tools/giros-http-reservas.txt"),pasada.getId()+","+futura.getId()+","+pendiente.getId());
   Files.writeString(Path.of(".tools/contenido-http.json"),"{\"schema\":\""+esquema+"\",\"propio\":"+propio.getId()+",\"ajeno\":"+ajeno.getId()+",\"finalizada\":"+pasada.getId()+",\"futura\":"+futura.getId()+"}");
   System.out.println("Contenido y giros preparados en "+esquema);
  }finally{ConexionBD.cerrar();}
 }
}
