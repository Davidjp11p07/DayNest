package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.model.*;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/** Ausencia de resultados: Optional vacío o lista vacía; fallos: DaoException. */
public interface ReservaDao {
    /** Bloqueo de fila para serializar pagos, cancelaciones y giros. */
    Optional<Reserva> buscarPorIdParaActualizar(long id);
    Optional<Reserva> buscarPorId(long id);
    List<Reserva> listar();
    Optional<BigDecimal> comisionHistorica(long id);
    Reserva crear(Reserva reserva);
    void actualizarEstado(long id, EstadoReserva estado);
    List<Reserva> buscarPorPropietario(long propietarioId);
    List<Reserva> buscarPorCliente(long clienteId);
    List<Reserva> buscarPorInmueble(long inmuebleId);
    List<Reserva> buscarPorEstado(EstadoReserva estado);
    boolean existeCruce(long inmuebleId, PeriodoEstadia periodo, Long reservaExcluida);
}
