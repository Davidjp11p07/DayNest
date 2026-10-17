package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.sql.*;
import java.nio.file.*;
import java.io.*;
import java.net.URI;
import java.util.*;

/** Migra sin reiniciar datos. Antes de aplicar restaura y ensaya un respaldo completo. */
public final class MigrarProfesor {
 private static final List<String> TABLAS=List.of("usuario","cliente","propietario","administrador","pais","division_administrativa","ciudad","inmueble","ocupacion","reserva","bloqueo","movimiento_pago","giro");
 private static String q(String x){if(!x.matches("[a-z][a-z0-9_]{0,62}"))throw new IllegalArgumentException("Identificador invalido");return "\""+x+"\"";}
 private static boolean existe(Connection c,String tabla)throws SQLException {
  try(PreparedStatement s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=current_schema() AND table_name=? AND table_type='BASE TABLE')")){s.setString(1,tabla);try(ResultSet r=s.executeQuery()){r.next();return r.getBoolean(1);}}
 }
 private static Map<String,List<String>> columnas(Connection c)throws SQLException {
  Map<String,List<String>> out=new LinkedHashMap<>();
  try(PreparedStatement s=c.prepareStatement("SELECT table_name,column_name FROM information_schema.columns WHERE table_schema=current_schema() ORDER BY table_name,ordinal_position");ResultSet r=s.executeQuery()){
   while(r.next())if(TABLAS.contains(r.getString(1)))out.computeIfAbsent(r.getString(1),k->new ArrayList<>()).add(r.getString(2));
  }return out;
 }
 private static Map<String,String> huellas(Connection c,Map<String,List<String>> cols,boolean nuevo)throws SQLException {
  Map<String,String> out=new TreeMap<>();
  for(var t:cols.entrySet()){
   String origen=nuevo&&t.getKey().equals("inmueble")?"inmueble_operativo":t.getKey();
   String select=String.join(",",t.getValue().stream().map(MigrarProfesor::q).toList());
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*),md5(coalesce(string_agg(to_jsonb(x)::text,E'\\n' ORDER BY to_jsonb(x)::text),'')) FROM (SELECT "+select+" FROM "+q(origen)+") x")){r.next();out.put(t.getKey(),r.getLong(1)+":"+r.getString(2));}
  }return out;
 }
 public static void migrar(Connection c)throws Exception {
  if(!c.getAutoCommit())throw new IllegalStateException("Se requiere una conexion sin transaccion abierta");
  String ns=c.getSchema();if(!"daynest".equals(ns)&&!ns.matches("renta_test_[a-f0-9]{32}"))throw new IllegalStateException("Esquema no autorizado");
  if(existe(c,"clientes")) return; // Nomenclatura posterior: no ejecutar de nuevo la migracion historica.
  if(existe(c,"asocia")){
   for(String t:List.of("cliente","propietario","banco","inmueble","ciudad","pais","asocia","vincula","relaciona","renta"))if(!existe(c,t))throw new IllegalStateException("Migracion parcial; requiere inspeccion");
   return;
  }
  c.setAutoCommit(false);
  try {
   Map<String,List<String>> cols=columnas(c);Map<String,String> antes=huellas(c,cols,false);
   try(Statement s=c.createStatement()){s.execute(Files.readString(Path.of("sql/migraciones/modelo-profesor.sql")));}
   if(!antes.equals(huellas(c,cols,true)))throw new IllegalStateException("Cambiarian datos historicos; se revierte la migracion");
   c.commit();
  }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
 }
 private static String bin(String exe){return Path.of(System.getenv("ProgramFiles"),"PostgreSQL","18","bin",exe+".exe").toString();}
 private static void ensayar(Path respaldo)throws Exception {
  Properties p=new Properties();try(InputStream in=ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")){p.load(in);}
  URI uri=URI.create(p.getProperty("db.url").substring(5));String db="daynest_profesor_test_"+UUID.randomUUID().toString().replace("-","");
  String url="jdbc:postgresql://"+uri.getHost()+":"+(uri.getPort()<0?5432:uri.getPort())+"/"+db;
  boolean creada=false;
  try {
   try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute("CREATE DATABASE "+q(db));creada=true;}
   ProcessBuilder pb=new ProcessBuilder(bin("pg_restore"),"-h",uri.getHost(),"-p",Integer.toString(uri.getPort()<0?5432:uri.getPort()),"-U",p.getProperty("db.usuario"),"-d",db,"--exit-on-error","--no-owner","--no-acl",respaldo.toAbsolutePath().toString());
   pb.environment().put("PGPASSWORD",p.getProperty("db.contrasena"));pb.redirectErrorStream(true);pb.redirectOutput(Path.of(".tools","profesor-restauracion.log").toFile());
   if(pb.start().waitFor()!=0)throw new IllegalStateException("No pudo restaurarse el respaldo; no se aplico la migracion");
   try(Connection c=DriverManager.getConnection(url,p.getProperty("db.usuario"),p.getProperty("db.contrasena"));Statement s=c.createStatement()){
    s.execute("SET search_path TO daynest,public");migrar(c);migrar(c);
    try(ResultSet r=s.executeQuery("SELECT count(*) FROM inmueble_operativo")){r.next();System.out.println("Ensayo: inmuebles conservados="+r.getLong(1));}
   }
  }finally{
   if(creada){try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute("DROP DATABASE "+q(db)+" WITH (FORCE)");}}
  }
 }
 public static void main(String[] args)throws Exception {
  if(args.length!=1||!Set.of("--ensayar","--aplicar").contains(args[0]))throw new IllegalArgumentException("Use --ensayar o --aplicar");
  try {
   Path respaldo=MigrarModelo.respaldar();
   ensayar(respaldo);
   if(args[0].equals("--aplicar")){
    try(java.net.Socket s=new java.net.Socket()){s.connect(new java.net.InetSocketAddress("127.0.0.1",8080),500);throw new IllegalStateException("Detenga Jetty antes de aplicar");}catch(java.net.ConnectException e){}
    try(Connection c=ConexionBD.abrir()){migrar(c);}
    System.out.println("Modelo del profesor aplicado; respaldo: "+respaldo);
   }else System.out.println("Ensayo aprobado; base operativa intacta. Respaldo: "+respaldo);
  }finally{ConexionBD.cerrar();}
 }
}
