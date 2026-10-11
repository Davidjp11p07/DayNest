package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoService {
    Pago registrar(long clienteId, long reservaId, MetodoPago metodo, String referencia);
    List<Pago> listarPropios(long clienteId);
    ResumenReserva resumen(long clienteId,long reservaId);
}
