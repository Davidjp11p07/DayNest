package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.dao.*;import co.edu.rentainmuebles.dao.jdbc.*;import co.edu.rentainmuebles.model.*;import java.time.*;import java.util.*;
public final class BloqueoService {
 private final BloqueoDao dao=new BloqueoDaoJdbc();
 private void propietario(long id){Usuario u=new UsuarioDaoJdbc().buscarPorId(id).orElseThrow(()->new NegocioException("Usuario inexistente."));if(!u.isActivo()||!u.isPropietario())throw new NegocioException("Perfil propietario requerido.");}
 public List<Bloqueo> listar(long id){propietario(id);try{return dao.listar(id);}catch(DaoException e){throw new NegocioException("No pudo consultar los bloqueos.",e);}}
 public void crear(long propietario,long inmueble,LocalDate inicio,LocalDate fin,String motivo){propietario(propietario);try{
  new PeriodoEstadia(inicio,fin);if(inicio.isBefore(LocalDate.now(ZoneId.of("America/Bogota")))||motivo==null||motivo.isBlank()||motivo.length()>250)throw new NegocioException("Fechas futuras y motivo de hasta 250 caracteres requeridos.");
  dao.crear(propietario,inmueble,inicio,fin,motivo);
 }catch(DaoException|IllegalArgumentException|NullPointerException e){throw new NegocioException("Las fechas están ocupadas, los datos no son válidos o el inmueble es ajeno.",e);}}
 public void liberar(long propietario,long id){propietario(propietario);try{dao.liberar(propietario,id);}catch(DaoException e){throw new NegocioException("No pudo liberar el bloqueo.",e);}}
}