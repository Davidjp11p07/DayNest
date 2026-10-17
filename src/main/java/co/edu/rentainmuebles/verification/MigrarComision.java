package co.edu.rentainmuebles.verification;
import co.edu.rentainmuebles.config.ConexionBD;
import java.sql.*;
import java.nio.file.*;

/** Extension incremental, con respaldo previo y rollback ante errores. */
public final class MigrarComision {
 public static void main(String[] args) throws Exception {
  if(args.length!=1 || !args[0].equals("--aplicar")) throw new IllegalArgumentException("Use --aplicar");
  try(Connection c=ConexionBD.abrir()) {
   if(!"daynest".equals(c.getSchema())) throw new IllegalStateException("Esquema no autorizado");
   Path respaldo=MigrarModelo.respaldar();
   c.setAutoCommit(false);
   try(Statement s=c.createStatement()) {
    s.execute(Files.readString(Path.of("sql/migraciones/migrar-comision.sql")).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$",""));
    c.commit();
    System.out.println("Comision migrada; giros historicos conservados. Respaldo: "+respaldo);
   }catch(Exception ex){c.rollback();throw ex;}
  }finally{ConexionBD.cerrar();}
 }
}
