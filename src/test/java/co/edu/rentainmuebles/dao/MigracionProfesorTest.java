package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.verification.MigrarProfesor;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MigracionProfesorTest extends SoportePostgres {
 private Set<String> estructura(Connection c)throws Exception {
  Set<String> out=new TreeSet<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT table_name,column_name,data_type,is_nullable FROM information_schema.columns WHERE table_schema=current_schema()")){while(r.next())out.add(r.getString(1)+"|"+r.getString(2)+"|"+r.getString(3)+"|"+r.getString(4));}return out;
 }
 @Test void migracionConservaDatosYEquivaleANuevaInstalacion()throws Exception {
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   String original=c.getSchema(),temporal="renta_test_"+UUID.randomUUID().toString().replace("-","");Set<String> esperado=estructura(c);
   try {
    s.execute("CREATE SCHEMA "+temporal);s.execute("SET search_path TO "+temporal+",public");
    s.execute(Files.readString(Path.of("src/test/resources/schema-profesor-origen.sql")));
    // Las instalaciones migradas tienen las columnas de filtros al final.
    s.execute("ALTER TABLE inmueble DROP COLUMN es_rural,DROP COLUMN cerca_mar");
    s.execute("ALTER TABLE inmueble ADD COLUMN es_rural BOOLEAN,ADD COLUMN cerca_mar BOOLEAN");
    s.execute(Files.readString(Path.of("src/test/resources/geografia-antes-nomenclatura.sql")));
    try(PreparedStatement q=c.prepareStatement("INSERT INTO usuario(nombre_completo,correo_normalizado,contrasena_hash) VALUES('Persona historica','historia@test.com',?)")){q.setString(1,cliente.getHashContrasena());q.executeUpdate();}
    s.execute("INSERT INTO cliente SELECT id_usuario FROM usuario");s.execute("INSERT INTO propietario SELECT id_usuario FROM usuario");
    s.execute("INSERT INTO inmueble(id_propietario,id_ciudad,titulo,direccion_detallada,capacidad,tarifa_noche,moneda,estado) SELECT 1,min(id_ciudad),'Historico','Calle 1',4,150000,'COP','PUBLICADO' FROM ciudad");
    c.setAutoCommit(false);
    s.execute("INSERT INTO ocupacion(id_inmueble,fecha_inicio,fecha_fin,tipo) VALUES(1,CURRENT_DATE+10,CURRENT_DATE+12,'RESERVA')");
    s.execute("INSERT INTO reserva(id_ocupacion,id_cliente,id_propietario_pactado,cantidad_huespedes,precio_noche_pactado,moneda,expira_en) VALUES(1,1,1,2,150000,'COP',clock_timestamp()+interval '30 minutes')");
    c.commit();c.setAutoCommit(true);
    MigrarProfesor.migrar(c);MigrarProfesor.migrar(c);
    c.setAutoCommit(false);s.execute(Files.readString(Path.of("sql/migraciones/nomenclatura-profesor.sql")));s.execute(Files.readString(Path.of("sql/migraciones/giros-automaticos.sql")));c.commit();c.setAutoCommit(true);
    assertEquals(esperado,estructura(c));
    try(ResultSet r=s.executeQuery("SELECT id_cliente,id_inmueble,fecha_salida-fecha_entrada FROM renta_operativa WHERE id_ocupacion=1")){assertTrue(r.next());assertEquals(1,r.getLong(1));assertEquals(1,r.getLong(2));assertEquals(2,r.getInt(3));}
    try(ResultSet r=s.executeQuery("SELECT nombre_completo FROM cliente WHERE id_cliente=1")){r.next();assertEquals("Persona historica",r.getString(1));}
    try(ResultSet r=s.executeQuery("SELECT id_propietario,precio_noche FROM asocia_operativa WHERE id_inmueble=1")){r.next();assertEquals(1,r.getLong(1));assertEquals("150000.00",r.getBigDecimal(2).toPlainString());}
   }finally{s.execute("SET search_path TO "+original+",public");s.execute("DROP SCHEMA IF EXISTS "+temporal+" CASCADE");}
  }
 }
}
