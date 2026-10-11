package co.edu.rentainmuebles.service;
import co.edu.rentainmuebles.dao.CiudadDao;import co.edu.rentainmuebles.model.Ciudad;import java.util.List;
public final class CatalogoService {
 private final CiudadDao dao;
 public CatalogoService(CiudadDao dao){this.dao=dao;}
 public List<Ciudad> ciudades(){return dao.listar();}
 public java.util.Map<Long,String> paises(){return dao.paises();}
 public java.util.Map<Long,String> divisiones(){return dao.divisiones();}
 public Ciudad resolver(String texto){try{return dao.resolver(texto);}catch(IllegalArgumentException e){throw new NegocioException(e.getMessage());}}
}