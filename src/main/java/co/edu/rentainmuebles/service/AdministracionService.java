package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AdministracionService {
    List<Inmueble> listarInmuebles(long administradorId, EstadoInmueble estado);
    List<Reserva> listarReservas(long administradorId, EstadoReserva estado);
    List<Pago> listarPagos(long administradorId, EstadoReserva estadoReserva);
    List<Giro> listarGiros(long administradorId, EstadoReserva estadoReserva);
    void desactivarInmueble(long administradorId, long inmuebleId);
    void cancelarReserva(long administradorId, long reservaId);
    Giro finalizarYGenerarGiro(long administradorId, long reservaId, String referencia);
}
