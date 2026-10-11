package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.model.*;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/** Ausencia de resultados: Optional vacío o lista vacía; fallos: DaoException. */
public interface InmuebleDao {
    /** Bloqueo de fila para coordinar ediciones y reservas dentro de UnidadTrabajoDao. */
    Optional<Inmueble> buscarPorIdParaActualizar(long id);
    Optional<Inmueble> buscarPorId(long id);
    List<Inmueble> listar();
    Inmueble crear(Inmueble inmueble);
    Inmueble actualizar(Inmueble inmueble);
    void eliminar(long id);
    void desactivar(long id);
    List<Inmueble> buscarPorPropietario(long propietarioId);
    List<Inmueble> buscar(String ciudad, BigDecimal precioMinimo, BigDecimal precioMaximo, Integer capacidadMinima, PeriodoEstadia periodo);
    List<Inmueble> buscar(String ciudad,BigDecimal minimo,BigDecimal maximo,Integer capacidad,PeriodoEstadia periodo,FiltrosInmueble filtros);
}
