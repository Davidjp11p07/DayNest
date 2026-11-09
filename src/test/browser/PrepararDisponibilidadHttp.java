import co.edu.rentainmuebles.config.*;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.verification.PrepararVerificacionHttp;
import java.nio.file.*;
import java.sql.*;

/** Crea exclusivamente un esquema UUID con una reserva y un bloqueo futuros. */
class PrepararDisponibilidadHttp {
 public static void main(String[] args) throws Exception {
  try {
   PrepararVerificacionHttp.main(new String[]{"--crear"});
   String esquema=Files.readString(Path.of(".tools/http-modelo-schema.txt")).trim();
   if(!esquema.matches("renta_http_[a-f0-9]{32}"))throw new IllegalStateException();
   System.setProperty("renta.db.schema",esquema);
   long inmueble,propietario,cliente;
   try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
    if(!esquema.equals(c.getSchema()))throw new IllegalStateException();
    s.execute(Files.readString(Path.of("sql/demostracion/datos-demo-profesor.sql")));
    try(ResultSet r=s.executeQuery("SELECT i.id_inmueble,i.id_propietario FROM inmueble_operativo i JOIN usuario u ON u.id_usuario=i.id_propietario WHERE u.correo_normalizado='propietario1@renta.test' ORDER BY i.id_inmueble LIMIT 1")){
     if(!r.next())throw new IllegalStateException();inmueble=r.getLong(1);propietario=r.getLong(2);
    }
    try(ResultSet r=s.executeQuery("SELECT id_usuario FROM usuario WHERE correo_normalizado='cliente1@renta.test'")){r.next();cliente=r.getLong(1);}
   }
   var servicios=Servicios.crear();var hoy=servicios.hoy();
   var reserva=servicios.reservas.crear(cliente,inmueble,hoy.plusDays(10),hoy.plusDays(13),1);
   var bloqueos=new BloqueoService();bloqueos.crear(propietario,inmueble,hoy.plusDays(20),hoy.plusDays(22),"Motivo privado no publicable");
   long bloqueo=bloqueos.listar(propietario).get(0).ocupacion().id();
   Files.writeString(Path.of(".tools/disponibilidad-http-schema.txt"),esquema);
   Files.writeString(Path.of(".tools/disponibilidad-http.json"),"{\"schema\":\""+esquema+"\",\"inmueble\":"+inmueble+",\"reserva\":"+reserva.getId()+",\"bloqueo\":"+bloqueo+",\"inicio\":\""+hoy.plusDays(10)+"\",\"salida\":\""+hoy.plusDays(13)+"\"}");
   System.out.println("Fixture de disponibilidad preparado: "+esquema);
  }finally{ConexionBD.cerrar();}
 }
}
