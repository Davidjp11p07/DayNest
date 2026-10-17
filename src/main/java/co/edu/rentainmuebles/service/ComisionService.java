package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.Rol;
import java.sql.*;
import java.math.*;
import java.util.*;

/** Configuracion versionada y supervision de liquidaciones simuladas. */
public final class ComisionService {
 public record Liquidacion(long reservaId,String moneda,String estado,String motivo,long intentos,BigDecimal total,BigDecimal porcentaje,BigDecimal neto,String destino) {
  public long getReservaId(){return reservaId;} public String getMoneda(){return moneda;} public String getEstado(){return estado;} public String getMotivo(){return motivo;}
  public long getIntentos(){return intentos;} public BigDecimal getTotal(){return total;} public BigDecimal getPorcentaje(){return porcentaje;}
  public BigDecimal getNeto(){return neto;} public String getDestino(){return destino;}
 }
 private static void autorizar(Connection c,long usuario,Rol rol)throws SQLException {
  String tabla=rol==Rol.ADMINISTRADOR?"administrador":"propietario";
  try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM usuario u JOIN "+tabla+" p USING(id_usuario) WHERE u.id_usuario=? AND u.activo")){
   s.setLong(1,usuario);try(ResultSet r=s.executeQuery()){if(!r.next())throw new NegocioException("No tiene permiso para esta operacion.");}
  }
 }
 public boolean disponible(){try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT to_regclass('configuracion_comision') IS NOT NULL")){r.next();return r.getBoolean(1);}catch(SQLException e){throw new NegocioException("No pudo consultarse la configuracion.",e);}}
 public BigDecimal porcentaje(long administrador){
  try(Connection c=ConexionBD.abrir()){autorizar(c,administrador,Rol.ADMINISTRADOR);
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT porcentaje FROM configuracion_comision ORDER BY id_configuracion DESC LIMIT 1")){if(!r.next())throw new NegocioException("Falta configurar la comision.");return r.getBigDecimal(1);}
  }catch(SQLException e){throw new NegocioException("No pudo leerse la comision.",e);}
 }
 public void cambiar(long administrador,BigDecimal porcentaje){
  if(porcentaje==null||porcentaje.signum()<0||porcentaje.compareTo(new BigDecimal("100"))>=0)throw new NegocioException("La comision debe estar entre 0 y menos de 100 %.");
  final BigDecimal fraccion;try{fraccion=porcentaje.movePointLeft(2).setScale(4,RoundingMode.UNNECESSARY);}catch(ArithmeticException e){throw new NegocioException("Use como maximo dos decimales en el porcentaje.");}
  try(Connection c=ConexionBD.abrir()){
   c.setAutoCommit(false);try {
    // Serializa tambien con una desactivacion o cambio de perfil concurrente.
    try(PreparedStatement s=c.prepareStatement("SELECT id_usuario FROM usuario WHERE id_usuario=? FOR UPDATE")){s.setLong(1,administrador);s.executeQuery().close();}
    autorizar(c,administrador,Rol.ADMINISTRADOR);
    try(Statement s=c.createStatement()){s.execute("SELECT pg_advisory_xact_lock(713026,30)");}
    try(PreparedStatement s=c.prepareStatement("INSERT INTO configuracion_comision(porcentaje,id_usuario) VALUES(?,?)")){s.setBigDecimal(1,fraccion);s.setLong(2,administrador);s.executeUpdate();}c.commit();
   }catch(SQLException|RuntimeException e){c.rollback();throw e;}
  }catch(SQLException e){throw new NegocioException("No pudo guardarse la comision.",e);}
 }
 public List<Liquidacion> liquidaciones(long usuario,boolean administrador){
  try(Connection c=ConexionBD.abrir()){
   autorizar(c,usuario,administrador?Rol.ADMINISTRADOR:Rol.PROPIETARIO);
   String sql="SELECT r.id_ocupacion,r.moneda,coalesce(l.estado,CASE WHEN g.estado='EJECUTADO' THEN 'EJECUTADA' ELSE 'PENDIENTE' END) estado,coalesce(l.motivo,CASE WHEN r.estado='CONFIRMADA' THEN 'Se liquidara al finalizar la estancia' ELSE 'Pendiente de revision automatica' END) motivo,coalesce(l.intentos,0) intentos,(r.fch_s-r.fch_e)*r.precio_noche_pactado total,r.porcentaje_comision,concat_ws(' / ',coalesce(g.cuenta_bancaria_pactada->>'banco',b.nom_b),coalesce(g.cuenta_bancaria_pactada->>'numero_cuenta',a.num_cta),coalesce(g.cuenta_bancaria_pactada->>'tipo_cuenta',a.tipo_cta)) destino FROM renta r LEFT JOIN liquidacion_giro l USING(id_ocupacion) LEFT JOIN giro g ON g.id_reserva=r.id_ocupacion AND g.estado='EJECUTADO' LEFT JOIN relaciona a ON a.id_p=r.id_p LEFT JOIN banco b USING(id_b) WHERE r.estado IN ('CONFIRMADA','FINALIZADA')"+(administrador?"":" AND r.id_p=(SELECT id_propietario FROM propietario WHERE id_usuario=?)")+" ORDER BY r.id_ocupacion DESC";
   try(PreparedStatement s=c.prepareStatement(sql)){
    if(!administrador)s.setLong(1,usuario);try(ResultSet r=s.executeQuery()){
     List<Liquidacion> lista=new ArrayList<>();while(r.next()){
      BigDecimal total=r.getBigDecimal("total"),tasa=r.getBigDecimal("porcentaje_comision");
      lista.add(new Liquidacion(r.getLong("id_ocupacion"),r.getString("moneda"),r.getString("estado"),r.getString("motivo"),r.getLong("intentos"),total,tasa,tasa==null?null:total.subtract(total.multiply(tasa).setScale(2,RoundingMode.HALF_UP)),r.getString("destino")));
     }return lista;
    }
   }
  }catch(SQLException e){throw new NegocioException("No pudieron consultarse las liquidaciones.",e);}
 }
 public void reintentar(long administrador,long reserva){
  try(Connection c=ConexionBD.abrir()){
   c.setAutoCommit(false);try {
    autorizar(c,administrador,Rol.ADMINISTRADOR);
    try(PreparedStatement s=c.prepareStatement("SELECT liquidar_reserva_automatica(?)")){s.setLong(1,reserva);s.executeQuery().close();}
    try(PreparedStatement s=c.prepareStatement("UPDATE liquidacion_giro SET historial=historial||jsonb_build_array(jsonb_build_object('evento','REVISION_ADMIN','id_usuario',CAST(? AS bigint),'fecha',clock_timestamp())) WHERE id_ocupacion=?")){s.setLong(1,administrador);s.setLong(2,reserva);s.executeUpdate();}c.commit();
   }catch(SQLException|RuntimeException e){c.rollback();throw e;}
  }catch(SQLException e){throw new NegocioException("No pudo reintentarse la liquidacion.",e);}
 }
}
