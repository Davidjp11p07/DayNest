package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.dao.GiroDao;import co.edu.rentainmuebles.model.*;import java.sql.*;import java.util.*;import java.time.*;
public final class GiroDaoJdbc extends JdbcSoporte<Giro> implements GiroDao {
 public GiroDaoJdbc(){super();} GiroDaoJdbc(Connection c){super(c);}
 protected Giro mapear(Connection c,ResultSet r)throws SQLException{Reserva reserva=Mapeador.reserva(c,r.getLong("id_reserva"));return new Giro(r.getLong("id_giro"),reserva,reserva.getPropietarioPactado(),r.getBigDecimal("monto"),r.getString("referencia_externa"),r.getObject("creado_en",OffsetDateTime.class).atZoneSameInstant(ZoneId.of("America/Bogota")).toLocalDateTime(),EstadoGiro.valueOf(r.getString("estado")),(Long)r.getObject("id_admin_autorizador"),r.getString("clave_idempotencia"));}
 public Optional<Giro> buscarPorId(long id){return uno("SELECT * FROM giro WHERE id_giro=?",s->s.setLong(1,id(id)));}
 public List<Giro> listar(){return consultar("SELECT * FROM giro ORDER BY id_giro",s->{});}
 public List<Giro> buscarPorPropietario(long id){return consultar("SELECT g.* FROM giro g JOIN reserva r ON r.id_ocupacion=g.id_reserva WHERE r.id_propietario_pactado=? ORDER BY g.id_giro",s->s.setLong(1,id(id)));}
 public Optional<Giro> buscarPorReserva(long id){return uno("SELECT * FROM giro WHERE id_reserva=? AND estado<>'FALLIDO'",s->s.setLong(1,id(id)));}
 public Giro crear(Giro g){nueva(g.getId());return transaccion(c->{
  List<Giro> prev=consultar(c,"SELECT * FROM giro WHERE clave_idempotencia=?",s->s.setString(1,g.getClaveIdempotencia()));
  if(!prev.isEmpty()){Giro anterior=prev.get(0);if(!anterior.getReserva().getId().equals(g.getReserva().getId())||anterior.getMonto().compareTo(g.getMonto())!=0||!anterior.getMoneda().equals(g.getMoneda()))throw new co.edu.rentainmuebles.dao.DaoException("La clave idempotente pertenece a otro giro.");return anterior;}
  long id;
  try(PreparedStatement s=c.prepareStatement("INSERT INTO giro(id_reserva,monto,clave_idempotencia,referencia_externa) VALUES(?,?,?,?) RETURNING id_giro")){s.setLong(1,g.getReserva().getId());s.setBigDecimal(2,g.getMonto());s.setString(3,g.getClaveIdempotencia());s.setString(4,g.getReferencia());try(ResultSet r=s.executeQuery()){r.next();id=r.getLong(1);}}
  if(g.getEstado()==EstadoGiro.FALLIDO){try(PreparedStatement s=c.prepareStatement("UPDATE giro SET estado='FALLIDO' WHERE id_giro=?")){s.setLong(1,id);s.executeUpdate();}}
  if(g.getEstado()==EstadoGiro.PROCESANDO||g.getEstado()==EstadoGiro.EJECUTADO){try(PreparedStatement s=c.prepareStatement("UPDATE giro SET estado='PROCESANDO' WHERE id_giro=?")){s.setLong(1,id);s.executeUpdate();}}
  if(g.getEstado()==EstadoGiro.EJECUTADO){try(PreparedStatement s=c.prepareStatement("UPDATE giro SET estado='EJECUTADO',id_admin_autorizador=?,ejecutado_en=clock_timestamp() WHERE id_giro=?")){s.setObject(1,g.getAdministradorId(),Types.BIGINT);s.setLong(2,id);s.executeUpdate();}}
  return new GiroDaoJdbc(c).buscarPorId(id).orElseThrow();
 });}
}
