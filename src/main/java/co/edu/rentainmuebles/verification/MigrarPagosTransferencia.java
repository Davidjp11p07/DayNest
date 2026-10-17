package co.edu.rentainmuebles.verification;

import co.edu.rentainmuebles.config.ConexionBD;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Actualiza funciones de pago, verificando que ninguna fila historica cambie. */
public final class MigrarPagosTransferencia {
 private MigrarPagosTransferencia() { }
 private static Map<String,String> huellas(Connection c,List<String> tablas)throws SQLException {
  Map<String,String> out=new TreeMap<>();
  for(String tabla:tablas)try(Statement s=c.createStatement();ResultSet r=s.executeQuery(
   "SELECT count(*),md5(coalesce(string_agg(to_jsonb(t)::text,E'\\n' ORDER BY to_jsonb(t)::text),'')) FROM "+tabla+" t")){
   r.next();out.put(tabla,r.getLong(1)+":"+r.getString(2));
  }return out;
 }
 public static void migrar(Connection c)throws Exception {
  if(!c.getAutoCommit())throw new IllegalArgumentException("Se requiere transaccion propia.");
  if(!"daynest".equals(c.getSchema())&&!c.getSchema().matches("renta_(test|http)_[a-f0-9]{32}"))throw new IllegalArgumentException("Esquema no autorizado.");
  c.setAutoCommit(false);
  try {
   List<String> tablas=new ArrayList<>();
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE' ORDER BY table_name")){
    while(r.next()){String tabla=r.getString(1);if(!tabla.matches("[a-z_]+"))throw new IllegalArgumentException("Identificador no permitido");tablas.add(tabla);}
   }
   if(tablas.size()!=19||!tablas.contains("configuracion_comision"))throw new IllegalStateException("Complete primero la migracion financiera.");
   try(Statement s=c.createStatement()){s.execute("LOCK TABLE "+String.join(",",tablas)+" IN ACCESS EXCLUSIVE MODE");}
   var antes=huellas(c,tablas);
   try(Statement s=c.createStatement()){s.execute(Files.readString(Path.of("sql/migraciones/pagos-transferencia.sql")));}
   if(!antes.equals(huellas(c,tablas)))throw new IllegalStateException("Cambiarian datos historicos; se revierte la migracion.");
   c.commit();
  }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
 }
 public static void main(String[] args)throws Exception {
  if(args.length!=1||!"--aplicar".equals(args[0]))throw new IllegalArgumentException("Use --aplicar: crea respaldo y actualiza solo funciones.");
  try {
   System.out.println("Respaldo: "+MigrarModelo.respaldar());
   try(Connection c=ConexionBD.abrir()){migrar(c);migrar(c);}
   System.out.println("Funciones actualizadas; huellas de las 19 tablas conservadas; migracion reejecutable.");
  }finally{ConexionBD.cerrar();}
 }
}
