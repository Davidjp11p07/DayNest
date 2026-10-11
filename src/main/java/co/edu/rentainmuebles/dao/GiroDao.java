package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.model.*;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/** Ausencia de resultados: Optional vacío o lista vacía; fallos: DaoException. */
public interface GiroDao {
    Optional<Giro> buscarPorId(long id);
    List<Giro> listar();
    Giro crear(Giro giro);
    Optional<Giro> buscarPorReserva(long reservaId);
    List<Giro> buscarPorPropietario(long propietarioId);
}
