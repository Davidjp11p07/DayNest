package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import java.sql.*;
import java.util.*;

/** Una cuenta bancaria por entidad. La copia entre perfiles es explicita y atomica. */
public final class DatosCuentaService {
 @FunctionalInterface private interface Operacion<T>{T ejecutar(Connection c)throws SQLException;}
 private <T>T transaccion(long usuario,Operacion<T> op){
  try(Connection c=ConexionBD.abrir()) {
   c.setAutoCommit(false);
   try {
    try(PreparedStatement s=c.prepareStatement("SELECT activo FROM usuario WHERE id_usuario=? FOR UPDATE")){s.setLong(1,usuario);try(ResultSet r=s.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new NegocioException("La cuenta debe estar activa.");}}
    T resultado=op.ejecutar(c);c.commit();return resultado;
   }catch(SQLException|RuntimeException e){c.rollback();throw e;}
  }catch(SQLException e){throw new NegocioException("No se guardaron los datos; revise el banco, la ciudad y los valores indicados.",e);}
 }
 private static String tabla(Rol perfil){if(perfil==Rol.CLIENTE)return "cliente";if(perfil==Rol.PROPIETARIO)return "propietario";throw new NegocioException("Seleccione cliente o propietario.");}
 private static void exigePerfil(Connection c,long usuario,Rol perfil)throws SQLException {
  try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM "+tabla(perfil)+" WHERE id_usuario=?")){s.setLong(1,usuario);try(ResultSet r=s.executeQuery()){if(!r.next())throw new NegocioException("Debe activar el perfil indicado antes de guardar sus datos.");}}
 }
 public List<Banco> bancos(){
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT b.*,p.nombre pais FROM banco_operativa b JOIN pais p USING(id_pais) ORDER BY p.nombre,b.nombre")){
   List<Banco> lista=new ArrayList<>();while(r.next())lista.add(new Banco(r.getLong("id_banco"),r.getString("nombre"),r.getLong("id_pais"),r.getString("pais")));return lista;
  }catch(SQLException e){throw new NegocioException("No pudo cargarse el catalogo de bancos.",e);}
 }
 public DatosPerfil consultar(long usuario,Rol perfil){return transaccion(usuario,c->{
  exigePerfil(c,usuario,perfil);String relacion=perfil==Rol.CLIENTE?"vincula_operativa":"relaciona_operativa",id=perfil==Rol.CLIENTE?"id_cliente":"id_propietario";
  String sql="SELECT e.*,"+(perfil==Rol.CLIENTE?"e.id_ciudad":"NULL::bigint")+" residencia,x.id_banco,x.numero_cuenta,x.tipo_cuenta FROM "+tabla(perfil)+" e LEFT JOIN "+relacion+" x ON x."+id+"=e."+id+" WHERE e.id_usuario=?";
  try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,usuario);try(ResultSet r=s.executeQuery()){r.next();Long banco=(Long)r.getObject("id_banco");return new DatosPerfil(r.getLong(id),perfil,r.getString("nombre_completo"),r.getString("correo_normalizado"),r.getString("telefono"),(Long)r.getObject("residencia"),banco==null?null:new CuentaBancaria(banco,r.getString("numero_cuenta"),r.getString("tipo_cuenta")));}}
 });}
 public void guardar(long usuario,Rol perfil,CuentaBancaria cuenta,boolean ambos){
  Objects.requireNonNull(cuenta);transaccion(usuario,c->{
   exigePerfil(c,usuario,perfil);
   if(ambos){exigePerfil(c,usuario,Rol.CLIENTE);exigePerfil(c,usuario,Rol.PROPIETARIO);}
   try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM banco_operativa WHERE id_banco=? FOR KEY SHARE")){s.setLong(1,cuenta.bancoId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new NegocioException("El banco seleccionado no existe.");}}
   guardarEn(c,usuario,perfil,cuenta);
   if(ambos)guardarEn(c,usuario,perfil==Rol.CLIENTE?Rol.PROPIETARIO:Rol.CLIENTE,cuenta);
   return null;
  });
 }
 private static void guardarEn(Connection c,long usuario,Rol perfil,CuentaBancaria cuenta)throws SQLException {
  String relacion=perfil==Rol.CLIENTE?"vincula_operativa":"relaciona_operativa",id=perfil==Rol.CLIENTE?"id_cliente":"id_propietario";
  try(PreparedStatement s=c.prepareStatement("INSERT INTO "+relacion+"("+id+",id_banco,numero_cuenta,tipo_cuenta) SELECT "+id+",?,?,? FROM "+tabla(perfil)+" WHERE id_usuario=? ON CONFLICT("+id+") DO UPDATE SET id_banco=EXCLUDED.id_banco,numero_cuenta=EXCLUDED.numero_cuenta,tipo_cuenta=EXCLUDED.tipo_cuenta,actualizada_en=clock_timestamp()")){
   s.setLong(1,cuenta.bancoId());s.setString(2,cuenta.numeroCuenta());s.setString(3,cuenta.tipoCuenta());s.setLong(4,usuario);s.executeUpdate();
  }
 }
 public void residencia(long usuario,long ciudad){transaccion(usuario,c->{exigePerfil(c,usuario,Rol.CLIENTE);try(PreparedStatement s=c.prepareStatement("UPDATE cliente SET id_ciudad=? WHERE id_usuario=?")){s.setLong(1,ciudad);s.setLong(2,usuario);s.executeUpdate();}return null;});}
 public void crearBanco(long administrador,String nombre,long pais){
  if(nombre==null||nombre.isBlank()||nombre.trim().length()>160)throw new NegocioException("El nombre del banco es obligatorio y admite hasta 160 caracteres.");
  transaccion(administrador,c->{
   try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM administrador WHERE id_usuario=?")){s.setLong(1,administrador);try(ResultSet r=s.executeQuery()){if(!r.next())throw new NegocioException("Solo administracion puede registrar bancos.");}}
   try(PreparedStatement s=c.prepareStatement("INSERT INTO banco_operativa(nombre,id_pais) VALUES(?,?)")){s.setString(1,nombre.trim());s.setLong(2,pais);s.executeUpdate();}return null;
  });
 }
}
