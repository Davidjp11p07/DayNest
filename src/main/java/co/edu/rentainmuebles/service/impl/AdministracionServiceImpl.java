package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.dao.UnidadTrabajoDao;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class AdministracionServiceImpl extends ServicioBase implements AdministracionService {

    private final GiroService giroService;
    public AdministracionServiceImpl(UnidadTrabajoDao trabajo, Clock reloj, GiroService giroService) { super(trabajo,reloj); this.giroService = java.util.Objects.requireNonNull(giroService); }
    @Override public List<Inmueble> listarInmuebles(long administradorId, EstadoInmueble estado) {
        return ejecutar(d -> { actor(d,administradorId,Rol.ADMINISTRADOR); return d.inmuebles().listar().stream().filter(i -> estado == null || i.getEstado() == estado).toList(); });
    }
    @Override public List<Reserva> listarReservas(long administradorId, EstadoReserva estado) {
        return ejecutar(d -> { actor(d,administradorId,Rol.ADMINISTRADOR); return estado == null ? d.reservas().listar() : d.reservas().buscarPorEstado(estado); });
    }
    @Override public List<Pago> listarPagos(long administradorId, EstadoReserva estado) {
        return ejecutar(d -> { actor(d,administradorId,Rol.ADMINISTRADOR); return d.pagos().listar().stream().filter(p -> estado == null || p.getReserva().getEstado() == estado).toList(); });
    }
    @Override public List<Giro> listarGiros(long administradorId, EstadoReserva estado) {
        return ejecutar(d -> { actor(d,administradorId,Rol.ADMINISTRADOR); return d.giros().listar().stream().filter(g -> estado == null || g.getReserva().getEstado() == estado).toList(); });
    }
    @Override public void desactivarInmueble(long administradorId, long inmuebleId) {
        ejecutar(d -> { actor(d,administradorId,Rol.ADMINISTRADOR); inmueble(d,inmuebleId); d.inmuebles().desactivar(inmuebleId); return null; });
    }
    @Override public void cancelarReserva(long administradorId, long reservaId) {
        ejecutar(d -> { actor(d,administradorId,Rol.ADMINISTRADOR); cancelarPendiente(d,reserva(d,reservaId)); return null; });
    }
    @Override public Giro finalizarYGenerarGiro(long administradorId, long reservaId, String referencia) {
        return giroService.generar(administradorId,reservaId,referencia);
    }

}
