package co.edu.rentainmuebles.verification;
import co.edu.rentainmuebles.config.ConexionBD;import java.sql.*;import java.nio.file.*;
/** Modelo nuevo aislado; conserva íntegramente public como archivo de las operaciones anteriores. */
public final class MigrarModeloDefinitivo {
 public static void main(String[] args)throws Exception{
  if(args.length!=1||!args[0].equals("--confirmar"))throw new IllegalArgumentException("Use --confirmar para respaldar y preparar el nuevo esquema daynest.");
  Path respaldo=MigrarModelo.respaldar();
  try(Connection c=ConexionBD.abrir()){
   c.setAutoCommit(false);
   try(Statement s=c.createStatement()){
    try(ResultSet r=s.executeQuery("SELECT to_regclass('daynest.ciudad')")){r.next();if(r.getString(1)!=null){System.out.println("El modelo definitivo ya existe; no se modificó. Respaldo: "+respaldo);return;}}
    s.execute("CREATE SCHEMA daynest");s.execute("SET LOCAL search_path TO daynest,public");
    s.execute(sql("sql/instalacion/schema.sql"));
    s.execute("INSERT INTO usuario(id_usuario,nombre_completo,correo_normalizado,contrasena_hash,telefono,activo) OVERRIDING SYSTEM VALUE SELECT id_usuario,nombre_completo,lower(btrim(correo)),clave_hash,telefono,activo FROM public.usuario");
    s.execute("INSERT INTO cliente(id_usuario) SELECT id_usuario FROM public.cliente");s.execute("INSERT INTO propietario(id_usuario) SELECT id_usuario FROM public.propietario");s.execute("INSERT INTO administrador SELECT id_usuario FROM public.usuario WHERE es_administrador");
    s.execute("SELECT setval(pg_get_serial_sequence('usuario','id_usuario'),COALESCE((SELECT max(id_usuario) FROM usuario),1),true)");
    s.execute(sql("sql/instalacion/geografia.sql"));
    // Los inmuebles mantienen su identificador. Ubicación de los registros previos: Colombia.
    s.execute("INSERT INTO inmueble_operativo(id_inmueble,id_propietario,id_ciudad,titulo,descripcion,direccion_detallada,tipo,capacidad,tarifa_noche,moneda,estado) OVERRIDING SYSTEM VALUE SELECT i.id_inmueble,i.id_propietario,c.id_ciudad,i.titulo,i.descripcion,i.direccion,'APARTAMENTO',i.capacidad_maxima,i.tarifa_noche,'COP',CASE i.estado_publicacion WHEN 'ACTIVO' THEN 'PUBLICADO' ELSE 'INACTIVO' END FROM public.inmueble i JOIN ciudad c ON translate(lower(c.nombre),'áéíóúüñ','aeiouun')=translate(lower(i.ciudad),'áéíóúüñ','aeiouun') JOIN division_administrativa d USING(id_division) JOIN pais p ON p.id_pais=d.id_pais WHERE p.codigo_iso2='CO'");
    try(ResultSet r=s.executeQuery("SELECT (SELECT count(*) FROM public.inmueble)=(SELECT count(*) FROM inmueble)")){r.next();if(!r.getBoolean(1))throw new IllegalStateException("Hay inmuebles con ciudad fuera del catálogo; no se migró.");}
    s.execute("SELECT setval(pg_get_serial_sequence('inmuebles','id_i'),COALESCE((SELECT max(id_inmueble) FROM inmueble),1),true)");
    s.execute(sql("sql/instalacion/data.sql"));
    // Reservas previas confirmadas conservan sus noches: bloqueos informativos del propietario.
    s.execute("INSERT INTO ocupacion(id_inmueble,fecha_inicio,fecha_fin,tipo) SELECT r.id_inmueble,r.fecha_entrada,r.fecha_salida,'BLOQUEO' FROM public.reserva r WHERE r.estado='CONFIRMADA' AND r.fecha_salida>CURRENT_DATE");
    s.execute("INSERT INTO bloqueo(id_ocupacion,id_propietario_solicitante,motivo) SELECT o.id_ocupacion,i.id_propietario,'Reserva del historial anterior; consultar public.reserva' FROM ocupacion o JOIN inmueble_operativo i USING(id_inmueble) WHERE o.tipo='BLOQUEO'");
    c.commit();System.out.println("Modelo definitivo en daynest. public permanece intacto como historial legado. Respaldo: "+respaldo);
   }catch(Exception e){c.rollback();throw e;}
  }
 }
 private static String sql(String archivo)throws Exception{return Files.readString(Path.of(archivo)).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$","");}
}
