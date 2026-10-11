package co.edu.rentainmuebles.verification;
import co.edu.rentainmuebles.config.ConexionBD;import java.sql.*;import java.nio.file.*;
/** Inicializa únicamente un esquema nuevo; nunca reemplaza datos. */
public final class InicializarBD {
 public static void main(String[] args)throws Exception{try(Connection c=ConexionBD.abrir()){
  String esquema=System.getProperty("renta.db.schema","daynest");if(!esquema.matches("[a-z][a-z0-9_]{0,62}"))throw new IllegalArgumentException("Esquema inválido");
  c.setAutoCommit(false);try(Statement s=c.createStatement()){
   try(ResultSet r=s.executeQuery("SELECT to_regclass('"+esquema+".usuario')")){r.next();if(r.getString(1)!=null)throw new IllegalStateException("El esquema ya existe. Use MigrarModeloDefinitivo para el modelo anterior; no se modificaron datos.");}
   s.execute("CREATE SCHEMA IF NOT EXISTS \""+esquema+"\"");s.execute("SET LOCAL search_path TO \""+esquema+"\",public");
   for(String f:new String[]{"schema.sql","data.sql"})s.execute(Files.readString(Path.of(f)).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$",""));c.commit();
  }catch(Exception e){c.rollback();throw e;}
 }}
}