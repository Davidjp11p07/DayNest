package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.dao.UnidadTrabajoDao;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ReservaServiceImpl extends ServicioBase implements ReservaService {
    @Override public List<Reserva> listarRecibidas(long propietarioId) {
        return ejecutar(d -> {
            actor(d,propietarioId,Rol.PROPIETARIO);
            return d.reservas().listar().stream().filter(r->r.getPropietarioPactado().getId()==propietarioId).toList();
        });
    }

    public ReservaServiceImpl(UnidadTrabajoDao trabajo, Clock reloj) { super(trabajo,reloj); }
    @Override public Reserva crear(long clienteId, long inmuebleId, LocalDate entrada, LocalDate salida, int huespedes) {
        return ejecutar(d -> {
            Cliente c = actor(d,clienteId,Rol.CLIENTE).getCliente();
            PeriodoEstadia periodo = new PeriodoEstadia(entrada,salida);
            if (!entrada.isAfter(hoy())) { throw new NegocioException("La entrada debe ser posterior al día de hoy."); }
            Inmueble i = inmueble(d,inmuebleId);
            if (i.getEstado() != EstadoInmueble.PUBLICADO || !i.getPropietario().isActivo()) { throw new NegocioException("El inmueble o su propietario están inactivos."); }
            if (huespedes <= 0 || huespedes > i.getCapacidad()) { throw new NegocioException("La cantidad de huéspedes debe ser positiva y no exceder la capacidad del inmueble."); }
            if (d.reservas().existeCruce(inmuebleId,periodo,null)) { throw new NegocioException("El inmueble no está disponible en esas fechas."); }
            return d.reservas().crear(new Reserva(null,c,i,periodo,huespedes,i.getPrecioPorNoche(),EstadoReserva.PENDIENTE_PAGO,ahora()));
        });
    }
    @Override public void cancelar(long clienteId, long reservaId) {
        ejecutar(d -> { actor(d,clienteId,Rol.CLIENTE); Reserva r = reserva(d,reservaId); propia(r,clienteId); cancelarPendiente(d,r); return null; });
    }
    @Override public List<Reserva> listarPropias(long clienteId) {
        return ejecutar(d -> { actor(d,clienteId,Rol.CLIENTE); return d.reservas().buscarPorCliente(clienteId); });
    }

}
