package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.model.Calificacion;
import co.edu.rentainmuebles.model.FotoInmueble;
import java.util.List;
import java.util.Optional;

public interface ContenidoInmuebleDao {
    long agregarFoto(long propietarioId, long inmuebleId, String tipo, String mime, String descripcion, byte[] datos);
    List<FotoInmueble> fotosPublicas(long inmuebleId);
    List<FotoInmueble> fotosPropias(long propietarioId, long inmuebleId);
    Optional<Imagen> imagenPublica(long fotoId);
    Optional<Imagen> imagenPropia(long propietarioId, long fotoId);
    void eliminarFoto(long propietarioId, long fotoId);
    void calificar(long usuarioId, String rol, long reservaId, int puntuacion, String comentario);
    boolean yaCalifico(long usuarioId, String rol, long reservaId);
    List<Calificacion> calificacionesDelCliente(long clienteId);
    List<Calificacion> calificacionesInmueble(long inmuebleId);

    record Imagen(String mime, byte[] datos) { }
}
