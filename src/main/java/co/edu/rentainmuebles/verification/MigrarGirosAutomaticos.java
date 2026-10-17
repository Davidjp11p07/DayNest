package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.sql.*;import java.nio.file.*;import java.util.*;import java.io.*;import java.net.URI;

/** Prepara y ensaya la extension financiera sin aplicarla desde main a datos operativos. */
public final class MigrarGirosAutomaticos {
 private MigrarGirosAutomaticos() { }
 private static Map<String,List<String>> columnas(Connection c)throws SQLException {
  Map<String,List<String>> out=new TreeMap<>();
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT t.table_name,c.column_name FROM information_schema.tables t JOIN information_schema.columns c USING(table_schema,table_name) WHERE t.table_schema=current_schema() AND t.table_type='BASE TABLE' ORDER BY t.table_name,c.ordinal_position")){
   while(r.next())out.computeIfAbsent(r.getString(1),k->new ArrayList<>()).add(r.getString(2));
  }return out;
 }
 private static Map<String,String> huellas(Connection c,Map<String,List<String>> columnas)throws SQLException {
  Map<String,String> out=new TreeMap<>();
  for(var tabla:columnas.entrySet()){
   String campos=String.join(",",tabla.getValue());
   if(!(tabla.getKey()+campos).matches("[a-z0-9_,]+"))throw new IllegalArgumentException("Identificador invalido");
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*),md5(coalesce(string_agg(to_jsonb(x)::text,E'\\n' ORDER BY to_jsonb(x)::text),'')) FROM (SELECT "+campos+" FROM "+tabla.getKey()+") x")){r.next();out.put(tabla.getKey(),r.getLong(1)+":"+r.getString(2));}
  }return out;
 }
 public static void migrar(Connection c)throws Exception {
  if(!c.getAutoCommit())throw new IllegalArgumentException("Se requiere transaccion propia");
  if(!c.getSchema().equals("daynest")&&!c.getSchema().matches("renta_test_[a-f0-9]{32}"))throw new IllegalArgumentException("Esquema no autorizado");
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT to_regclass('configuracion_comision') IS NOT NULL")){r.next();if(r.getBoolean(1))return;}
  c.setAutoCommit(false);
  try {
   Map<String,List<String>> cols=columnas(c);
   if(cols.size()!=17||!cols.containsKey("clientes"))throw new IllegalStateException("Termine primero la nomenclatura del profesor");
   try(Statement s=c.createStatement()){s.execute("LOCK TABLE "+String.join(",",cols.keySet())+" IN ACCESS EXCLUSIVE MODE");}
   Map<String,String> antes=huellas(c,cols);
   try(Statement s=c.createStatement()){s.execute(Files.readString(Path.of("giros-automaticos.sql")));}
   if(!antes.equals(huellas(c,cols)))throw new IllegalStateException("Cambiarian valores historicos; se revierte la extension");
   c.commit();
  }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
 }
 public static void main(String[] args)throws Exception {
  if(args.length!=1||!args[0].equals("--ensayar"))throw new IllegalArgumentException("Solo --ensayar; aplicar requiere confirmacion separada");
  Properties p=new Properties();try(InputStream in=ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")){p.load(in);}
  URI u=URI.create(p.getProperty("db.url").substring(5));String db="daynest_giros_test_"+UUID.randomUUID().toString().replace("-","");
  try {
   Path backup=MigrarModelo.respaldar();
   try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute("CREATE DATABASE "+db);}
   Files.writeString(Path.of(".tools/giros-base-ensayo.txt"),db);
   ProcessBuilder pb=new ProcessBuilder(Path.of(System.getenv("ProgramFiles"),"PostgreSQL","18","bin","pg_restore.exe").toString(),"-h",u.getHost(),"-p",Integer.toString(u.getPort()<0?5432:u.getPort()),"-U",p.getProperty("db.usuario"),"-d",db,"--exit-on-error","--no-owner","--no-acl",backup.toAbsolutePath().toString());
   pb.environment().put("PGPASSWORD",p.getProperty("db.contrasena"));pb.redirectErrorStream(true);pb.redirectOutput(Path.of(".tools/giros-restauracion.log").toFile());
   if(pb.start().waitFor()!=0)throw new IllegalStateException("Restauracion fallida; base operativa intacta");
   String url="jdbc:postgresql://"+u.getHost()+":"+(u.getPort()<0?5432:u.getPort())+"/"+db;
   try(Connection c=DriverManager.getConnection(url,p.getProperty("db.usuario"),p.getProperty("db.contrasena"));Statement s=c.createStatement()){
    s.execute("SET search_path TO daynest,public");s.execute("SET TIME ZONE 'America/Bogota'");migrar(c);migrar(c);
    try(ResultSet r=s.executeQuery("SELECT count(*) FROM inmuebles")){r.next();System.out.println("Inmuebles conservados: "+r.getLong(1));}
    try(ResultSet r=s.executeQuery("SELECT count(*) FROM renta r WHERE EXISTS(SELECT 1 FROM movimiento_pago m WHERE m.id_reserva=r.id_ocupacion AND m.clase='COBRO' AND m.estado='APROBADO') AND r.porcentaje_comision IS NULL")){r.next();if(r.getLong(1)!=0)throw new IllegalStateException("Faltan tasas historicas");}
   }
   System.out.println("Ensayo aprobado; historial original de las 17 tablas conservado; extension idempotente.");
   System.out.println("Respaldo: "+backup+"; copia conservada: "+db);
  }finally{ConexionBD.cerrar();}
 }
}
