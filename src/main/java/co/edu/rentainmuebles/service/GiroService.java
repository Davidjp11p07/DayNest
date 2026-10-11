package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GiroService {
    Giro generar(long administradorId, long reservaId, String referencia);
    List<Giro> listarPropios(long propietarioId);
}
