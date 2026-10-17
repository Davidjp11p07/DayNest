package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.dao.InmuebleDao;import co.edu.rentainmuebles.model.*;
import java.sql.*;import java.util.*;import java.math.BigDecimal;
public final class InmuebleDaoJdbc extends JdbcSoporte<Inmueble> implements InmuebleDao {
 public InmuebleDaoJdbc(){super();} InmuebleDaoJdbc(Connection c){super(c);}
 protected Inmueble mapear(Connection c,ResultSet r)throws SQLException{return new Inmueble(r.getLong("id_inmueble"),Mapeador.usuario(c,r.getLong("id_propietario")).getPropietario(),r.getString("titulo"),new CiudadDaoJdbc(c).buscar(r.getLong("id_ciudad")),r.getString("direccion_detallada"),r.getString("descripcion"),r.getInt("capacidad"),r.getBigDecimal("tarifa_noche"),EstadoInmueble.valueOf(r.getString("estado")),r.getString("tipo"),r.getString("moneda"),(Boolean)r.getObject("es_rural"),(Boolean)r.getObject("cerca_mar")).conDetalles(r.getBigDecimal("area_m2"),(Integer)(r.getObject("habitaciones")==null?null:((Number)r.getObject("habitaciones")).intValue()));}
 public Optional<Inmueble> buscarPorId(long id){return uno("SELECT * FROM inmueble_operativo WHERE id_inmueble=?",s->s.setLong(1,id(id)));}
 public Optional<Inmueble> buscarPorIdParaActualizar(long id){return uno("SELECT * FROM inmueble_operativo WHERE id_inmueble=? FOR UPDATE",s->s.setLong(1,id(id)));}
 public List<Inmueble> listar(){return consultar("SELECT * FROM inmueble_operativo ORDER BY id_inmueble",s->{});}
 public List<Inmueble> buscarPorPropietario(long id){return consultar("SELECT * FROM inmueble_operativo WHERE id_propietario=? ORDER BY id_inmueble",s->s.setLong(1,id(id)));}
 public Inmueble crear(Inmueble i){nueva(i.getId());return escribir(i,false);}
 public Inmueble actualizar(Inmueble i){id(i.getId());return escribir(i,true);}
 private Inmueble escribir(Inmueble i,boolean update){return conexion(c->{
  Ciudad ciudad=i.getUbicacion()!=null?i.getUbicacion():new CiudadDaoJdbc(c).resolver(i.getCiudad());
  String sql=update?"UPDATE inmueble_operativo SET id_propietario=?,id_ciudad=?,titulo=?,direccion_detallada=?,descripcion=?,capacidad=?,tarifa_noche=?,estado=?,tipo=?,moneda=?,es_rural=?,cerca_mar=?,area_m2=?,habitaciones=? WHERE id_inmueble=? RETURNING *":"INSERT INTO inmueble_operativo(id_propietario,id_ciudad,titulo,direccion_detallada,descripcion,capacidad,tarifa_noche,estado,tipo,moneda,es_rural,cerca_mar,area_m2,habitaciones) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING *";
  return consultar(c,sql,s->{s.setLong(1,i.getPropietario().getId());s.setLong(2,ciudad.id());s.setString(3,i.getTitulo());s.setString(4,i.getDireccion());s.setString(5,i.getDescripcion());s.setInt(6,i.getCapacidad());s.setBigDecimal(7,i.getPrecioPorNoche());s.setString(8,i.getEstado().name());s.setString(9,i.getTipo());s.setString(10,ciudad.getMoneda());s.setObject(11,i.getEsRural(),Types.BOOLEAN);s.setObject(12,i.getCercaMar(),Types.BOOLEAN);s.setBigDecimal(13,i.getAreaM2());s.setObject(14,i.getHabitaciones(),Types.SMALLINT);if(update)s.setLong(15,i.getId());}).get(0);
 });}
 public void desactivar(long id){modificar("UPDATE inmueble_operativo SET estado='INACTIVO' WHERE id_inmueble=?",s->s.setLong(1,id(id)));}
 public void eliminar(long id){modificar("DELETE FROM inmueble_operativo WHERE id_inmueble=?",s->s.setLong(1,id(id)));}
 public List<Inmueble> buscar(String texto,BigDecimal min,BigDecimal max,Integer cap,PeriodoEstadia p){return buscar(texto,min,max,cap,p,FiltrosInmueble.ninguno());}
 public List<Inmueble> buscar(String texto,BigDecimal min,BigDecimal max,Integer cap,PeriodoEstadia p,FiltrosInmueble f){
  Objects.requireNonNull(f);
  if((min!=null&&min.signum()<0)||(max!=null&&max.signum()<0)||(min!=null&&max!=null&&min.compareTo(max)>0)||(cap!=null&&cap<1))throw new IllegalArgumentException("Filtros inválidos.");
  Long sel=texto!=null&&texto.startsWith("id:")?new CiudadDaoJdbc().resolver(texto).id():null;
  String nombre=texto==null||texto.isBlank()||sel!=null?null:CiudadDaoJdbc.normalizar(texto);
  return consultar("""
   SELECT i.* FROM inmueble_operativo i JOIN ciudad c USING(id_ciudad) JOIN division_administrativa d USING(id_division) WHERE i.estado='PUBLICADO'
   AND (CAST(? AS bigint) IS NULL OR i.id_ciudad=?)
   AND (CAST(? AS varchar) IS NULL OR translate(lower(c.nombre),'áéíóúüñ','aeiouun')=?)
   AND (CAST(? AS numeric) IS NULL OR i.tarifa_noche>=?) AND (CAST(? AS numeric) IS NULL OR i.tarifa_noche<=?)
   AND (CAST(? AS integer) IS NULL OR i.capacidad>=?)
   AND (CAST(? AS date) IS NULL OR NOT EXISTS(SELECT 1 FROM ocupacion o WHERE o.id_inmueble=i.id_inmueble AND o.activa AND o.fecha_inicio<? AND o.fecha_fin>?))
   AND (CAST(? AS bigint) IS NULL OR d.id_pais=?)
   AND (CAST(? AS bigint) IS NULL OR c.id_division=?)
   AND (CAST(? AS varchar) IS NULL OR i.tipo=?)
   AND (CAST(? AS boolean) IS NULL OR i.es_rural=?)
   AND (CAST(? AS boolean) IS NULL OR i.cerca_mar=?)
   ORDER BY i.moneda,i.tarifa_noche,i.id_inmueble
   """,s->{s.setObject(1,sel,Types.BIGINT);s.setObject(2,sel,Types.BIGINT);s.setString(3,nombre);s.setString(4,nombre);s.setBigDecimal(5,min);s.setBigDecimal(6,min);s.setBigDecimal(7,max);s.setBigDecimal(8,max);s.setObject(9,cap,Types.INTEGER);s.setObject(10,cap,Types.INTEGER);s.setObject(11,p==null?null:p.getEntrada(),Types.DATE);s.setObject(12,p==null?null:p.getSalida(),Types.DATE);s.setObject(13,p==null?null:p.getEntrada(),Types.DATE);
    s.setObject(14,f.paisId(),Types.BIGINT);s.setObject(15,f.paisId(),Types.BIGINT);
    s.setObject(16,f.divisionId(),Types.BIGINT);s.setObject(17,f.divisionId(),Types.BIGINT);
    s.setString(18,f.tipo());s.setString(19,f.tipo());
    s.setObject(20,f.rural(),Types.BOOLEAN);s.setObject(21,f.rural(),Types.BOOLEAN);
    s.setObject(22,f.mar(),Types.BOOLEAN);s.setObject(23,f.mar(),Types.BOOLEAN);});
 }
}