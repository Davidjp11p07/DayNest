package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Ensayo de nombres y claves; nunca aplica desde main sobre la base operativa. */
public final class MigrarNomenclatura {
 private MigrarNomenclatura() { }
 private static boolean nueva(Connection c)throws SQLException {
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=current_schema() AND table_name='clientes' AND table_type='BASE TABLE')")){r.next();return r.getBoolean(1);}
 }
 private static Map<String,String> huellas(Connection c,boolean despues)throws SQLException {
  Map<String,String> datos=new TreeMap<>();
  for(String t:List.of("usuario","cliente","propietario","administrador","pais","division_administrativa","ciudad","inmueble","ocupacion","renta","bloqueo","movimiento_pago","giro","banco","asocia","vincula","relaciona")) {
   String origen=despues&&Set.of("renta","banco","asocia","vincula","relaciona").contains(t)?t+"_operativa":t;
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*),md5(coalesce(string_agg(to_jsonb(x)::text,E'\\n' ORDER BY to_jsonb(x)::text),'')) FROM "+origen+" x")){r.next();datos.put(t,r.getLong(1)+":"+r.getString(2));}
  }return datos;
 }
 public static void migrar(Connection c)throws Exception {
  if(!c.getAutoCommit())throw new IllegalArgumentException("Se requiere transaccion propia");
  if(!c.getSchema().equals("daynest")&&!c.getSchema().matches("renta_test_[a-f0-9]{32}"))throw new IllegalArgumentException("Esquema no autorizado");
  if(nueva(c))return;
  c.setAutoCommit(false);
  try {
   try(Statement s=c.createStatement()){s.execute("LOCK TABLE usuario,cliente,propietario,administrador,pais,division_administrativa,ciudad,inmueble,ocupacion,renta,bloqueo,movimiento_pago,giro,banco,asocia,vincula,relaciona IN ACCESS EXCLUSIVE MODE");}
   Map<String,String> antes=huellas(c,false);
   try(Statement s=c.createStatement()){s.execute(Files.readString(Path.of("sql/migraciones/nomenclatura-profesor.sql")));}
   if(!antes.equals(huellas(c,true)))throw new IllegalStateException("Cambiarian filas historicas; migracion revertida");
   c.commit();
  }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
 }
 public static void main(String[] args)throws Exception {
  if(args.length!=1||!args[0].equals("--ensayar"))throw new IllegalArgumentException("Solo --ensayar; aplicar requiere confirmacion separada");
  Properties p=new Properties();try(InputStream in=ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")){p.load(in);}
  URI u=URI.create(p.getProperty("db.url").substring(5));
  String db="daynest_nombres_test_"+UUID.randomUUID().toString().replace("-","");
  try {
   Path respaldo=MigrarModelo.respaldar();
   try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute("CREATE DATABASE "+db);}
   Files.writeString(Path.of(".tools/nomenclatura-base-ensayo.txt"),db);
   ProcessBuilder pb=new ProcessBuilder(Path.of(System.getenv("ProgramFiles"),"PostgreSQL","18","bin","pg_restore.exe").toString(),"-h",u.getHost(),"-p",Integer.toString(u.getPort()<0?5432:u.getPort()),"-U",p.getProperty("db.usuario"),"-d",db,"--exit-on-error","--no-owner","--no-acl",respaldo.toAbsolutePath().toString());
   pb.environment().put("PGPASSWORD",p.getProperty("db.contrasena"));pb.redirectErrorStream(true);pb.redirectOutput(Path.of(".tools/nomenclatura-restauracion.log").toFile());
   if(pb.start().waitFor()!=0)throw new IllegalStateException("Restauracion fallida; base operativa intacta");
   String url="jdbc:postgresql://"+u.getHost()+":"+(u.getPort()<0?5432:u.getPort())+"/"+db;
   try(Connection c=DriverManager.getConnection(url,p.getProperty("db.usuario"),p.getProperty("db.contrasena"));Statement s=c.createStatement()){
    s.execute("SET search_path TO daynest,public");s.execute("SET TIME ZONE 'America/Bogota'");migrar(c);migrar(c);
    try(ResultSet r=s.executeQuery("SELECT count(*) FROM inmuebles")){r.next();System.out.println("Inmuebles conservados: "+r.getLong(1));}
   }
   System.out.println("Ensayo aprobado; filas de las 17 tablas verificadas. Base operativa intacta.");
   System.out.println("Respaldo: "+respaldo+"; copia de ensayo conservada: "+db);
  }finally{ConexionBD.cerrar();}
 }
}
