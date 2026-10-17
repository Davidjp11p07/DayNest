package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.config.ConexionBD;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Consulta financiera: una fila por renta; no registra dinero ni modifica operaciones. */
public final class IngresosService {
 private static final int TAMANO=20;
 private static final String BASE="""
  WITH operaciones AS (
   SELECT r.id_ocupacion id_reserva,r.id_i,i.titulo,r.moneda,r.fch_e,r.fch_s,
    r.estado,r.id_p,pr.nom_p propietario,c.nom_cl cliente,r.porcentaje_comision,
    (r.fch_s-r.fch_e)*r.precio_noche_pactado total,
    coalesce(p.cobrado,0) cobrado,coalesce(p.devuelto,0) devuelto,
    coalesce(p.pendientes,0) pendientes,
    g.id_giro,g.estado estado_giro,g.monto,g.monto_comision,g.ejecutado_en,
    l.motivo
   FROM renta r JOIN inmuebles i ON i.id_i=r.id_i
   JOIN propietarios pr ON pr.id_p=r.id_p JOIN clientes c ON c.id_cl=r.id_cl
   LEFT JOIN LATERAL (
    SELECT sum(monto) FILTER(WHERE clase='COBRO' AND estado='APROBADO') cobrado,
     sum(monto) FILTER(WHERE clase='DEVOLUCION' AND estado='APROBADO') devuelto,
     count(*) FILTER(WHERE estado='PENDIENTE') pendientes
    FROM movimiento_pago p WHERE p.id_reserva=r.id_ocupacion
   ) p ON true
   LEFT JOIN LATERAL (
    SELECT * FROM giro g WHERE g.id_reserva=r.id_ocupacion AND g.estado<>'FALLIDO'
    ORDER BY (g.estado='EJECUTADO') DESC,g.id_giro DESC LIMIT 1
   ) g ON true
   LEFT JOIN liquidacion_giro l ON l.id_ocupacion=r.id_ocupacion
   WHERE (?=0 OR r.id_p=?) AND (?=0 OR r.id_i=?)
  ), cifras AS (
   SELECT *,cobrado-devuelto neto_cobrado,
    CASE WHEN estado_giro='EJECUTADO' THEN monto_comision ELSE 0 END comision_liquidada,
    CASE WHEN estado_giro='EJECUTADO' THEN monto ELSE 0 END neto_liquidado,
    CASE WHEN estado IN ('CONFIRMADA','FINALIZADA') AND estado_giro IS DISTINCT FROM 'EJECUTADO'
      AND cobrado-devuelto=total AND total>0 AND devuelto=0 AND pendientes=0
      AND porcentaje_comision IS NOT NULL
     THEN round(total*porcentaje_comision,2) ELSE 0 END comision_pendiente,
    CASE WHEN estado IN ('CONFIRMADA','FINALIZADA') AND estado_giro IS DISTINCT FROM 'EJECUTADO'
      AND cobrado-devuelto=total AND total>0 AND devuelto=0 AND pendientes=0
      AND porcentaje_comision IS NOT NULL
     THEN total-round(total*porcentaje_comision,2) ELSE 0 END neto_pendiente
   FROM operaciones
  )
  """;
 public record Importe(String moneda,long reservas,BigDecimal comisionLiquidada,BigDecimal comisionPendiente,
                       BigDecimal netoLiquidado,BigDecimal netoPendiente,BigDecimal netoCobrado) {
  public String getMoneda(){return moneda;}public long getReservas(){return reservas;}
  public BigDecimal getComisionLiquidada(){return comisionLiquidada;}public BigDecimal getComisionPendiente(){return comisionPendiente;}
  public BigDecimal getNetoLiquidado(){return netoLiquidado;}public BigDecimal getNetoPendiente(){return netoPendiente;}
  public BigDecimal getNetoCobrado(){return netoCobrado;}
 }
 public record ResumenInmueble(long id,String titulo,Importe importe) {
  public long getId(){return id;}public String getTitulo(){return titulo;}public Importe getImporte(){return importe;}
 }
 public record Detalle(long reservaId,long inmuebleId,String titulo,String moneda,String cliente,String propietario,
                       LocalDate entrada,LocalDate salida,String estado,BigDecimal total,BigDecimal netoCobrado,
                       BigDecimal porcentaje,BigDecimal comisionLiquidada,BigDecimal comisionPendiente,
                       BigDecimal netoLiquidado,BigDecimal netoPendiente,Long giroId,String estadoGiro,
                       LocalDateTime ejecutadoEn,String motivo) {
  public long getReservaId(){return reservaId;}public long getInmuebleId(){return inmuebleId;}public String getTitulo(){return titulo;}
  public String getMoneda(){return moneda;}public String getCliente(){return cliente;}public String getPropietario(){return propietario;}
  public LocalDate getEntrada(){return entrada;}public LocalDate getSalida(){return salida;}public String getEstado(){return estado;}
  public BigDecimal getTotal(){return total;}public BigDecimal getNetoCobrado(){return netoCobrado;}public BigDecimal getPorcentaje(){return porcentaje;}
  public BigDecimal getComisionLiquidada(){return comisionLiquidada;}public BigDecimal getComisionPendiente(){return comisionPendiente;}
  public BigDecimal getNetoLiquidado(){return netoLiquidado;}public BigDecimal getNetoPendiente(){return netoPendiente;}
  public Long getGiroId(){return giroId;}public String getEstadoGiro(){return estadoGiro;}
  public LocalDateTime getEjecutadoEn(){return ejecutadoEn;}public String getMotivo(){return motivo;}
 }
 public record Informe(List<Importe> totales,List<ResumenInmueble> inmuebles,List<Detalle> detalle,long totalRentas,int pagina,int paginas,long filtro) {
  public List<Importe> getTotales(){return totales;}public List<ResumenInmueble> getInmuebles(){return inmuebles;}public List<Detalle> getDetalle(){return detalle;}
  public long getTotalRentas(){return totalRentas;}public int getPagina(){return pagina;}public int getPaginas(){return paginas;}public long getFiltro(){return filtro;}
 }
 private static Importe importe(ResultSet r)throws SQLException {
  return new Importe(r.getString("moneda"),r.getLong("reservas"),r.getBigDecimal("comision_liquidada"),r.getBigDecimal("comision_pendiente"),r.getBigDecimal("neto_liquidado"),r.getBigDecimal("neto_pendiente"),r.getBigDecimal("neto_cobrado"));
 }
 private static Importe sumar(Importe a,Importe b) {
  return new Importe(a.moneda(),a.reservas()+b.reservas(),a.comisionLiquidada().add(b.comisionLiquidada()),a.comisionPendiente().add(b.comisionPendiente()),a.netoLiquidado().add(b.netoLiquidado()),a.netoPendiente().add(b.netoPendiente()),a.netoCobrado().add(b.netoCobrado()));
 }
 private static void parametros(PreparedStatement s,long propietario,long filtro)throws SQLException {
  s.setLong(1,propietario);s.setLong(2,propietario);s.setLong(3,filtro);s.setLong(4,filtro);
 }
 public Informe consultar(long usuario,boolean administracion,long filtro,int paginaSolicitada) {
  if(filtro<0)throw new NegocioException("El inmueble indicado no es valido.");
  try(Connection c=ConexionBD.abrir()) {
   c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
   try {
    long propietario;
    String permiso=administracion?"SELECT 0 FROM usuario u JOIN administrador a USING(id_usuario) WHERE u.id_usuario=? AND u.activo":"SELECT p.id_p FROM usuario u JOIN propietarios p USING(id_usuario) WHERE u.id_usuario=? AND u.activo";
    try(PreparedStatement s=c.prepareStatement(permiso)){
     s.setLong(1,usuario);try(ResultSet r=s.executeQuery()){if(!r.next())throw new NegocioException("No tiene permiso para consultar estos ingresos.");propietario=r.getLong(1);}
    }
    String resumen=BASE+"""
     , catalogo AS (
      SELECT i.id_i,i.titulo,i.moneda FROM inmuebles i JOIN asocia a USING(id_i)
      WHERE (?=0 OR a.id_p=?) AND (?=0 OR i.id_i=?)
      UNION SELECT id_i,titulo,moneda FROM cifras
     )
     SELECT k.id_i,k.titulo,k.moneda,count(m.id_reserva) reservas,
      coalesce(sum(m.comision_liquidada),0) comision_liquidada,
      coalesce(sum(m.comision_pendiente),0) comision_pendiente,
      coalesce(sum(m.neto_liquidado),0) neto_liquidado,
      coalesce(sum(m.neto_pendiente),0) neto_pendiente,
      coalesce(sum(m.neto_cobrado),0) neto_cobrado
     FROM catalogo k LEFT JOIN cifras m ON m.id_i=k.id_i AND m.moneda=k.moneda
     GROUP BY k.id_i,k.titulo,k.moneda ORDER BY k.id_i,k.moneda
     """;
    List<ResumenInmueble> inmuebles=new ArrayList<>();Map<String,Importe> totales=new TreeMap<>();long cantidad=0;
    try(PreparedStatement s=c.prepareStatement(resumen)) {
     parametros(s,propietario,filtro);s.setLong(5,propietario);s.setLong(6,propietario);s.setLong(7,filtro);s.setLong(8,filtro);
     try(ResultSet r=s.executeQuery()){while(r.next()){
      Importe importe=importe(r);inmuebles.add(new ResumenInmueble(r.getLong("id_i"),r.getString("titulo"),importe));
      totales.merge(importe.moneda(),importe,IngresosService::sumar);cantidad+=importe.reservas();
     }}
    }
    int paginas=(int)Math.min(Integer.MAX_VALUE,Math.max(1,(cantidad+TAMANO-1)/TAMANO));
    int pagina=Math.max(1,Math.min(paginaSolicitada,paginas));List<Detalle> detalle=new ArrayList<>();
    try(PreparedStatement s=c.prepareStatement(BASE+"SELECT *,ejecutado_en AT TIME ZONE current_setting('TimeZone') fecha_ejecucion FROM cifras ORDER BY id_reserva DESC LIMIT ? OFFSET ?")) {
     parametros(s,propietario,filtro);s.setInt(5,TAMANO);s.setLong(6,(long)(pagina-1)*TAMANO);
     try(ResultSet r=s.executeQuery()){while(r.next())detalle.add(new Detalle(
      r.getLong("id_reserva"),r.getLong("id_i"),r.getString("titulo"),r.getString("moneda"),r.getString("cliente"),r.getString("propietario"),
      r.getObject("fch_e",LocalDate.class),r.getObject("fch_s",LocalDate.class),r.getString("estado"),r.getBigDecimal("total"),r.getBigDecimal("neto_cobrado"),
      r.getBigDecimal("porcentaje_comision"),r.getBigDecimal("comision_liquidada"),r.getBigDecimal("comision_pendiente"),r.getBigDecimal("neto_liquidado"),r.getBigDecimal("neto_pendiente"),
      (Long)r.getObject("id_giro"),r.getString("estado_giro"),r.getObject("fecha_ejecucion",LocalDateTime.class),r.getString("motivo")));
     }
    }
    c.commit();return new Informe(List.copyOf(totales.values()),List.copyOf(inmuebles),List.copyOf(detalle),cantidad,pagina,paginas,filtro);
   }catch(SQLException|RuntimeException e){c.rollback();throw e;}
  }catch(SQLException e){throw new NegocioException("No pudieron consultarse los ingresos.",e);}
 }
}
