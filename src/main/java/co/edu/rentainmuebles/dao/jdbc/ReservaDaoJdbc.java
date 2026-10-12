package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.dao.ReservaDao;import co.edu.rentainmuebles.model.*;
import java.sql.*;import java.util.*;import java.time.*;
public final class ReservaDaoJdbc extends JdbcSoporte<Reserva> implements ReservaDao {
 static final String SELECT="SELECT r.*,o.id_inmueble,o.fecha_inicio,o.fecha_fin FROM reserva r JOIN ocupacion o USING(id_ocupacion)";
 public ReservaDaoJdbc(){super();} ReservaDaoJdbc(Connection c){super(c);}
 protected Reserva mapear(Connection c,ResultSet r)throws SQLException{return new Reserva(r.getLong("id_ocupacion"),Mapeador.usuario(c,r.getLong("id_cliente")).getCliente(),Mapeador.inmueble(c,r.getLong("id_inmueble")),new PeriodoEstadia(r.getObject("fecha_inicio",LocalDate.class),r.getObject("fecha_fin",LocalDate.class)),r.getInt("cantidad_huespedes"),r.getBigDecimal("precio_noche_pactado"),EstadoReserva.valueOf(r.getString("estado")),r.getObject("creada_en",OffsetDateTime.class).atZoneSameInstant(ZoneId.of("America/Bogota")).toLocalDateTime(),Mapeador.usuario(c,r.getLong("id_propietario_pactado")).getPropietario(),r.getString("moneda"),r.getObject("expira_en",OffsetDateTime.class));}
 public Optional<Reserva> buscarPorId(long id){return uno(SELECT+" WHERE r.id_ocupacion=?",s->s.setLong(1,id(id)));}
 public Optional<Reserva> buscarPorIdParaActualizar(long id){return uno(SELECT+" WHERE r.id_ocupacion=? FOR UPDATE OF r",s->s.setLong(1,id(id)));}
 public List<Reserva> listar(){return consultar(SELECT+" ORDER BY r.id_ocupacion",s->{});}
 public List<Reserva> buscarPorPropietario(long id){return consultar(SELECT+" WHERE r.id_propietario_pactado=? ORDER BY r.id_ocupacion",s->s.setLong(1,id(id)));}
 public List<Reserva> buscarPorCliente(long id){return consultar(SELECT+" WHERE r.id_cliente=? ORDER BY r.id_ocupacion",s->s.setLong(1,id(id)));}
 public List<Reserva> buscarPorInmueble(long id){return consultar(SELECT+" WHERE o.id_inmueble=? ORDER BY r.id_ocupacion",s->s.setLong(1,id(id)));}
 public List<Reserva> buscarPorEstado(EstadoReserva e){return consultar(SELECT+" WHERE r.estado=? ORDER BY r.id_ocupacion",s->s.setString(1,e.name()));}
 public Reserva crear(Reserva r){nueva(r.getId());return transaccion(c->{
  long id;
  try(PreparedStatement s=c.prepareStatement("INSERT INTO ocupacion(id_inmueble,fecha_inicio,fecha_fin,tipo) VALUES(?,?,?,'RESERVA') RETURNING id_ocupacion")){s.setLong(1,r.getInmueble().getId());s.setObject(2,r.getPeriodo().getEntrada());s.setObject(3,r.getPeriodo().getSalida());try(ResultSet rs=s.executeQuery()){rs.next();id=rs.getLong(1);}}
  try(PreparedStatement s=c.prepareStatement("INSERT INTO reserva(id_ocupacion,id_cliente,id_propietario_pactado,cantidad_huespedes,precio_noche_pactado,moneda,expira_en) VALUES(?,?,?,?,?,?,?)")){s.setLong(1,id);s.setLong(2,r.getCliente().getId());s.setLong(3,r.getPropietarioPactado().getId());s.setInt(4,r.getHuespedes());s.setBigDecimal(5,r.getPrecioPorNoche());s.setString(6,r.getMoneda());s.setObject(7,r.getExpiraEn());s.executeUpdate();}
  return new ReservaDaoJdbc(c).buscarPorId(id).orElseThrow();
 });}
 public void actualizarEstado(long id,EstadoReserva e){modificar("UPDATE reserva SET estado=? WHERE id_ocupacion=?",s->{s.setString(1,e.name());s.setLong(2,id(id));});}
 public boolean existeCruce(long id,PeriodoEstadia p,Long excluida){return conexion(c->{try(PreparedStatement s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM ocupacion WHERE id_inmueble=? AND activa AND fecha_inicio<? AND fecha_fin>? AND (CAST(? AS bigint) IS NULL OR id_ocupacion<>?))")){s.setLong(1,id(id));s.setObject(2,p.getSalida());s.setObject(3,p.getEntrada());s.setObject(4,excluida,Types.BIGINT);s.setObject(5,excluida,Types.BIGINT);try(ResultSet r=s.executeQuery()){r.next();return r.getBoolean(1);}}});}
}