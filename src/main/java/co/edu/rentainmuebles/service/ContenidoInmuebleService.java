package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.dao.ContenidoInmuebleDao;
import co.edu.rentainmuebles.dao.DaoException;
import co.edu.rentainmuebles.dao.jdbc.ContenidoInmuebleDaoJdbc;
import co.edu.rentainmuebles.model.Calificacion;
import co.edu.rentainmuebles.model.FotoInmueble;
import co.edu.rentainmuebles.model.PeriodoEstadia;
import co.edu.rentainmuebles.model.Ocupacion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Validaciones de contenido y errores recuperables para los formularios. */
public final class ContenidoInmuebleService implements ContenidoInmuebleDao {
    private final ContenidoInmuebleDao dao = new ContenidoInmuebleDaoJdbc();

    private <T> T ejecutar(Supplier<T> accion) {
        try { return accion.get(); }
        catch (DaoException e) { throw new NegocioException(e.getMessage(), e); }
    }

    public long agregarFoto(long propietario, long inmueble, String tipo, String mime, String descripcion, byte[] datos) {
        if (!List.of("INTERIOR", "EXTERIOR").contains(tipo == null ? "" : tipo))
            throw new NegocioException("Selecciona si la foto es del interior o exterior.");
        if (descripcion == null) descripcion = "";
        if (descripcion.length() > 160) throw new NegocioException("La descripción admite hasta 160 caracteres.");
        final String texto = descripcion;
        return ejecutar(() -> dao.agregarFoto(propietario, inmueble, tipo, mime, texto, datos));
    }

    public void eliminarFoto(long propietario, long foto) {
        ejecutar(() -> { dao.eliminarFoto(propietario, foto); return null; });
    }

    public void calificar(long usuario, String rol, long reserva, int puntos, String comentario) {
        if (!List.of("CLIENTE", "PROPIETARIO").contains(rol == null ? "" : rol) || puntos < 1 || puntos > 5)
            throw new NegocioException("Selecciona una calificación de 1 a 5.");
        if (comentario == null) comentario = "";
        if (comentario.length() > 1000) throw new NegocioException("El comentario admite hasta 1000 caracteres.");
        final String texto = comentario;
        ejecutar(() -> { dao.calificar(usuario, rol, reserva, puntos, texto); return null; });
    }

    public List<FotoInmueble> fotosPublicas(long id) { return ejecutar(() -> dao.fotosPublicas(id)); }
    public List<FotoInmueble> fotosPropias(long usuario, long id) { return ejecutar(() -> dao.fotosPropias(usuario, id)); }
    public Optional<Imagen> imagenPublica(long id) { return ejecutar(() -> dao.imagenPublica(id)); }
    public Optional<Imagen> imagenPropia(long usuario, long id) { return ejecutar(() -> dao.imagenPropia(usuario, id)); }
    public boolean yaCalifico(long usuario, String rol, long id) { return ejecutar(() -> dao.yaCalifico(usuario, rol, id)); }
    public List<Calificacion> calificacionesDelCliente(long id) { return ejecutar(() -> dao.calificacionesDelCliente(id)); }
    public List<Calificacion> calificacionesInmueble(long id) { return ejecutar(() -> dao.calificacionesInmueble(id)); }
    public List<PeriodoEstadia> fechasNoDisponibles(long id, LocalDate hoy) { return ejecutar(() -> dao.fechasNoDisponibles(id, hoy)); }
    public List<Ocupacion> ocupacionesCalendario(long id, Long propietario, LocalDate hoy) { return ejecutar(() -> dao.ocupacionesCalendario(id, propietario, hoy)); }
}
