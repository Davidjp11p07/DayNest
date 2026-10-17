package co.edu.rentainmuebles.dao.jdbc;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.dao.*;
import java.sql.*;import java.util.*;import java.text.Normalizer;
public final class CiudadDaoJdbc extends JdbcSoporte<Ciudad> implements CiudadDao {
 public CiudadDaoJdbc(){super();} CiudadDaoJdbc(Connection c){super(c);}
 static final String SELECT="SELECT c.*,d.nombre division_nombre,d.codigo_oficial division_codigo,d.id_pais,p.nombre pais_nombre,p.codigo_iso2 FROM ciudad c JOIN division_administrativa d USING(id_division) JOIN pais p ON p.id_pais=d.id_pais";
 protected Ciudad mapear(Connection c,ResultSet r)throws SQLException{return new Ciudad(r.getLong("id_ciudad"),new DivisionAdministrativa(r.getLong("id_division"),new Pais(r.getLong("id_pais"),r.getString("pais_nombre"),r.getString("codigo_iso2")),r.getString("division_nombre"),r.getString("division_codigo")),r.getString("nombre"),r.getString("codigo_oficial"));}
 public static String normalizar(String s){return Normalizer.normalize(s.trim(),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
 public List<Ciudad> listar(){return consultar(SELECT+" ORDER BY c.nombre,p.codigo_iso2",s->{});}
 public Ciudad resolver(String texto){
  if(texto==null||texto.isBlank())throw new IllegalArgumentException("Seleccione una ciudad del catálogo.");
  List<Ciudad> a=listar().stream().filter(c->texto.equals("id:"+c.id())||normalizar(c.getEtiqueta()).equals(normalizar(texto))||normalizar(c.nombre()).equals(normalizar(texto))).toList();
  if(a.size()!=1)throw new IllegalArgumentException(a.isEmpty()?"Ciudad fuera del catálogo.":"Seleccione ciudad, región y país para distinguir homónimos.");return a.get(0);
 }
 Ciudad buscar(long id){return uno(SELECT+" WHERE c.id_ciudad=?",s->s.setLong(1,id)).orElseThrow(()->new DaoException("Ciudad inexistente."));}
 public Map<Long,String> paises(){return etiquetas("SELECT id_pais,nombre FROM pais ORDER BY nombre");}
 public Map<Long,String> divisiones(){return etiquetas("SELECT d.id_division,d.nombre||', '||p.nombre FROM division_administrativa d JOIN pais p ON p.id_pais=d.id_pais ORDER BY d.nombre,p.nombre");}
 private Map<Long,String> etiquetas(String sql){return conexion(c->{Map<Long,String> out=new LinkedHashMap<>();try(Statement st=c.createStatement();ResultSet r=st.executeQuery(sql)){while(r.next())out.put(r.getLong(1),r.getString(2));}return out;});}
}
