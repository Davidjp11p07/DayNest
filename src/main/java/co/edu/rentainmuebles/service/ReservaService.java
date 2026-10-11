package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReservaService {
    List<Reserva> listarRecibidas(long propietarioId);
    Reserva crear(long clienteId, long inmuebleId, LocalDate entrada, LocalDate salida, int huespedes);
    void cancelar(long clienteId, long reservaId);
    List<Reserva> listarPropias(long clienteId);
}
