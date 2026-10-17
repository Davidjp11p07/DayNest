package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.dao.PagoDao;import co.edu.rentainmuebles.model.*;import java.sql.*;import java.util.*;import java.time.*;import java.math.BigDecimal;
/** Adaptador de movimientos de cobro/devolución; transferencias con cuenta historica registrada u ocasional. */
public final class PagoDaoJdbc extends JdbcSoporte<Pago> implements PagoDao {
 private static final String LECTURA="SELECT m.*,to_jsonb(m)->'cuenta_bancaria_pactada'->>'metodo' metodo_pago,to_jsonb(m)->'cuenta_bancaria_pactada'->>'banco' banco_pago,to_jsonb(m)->'cuenta_bancaria_pactada'->>'numero_cuenta' numero_pago,to_jsonb(m)->'cuenta_bancaria_pactada'->>'tipo_cuenta' tipo_pago,to_jsonb(m)->'cuenta_bancaria_pactada'->>'uso' uso_pago FROM movimiento_pago m";
 public PagoDaoJdbc(){super();} PagoDaoJdbc(Connection c){super(c);}
 protected Pago mapear(Connection c,ResultSet r)throws SQLException{return new Pago(r.getLong("id_movimiento"),Mapeador.reserva(c,r.getLong("id_reserva")),r.getBigDecimal("monto"),r.getString("referencia_externa"),r.getObject("creado_en",OffsetDateTime.class).atZoneSameInstant(ZoneId.of("America/Bogota")).toLocalDateTime(),EstadoPago.valueOf(r.getString("estado")),r.getString("clase"),(Long)r.getObject("id_cobro_origen"),r.getString("clave_idempotencia")).conDatosBancarios(r.getString("metodo_pago"),r.getString("banco_pago"),r.getString("numero_pago"),r.getString("tipo_pago"),r.getString("uso_pago"));}
 public Optional<Pago> buscarPorId(long id){return uno(LECTURA+" WHERE id_movimiento=?",s->s.setLong(1,id(id)));}
 public List<Pago> listar(){return consultar(LECTURA+" ORDER BY id_movimiento",s->{});}
 public List<Pago> listarPorReserva(long id){return consultar(LECTURA+" WHERE id_reserva=? ORDER BY id_movimiento",s->s.setLong(1,id(id)));}
 public Optional<Pago> buscarPorReserva(long id){return listarPorReserva(id).stream().filter(p->p.getClase().equals("COBRO")).findFirst();}
 public Pago crear(Pago p){nueva(p.getId());return transaccion(c->{
  if(p.getClase().equals("COBRO")&&p.getMetodo()!=MetodoPago.TRANSFERENCIA)throw new co.edu.rentainmuebles.dao.DaoException("Solo se admite transferencia bancaria simulada.");
  List<Pago> prev=consultar(c,LECTURA+" WHERE clave_idempotencia=?",s->s.setString(1,p.getClaveIdempotencia()));
  if(!prev.isEmpty()){Pago anterior=prev.get(0);if(!anterior.getReserva().getId().equals(p.getReserva().getId())||!anterior.getClase().equals(p.getClase())||anterior.getMonto().compareTo(p.getMonto())!=0||!Objects.equals(anterior.getCobroOrigen(),p.getCobroOrigen())||!anterior.getMoneda().equals(p.getMoneda()))throw new co.edu.rentainmuebles.dao.DaoException("La clave idempotente pertenece a otra operación.");return anterior;}
  long id;
  boolean cuentasNuevas;
  try(Statement t=c.createStatement();ResultSet x=t.executeQuery("SELECT to_regprocedure('cuenta_pago_transferencia(bigint,jsonb)') IS NOT NULL")){x.next();cuentasNuevas=x.getBoolean(1);}
  var cuenta=p.getCuentaOcasional();
  if(cuenta!=null&&!cuentasNuevas)throw new co.edu.rentainmuebles.dao.DaoException("Actualice el esquema para admitir cuentas ocasionales.");
  String seleccion=cuenta==null?"NULL":"jsonb_build_object('id_banco',CAST(? AS bigint),'numero_cuenta',CAST(? AS text),'tipo_cuenta',CAST(? AS text),'metodo','TRANSFERENCIA','uso','OCASIONAL')";
  String sql="INSERT INTO movimiento_pago(id_reserva,clase,monto,id_cobro_origen,clave_idempotencia,referencia_externa"+(cuentasNuevas?",cuenta_bancaria_pactada":"")+") VALUES(?,?,?,?,?,?"+(cuentasNuevas?","+seleccion:"")+") RETURNING id_movimiento";
  try(PreparedStatement s=c.prepareStatement(sql)){
   s.setLong(1,p.getReserva().getId());s.setString(2,p.getClase());s.setBigDecimal(3,p.getMonto());s.setObject(4,p.getCobroOrigen(),Types.BIGINT);s.setString(5,p.getClaveIdempotencia());s.setString(6,p.getReferencia());
   if(cuenta!=null){s.setLong(7,cuenta.bancoId());s.setString(8,cuenta.numeroCuenta());s.setString(9,cuenta.tipoCuenta());}
   try(ResultSet r=s.executeQuery()){r.next();id=r.getLong(1);}
  }
  if(p.getEstado()!=EstadoPago.PENDIENTE)try(PreparedStatement s=c.prepareStatement("UPDATE movimiento_pago SET estado=?,resuelto_en=clock_timestamp() WHERE id_movimiento=?")){s.setString(1,p.getEstado().name());s.setLong(2,id);s.executeUpdate();}
  return new PagoDaoJdbc(c).buscarPorId(id).orElseThrow();
 });}
 public boolean tieneCuentaCliente(long reservaId){return conexion(c->{
  try(Statement t=c.createStatement();ResultSet x=t.executeQuery("SELECT to_regclass('configuracion_comision') IS NOT NULL")){x.next();if(!x.getBoolean(1))return true;}
  try(PreparedStatement t=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM renta r JOIN vincula v USING(id_cl) WHERE r.id_ocupacion=?)")){t.setLong(1,id(reservaId));try(ResultSet x=t.executeQuery()){x.next();return x.getBoolean(1);}}
 });}
 public BigDecimal totalPagado(long id){return conexion(c->{try(PreparedStatement s=c.prepareStatement("SELECT COALESCE(SUM(CASE WHEN clase='COBRO' THEN monto ELSE -monto END),0) FROM movimiento_pago WHERE id_reserva=? AND estado='APROBADO'")){s.setLong(1,id(id));try(ResultSet r=s.executeQuery()){r.next();return r.getBigDecimal(1);}}});}
}
