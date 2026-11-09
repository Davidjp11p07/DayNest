package co.edu.rentainmuebles.dao.jdbc;

import co.edu.rentainmuebles.dao.ContenidoInmuebleDao;
import co.edu.rentainmuebles.dao.DaoException;
import co.edu.rentainmuebles.model.Calificacion;
import co.edu.rentainmuebles.model.FotoInmueble;
import co.edu.rentainmuebles.model.PeriodoEstadia;
import co.edu.rentainmuebles.model.Ocupacion;
import java.time.LocalDate;
import co.edu.rentainmuebles.config.ConexionBD;
import java.sql.*;
import java.util.*;

public final class ContenidoInmuebleDaoJdbc implements ContenidoInmuebleDao {
    private <T> T con(Sql<T> trabajo) {
        try (Connection c=ConexionBD.abrir()) { return trabajo.run(c); }
        catch(SQLException e) { throw new DaoException("No se pudo acceder a fotos o calificaciones.",e); }
    }
    @FunctionalInterface private interface Sql<T> { T run(Connection c) throws SQLException; }

    public List<Ocupacion> ocupacionesCalendario(long inmueble, Long propietario, LocalDate hoy) {
        return con(c -> {
            String sql = "SELECT o.fecha_inicio,o.fecha_fin,o.tipo FROM ocupacion o JOIN inmueble_operativo i USING(id_inmueble) "
                    + "WHERE o.id_inmueble=? AND o.activa AND o.fecha_fin>? AND "
                    + (propietario == null ? "i.estado='PUBLICADO'" : "i.id_propietario=?") + " ORDER BY o.fecha_inicio";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setLong(1, inmueble); s.setObject(2, hoy);
                if (propietario != null) s.setLong(3, propietario);
                try (ResultSet r = s.executeQuery()) {
                    List<Ocupacion> resultado = new ArrayList<>();
                    while (r.next()) resultado.add(new Ocupacion(0, inmueble, r.getObject(1, LocalDate.class), r.getObject(2, LocalDate.class), r.getString(3), true));
                    return resultado;
                }
            }
        });
    }

    public List<PeriodoEstadia> fechasNoDisponibles(long inmueble, LocalDate hoy) {
        return con(c -> {
            // Solo fechas: no se publican clientes, reservas ni motivos de bloqueos.
            String sql = "SELECT o.fecha_inicio,o.fecha_fin FROM ocupacion o JOIN inmueble i USING(id_inmueble) "
                    + "WHERE o.id_inmueble=? AND i.estado='PUBLICADO' AND o.activa AND o.fecha_fin>? "
                    + "ORDER BY o.fecha_inicio,o.fecha_fin";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setLong(1, inmueble); s.setObject(2, hoy);
                try (ResultSet r = s.executeQuery()) {
                    List<PeriodoEstadia> fechas = new ArrayList<>();
                    while (r.next()) fechas.add(new PeriodoEstadia(r.getObject(1, LocalDate.class), r.getObject(2, LocalDate.class)));
                    return fechas;
                }
            }
        });
    }

    public long agregarFoto(long propietarioId,long inmuebleId,String tipo,String mime,String descripcion,byte[] datos) {
        return con(c->{
            boolean auto=c.getAutoCommit();if(auto)c.setAutoCommit(false);
            try {
            try(PreparedStatement lock=c.prepareStatement("SELECT i.id_i FROM inmuebles i JOIN asocia a ON a.id_i=i.id_i WHERE i.id_i=? AND a.id_p=? FOR UPDATE OF i")) {
                lock.setLong(1,inmuebleId);lock.setLong(2,propietarioId);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new DaoException("El inmueble no existe o no pertenece al propietario.");}
            }
            try(PreparedStatement count=c.prepareStatement("SELECT count(*) FROM inmueble_foto WHERE id_inmueble=?")){count.setLong(1,inmuebleId);try(ResultSet r=count.executeQuery()){r.next();if(r.getInt(1)>=10)throw new DaoException("Cada inmueble admite hasta 10 fotos.");}}
            long id;
            try(PreparedStatement s=c.prepareStatement("INSERT INTO inmueble_foto(id_inmueble,tipo,mime,descripcion,datos) SELECT i.id_inmueble,?,?,?,? FROM inmueble_operativo i WHERE i.id_inmueble=? AND i.id_propietario=? AND i.estado<>'INACTIVO' RETURNING id_foto")) {
                s.setString(1,tipo);s.setString(2,mime);s.setString(3,descripcion);s.setBytes(4,datos);s.setLong(5,inmuebleId);s.setLong(6,propietarioId);
                try(ResultSet r=s.executeQuery()){if(!r.next())throw new DaoException("El inmueble no existe o no pertenece al propietario.");id=r.getLong(1);}
            }
            if(auto)c.commit();return id;
            } catch(SQLException|RuntimeException|Error e) {if(auto)c.rollback();throw e;} finally {if(auto)c.setAutoCommit(true);}
        });
    }
    private List<FotoInmueble> fotos(String sql,long id) {
        return con(c->{try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);try(ResultSet r=s.executeQuery()){List<FotoInmueble> out=new ArrayList<>();while(r.next())out.add(new FotoInmueble(r.getLong(1),r.getString(2),r.getString(3),r.getString(4)));return out;}}});
    }
    public List<FotoInmueble> fotosPublicas(long id){return fotos("SELECT f.id_foto,f.tipo,f.mime,f.descripcion FROM inmueble_foto f JOIN inmueble i USING(id_inmueble) WHERE f.id_inmueble=? AND i.estado='PUBLICADO' ORDER BY f.id_foto",id);}
    public List<FotoInmueble> fotosPropias(long propietario,long id){return con(c->{try(PreparedStatement s=c.prepareStatement("SELECT f.id_foto,f.tipo,f.mime,f.descripcion FROM inmueble_foto f JOIN inmueble_operativo i USING(id_inmueble) WHERE f.id_inmueble=? AND i.id_propietario=? ORDER BY f.id_foto")){s.setLong(1,id);s.setLong(2,propietario);try(ResultSet r=s.executeQuery()){List<FotoInmueble> out=new ArrayList<>();while(r.next())out.add(new FotoInmueble(r.getLong(1),r.getString(2),r.getString(3),r.getString(4)));return out;}}});}
    public Optional<Imagen> imagenPublica(long id){return con(c->{try(PreparedStatement s=c.prepareStatement("SELECT f.mime,f.datos FROM inmueble_foto f JOIN inmueble i USING(id_inmueble) WHERE f.id_foto=? AND i.estado='PUBLICADO'")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(new Imagen(r.getString(1),r.getBytes(2))):Optional.empty();}}});}
    public Optional<Imagen> imagenPropia(long propietario,long id){return con(c->{try(PreparedStatement s=c.prepareStatement("SELECT f.mime,f.datos FROM inmueble_foto f JOIN inmueble_operativo i USING(id_inmueble) WHERE f.id_foto=? AND i.id_propietario=?")){s.setLong(1,id);s.setLong(2,propietario);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(new Imagen(r.getString(1),r.getBytes(2))):Optional.empty();}}});}
    public void eliminarFoto(long propietario,long id){con(c->{try(PreparedStatement s=c.prepareStatement("DELETE FROM inmueble_foto f USING inmueble_operativo i WHERE f.id_inmueble=i.id_inmueble AND i.id_propietario=? AND f.id_foto=?")){s.setLong(1,propietario);s.setLong(2,id);if(s.executeUpdate()!=1)throw new DaoException("La foto no existe o no pertenece a tus inmuebles.");return null;}});}
    public void calificar(long usuario,String rol,long reserva,int puntos,String comentario){
        if(!List.of("CLIENTE","PROPIETARIO").contains(rol)||puntos<1||puntos>5)throw new IllegalArgumentException("Calificación inválida.");
        String sql="INSERT INTO calificacion_reserva(id_ocupacion,rol_autor,puntuacion,comentario) SELECT r.id_ocupacion,?,?,? FROM renta r JOIN ocupacion o USING(id_ocupacion) WHERE r.id_ocupacion=? AND r.estado='FINALIZADA' AND o.fecha_fin<=CURRENT_DATE AND ((?='CLIENTE' AND r.id_cl=?) OR (?='PROPIETARIO' AND r.id_p=?)) ON CONFLICT(id_ocupacion,rol_autor) DO NOTHING";
        con(c->{try(PreparedStatement s=c.prepareStatement(sql)){s.setString(1,rol);s.setInt(2,puntos);s.setString(3,comentario);s.setLong(4,reserva);s.setString(5,rol);s.setLong(6,usuario);s.setString(7,rol);s.setLong(8,usuario);if(s.executeUpdate()!=1)throw new DaoException("La estadía no está finalizada, no te pertenece o ya la calificaste.");return null;}});
    }
    public boolean yaCalifico(long usuario,String rol,long reserva){return con(c->{try(PreparedStatement s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM calificacion_reserva c JOIN renta r USING(id_ocupacion) WHERE c.id_ocupacion=? AND c.rol_autor=? AND ((?='CLIENTE' AND r.id_cl=?) OR (?='PROPIETARIO' AND r.id_p=?)))")){s.setLong(1,reserva);s.setString(2,rol);s.setString(3,rol);s.setLong(4,usuario);s.setString(5,rol);s.setLong(6,usuario);try(ResultSet r=s.executeQuery()){r.next();return r.getBoolean(1);}}});}
    public List<Calificacion> calificacionesDelCliente(long cliente){return con(c->{try(PreparedStatement s=c.prepareStatement("SELECT c.id_ocupacion,'Propietario',c.puntuacion,c.comentario,c.creada_en FROM calificacion_reserva c JOIN renta r USING(id_ocupacion) WHERE r.id_cl=? AND c.rol_autor='PROPIETARIO' ORDER BY c.creada_en DESC")){s.setLong(1,cliente);try(ResultSet r=s.executeQuery()){List<Calificacion> out=new ArrayList<>();while(r.next())out.add(new Calificacion(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getObject(5,java.time.OffsetDateTime.class)));return out;}}});}
    public List<Calificacion> calificacionesInmueble(long inmueble){return con(c->{try(PreparedStatement s=c.prepareStatement("SELECT c.id_ocupacion,'Huesped verificado',c.puntuacion,c.comentario,c.creada_en FROM calificacion_reserva c JOIN renta r USING(id_ocupacion) WHERE r.id_i=? AND c.rol_autor='CLIENTE' ORDER BY c.creada_en DESC")){s.setLong(1,inmueble);try(ResultSet r=s.executeQuery()){List<Calificacion> out=new ArrayList<>();while(r.next())out.add(new Calificacion(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getObject(5,java.time.OffsetDateTime.class)));return out;}}});}
}
