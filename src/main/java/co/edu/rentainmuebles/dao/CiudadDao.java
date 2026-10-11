package co.edu.rentainmuebles.dao;
import co.edu.rentainmuebles.model.Ciudad;
import java.util.List;
public interface CiudadDao { List<Ciudad> listar(); Ciudad resolver(String ciudad); java.util.Map<Long,String> paises(); java.util.Map<Long,String> divisiones(); }
