import co.edu.rentainmuebles.config.*;
import co.edu.rentainmuebles.verification.PrepararVerificacionHttp;
import java.nio.file.*;
import java.sql.*;

/** Crea un fixture UUID nuevo para el pago inmediato; no reserva ni cobra aun. */
class PrepararTransferenciasHttp {
 public static void main(String[] args)throws Exception {
  try {
   PrepararVerificacionHttp.main(new String[]{"--crear"});
   String esquema=Files.readString(Path.of(".tools/http-modelo-schema.txt")).trim();
   if(!esquema.matches("renta_http_[a-f0-9]{32}"))throw new IllegalStateException();
   System.setProperty("renta.db.schema",esquema);
   try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
    if(!esquema.equals(c.getSchema()))throw new IllegalStateException();
    s.execute(Files.readString(Path.of("sql/demostracion/datos-demo-profesor.sql")));
    s.execute("DELETE FROM vincula WHERE id_cl=(SELECT id_cl FROM clientes WHERE emai_cl='cliente2@renta.test')");
    long inmueble,banco;
    try(ResultSet r=s.executeQuery("SELECT min(id_i) FROM inmuebles")){r.next();inmueble=r.getLong(1);}
    try(ResultSet r=s.executeQuery("SELECT min(id_b) FROM banco")){r.next();banco=r.getLong(1);}
    Files.writeString(Path.of(".tools/transferencias-http-schema.txt"),esquema);
    Files.writeString(Path.of(".tools/transferencias-http.json"),"{\"schema\":\""+esquema+"\",\"inmueble\":"+inmueble+",\"banco\":"+banco+"}");
    System.out.println("Fixture de pago inmediato preparado: "+esquema);
   }
  }finally{ConexionBD.cerrar();}
 }
}
