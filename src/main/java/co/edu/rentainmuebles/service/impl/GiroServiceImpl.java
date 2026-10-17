package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.dao.UnidadTrabajoDao;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class GiroServiceImpl extends ServicioBase implements GiroService {

    private final PoliticaComision politica;
    public GiroServiceImpl(UnidadTrabajoDao trabajo, Clock reloj, PoliticaComision politica) { super(trabajo,reloj); this.politica = java.util.Objects.requireNonNull(politica); }
    @Override public Giro generar(long administradorId, long reservaId, String referencia) {
        return ejecutar(d -> {
            actor(d,administradorId,Rol.ADMINISTRADOR); Reserva r = reserva(d,reservaId);
            if (d.giros().buscarPorReserva(reservaId).isPresent()) { throw new NegocioException("La reserva ya tiene un giro generado."); }
            if (r.getPeriodo().getSalida().isAfter(hoy())) { throw new NegocioException("La estadía todavía no ha finalizado."); }
            if (r.getEstado() != EstadoReserva.CONFIRMADA && r.getEstado() != EstadoReserva.FINALIZADA) { throw new NegocioException("La reserva debe estar confirmada o finalizada para generar el giro."); }
            if (d.pagos().totalPagado(reservaId).compareTo(r.getTotal()) < 0) { throw new NegocioException("La reserva debe estar pagada antes de generar el giro."); }
            if (r.getEstado() == EstadoReserva.CONFIRMADA) d.reservas().actualizarEstado(reservaId,EstadoReserva.FINALIZADA);
            Reserva finalizada=d.reservas().buscarPorId(reservaId).orElseThrow();
            PoliticaComision efectiva=d.reservas().comisionHistorica(reservaId).<PoliticaComision>map(PoliticaComisionPorcentaje::new).orElse(politica);
            PoliticaComision.Desglose desglose=efectiva.calcular(r.getTotal());
            return d.giros().crear(new Giro(null,finalizada,finalizada.getPropietarioPactado(),desglose.neto(),referencia,ahora(),EstadoGiro.EJECUTADO,administradorId,"giro-reserva-"+reservaId+"-"+java.util.UUID.randomUUID(),desglose.porcentaje(),desglose.comision()));
        });
    }
    @Override public List<Giro> listarPropios(long propietarioId) {
        return ejecutar(d -> { actor(d,propietarioId,Rol.PROPIETARIO); return d.giros().buscarPorPropietario(propietarioId); });
    }

}
