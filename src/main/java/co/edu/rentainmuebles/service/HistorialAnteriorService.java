package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.dao.*;import co.edu.rentainmuebles.dao.jdbc.*;import java.util.*;
/** Consulta protegida del archivo previo; nunca altera public. */
public final class HistorialAnteriorService {
 private final HistorialAnteriorDao dao=new HistorialAnteriorDaoJdbc();
 public List<Map<String,Object>> consultar(long id){var u=new UsuarioDaoJdbc().buscarPorId(id).orElseThrow(()->new NegocioException("Usuario inexistente."));if(!u.isActivo()||!u.isEsAdministrador())throw new NegocioException("Administrador requerido.");try{return dao.consultar();}catch(DaoException e){throw new NegocioException("No pudo consultar el historial anterior.",e);}}
}