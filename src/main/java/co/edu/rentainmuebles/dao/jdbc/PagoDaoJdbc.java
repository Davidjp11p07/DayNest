package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.dao.PagoDao;import co.edu.rentainmuebles.model.*;import java.sql.*;import java.util.*;import java.time.*;import java.math.BigDecimal;
/** Adaptador de movimientos de cobro/devolución; el método elegido no es persistido por este modelo. */
public final class PagoDaoJdbc extends JdbcSoporte<Pago> implements PagoDao {
 public PagoDaoJdbc(){super();} PagoDaoJdbc(Connection c){super(c);}
 protected Pago mapear(Connection c,ResultSet r)throws SQLException{return new Pago(r.getLong("id_movimiento"),Mapeador.reserva(c,r.getLong("id_reserva")),r.getBigDecimal("monto"),r.getString("referencia_externa"),r.getObject("creado_en",OffsetDateTime.class).atZoneSameInstant(ZoneId.of("America/Bogota")).toLocalDateTime(),EstadoPago.valueOf(r.getString("estado")),r.getString("clase"),(Long)r.getObject("id_cobro_origen"),r.getString("clave_idempotencia"));}
 public Optional<Pago> buscarPorId(long id){return uno("SELECT * FROM movimiento_pago WHERE id_movimiento=?",s->s.setLong(1,id(id)));}
 public List<Pago> listar(){return consultar("SELECT * FROM movimiento_pago ORDER BY id_movimiento",s->{});}
 public List<Pago> listarPorReserva(long id){return consultar("SELECT * FROM movimiento_pago WHERE id_reserva=? ORDER BY id_movimiento",s->s.setLong(1,id(id)));}
 public Optional<Pago> buscarPorReserva(long id){return listarPorReserva(id).stream().filter(p->p.getClase().equals("COBRO")).findFirst();}
 public Pago crear(Pago p){nueva(p.getId());return transaccion(c->{
  List<Pago> prev=consultar(c,"SELECT * FROM movimiento_pago WHERE clave_idempotencia=?",s->s.setString(1,p.getClaveIdempotencia()));
  if(!prev.isEmpty()){Pago anterior=prev.get(0);if(!anterior.getReserva().getId().equals(p.getReserva().getId())||!anterior.getClase().equals(p.getClase())||anterior.getMonto().compareTo(p.getMonto())!=0||!Objects.equals(anterior.getCobroOrigen(),p.getCobroOrigen())||!anterior.getMoneda().equals(p.getMoneda()))throw new co.edu.rentainmuebles.dao.DaoException("La clave idempotente pertenece a otra operación.");return anterior;}
  long id;
  try(PreparedStatement s=c.prepareStatement("INSERT INTO movimiento_pago(id_reserva,clase,monto,id_cobro_origen,clave_idempotencia,referencia_externa) VALUES(?,?,?,?,?,?) RETURNING id_movimiento")){s.setLong(1,p.getReserva().getId());s.setString(2,p.getClase());s.setBigDecimal(3,p.getMonto());s.setObject(4,p.getCobroOrigen(),Types.BIGINT);s.setString(5,p.getClaveIdempotencia());s.setString(6,p.getReferencia());try(ResultSet r=s.executeQuery()){r.next();id=r.getLong(1);}}
  if(p.getEstado()!=EstadoPago.PENDIENTE)try(PreparedStatement s=c.prepareStatement("UPDATE movimiento_pago SET estado=?,resuelto_en=clock_timestamp() WHERE id_movimiento=?")){s.setString(1,p.getEstado().name());s.setLong(2,id);s.executeUpdate();}
  return new PagoDaoJdbc(c).buscarPorId(id).orElseThrow();
 });}
 public BigDecimal totalPagado(long id){return conexion(c->{try(PreparedStatement s=c.prepareStatement("SELECT COALESCE(SUM(CASE WHEN clase='COBRO' THEN monto ELSE -monto END),0) FROM movimiento_pago WHERE id_reserva=? AND estado='APROBADO'")){s.setLong(1,id(id));try(ResultSet r=s.executeQuery()){r.next();return r.getBigDecimal(1);}}});}
}
