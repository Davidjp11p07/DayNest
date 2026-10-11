package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.model.*;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/** Ausencia de resultados: Optional vacío o lista vacía; fallos: DaoException. */
public interface PagoDao {
    Optional<Pago> buscarPorId(long id);
    List<Pago> listar();
    Pago crear(Pago pago);
    Optional<Pago> buscarPorReserva(long reservaId);
    List<Pago> listarPorReserva(long reservaId);
    BigDecimal totalPagado(long reservaId);
}
