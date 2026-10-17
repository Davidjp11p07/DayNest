package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.dao.UnidadTrabajoDao;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class PagoServiceImpl extends ServicioBase implements PagoService {

    public PagoServiceImpl(UnidadTrabajoDao trabajo, Clock reloj) { super(trabajo,reloj); }
    @Override public Pago registrar(long clienteId, long reservaId, MetodoPago metodo, String referencia) {
        return ejecutar(d -> {
            actor(d,clienteId,Rol.CLIENTE); Reserva r = reserva(d,reservaId); propia(r,clienteId);
            if(!r.getExpiraEn().toInstant().isAfter(reloj.instant()))throw new NegocioException("La reserva venció; cree una nueva.");
            if (r.getEstado() != EstadoReserva.PENDIENTE_PAGO) { throw new NegocioException("Solo se pueden pagar reservas pendientes."); }
            if (!r.getPeriodo().getEntrada().isAfter(hoy())) { throw new NegocioException("El pago debe realizarse antes del día de entrada."); }
            if(d.reservas().existeCruce(r.getInmueble().getId(),r.getPeriodo(),r.getId()))
                throw new NegocioException("El inmueble no está disponible en esas fechas para confirmar la reserva.");
            if(!d.pagos().tieneCuentaCliente(reservaId))throw new NegocioException("Vincule su cuenta bancaria en Mis datos y cuentas antes de pagar.");
            Pago p = d.pagos().crear(new Pago(null,r,r.getTotal().subtract(d.pagos().totalPagado(reservaId)),referencia,ahora(),EstadoPago.APROBADO,"COBRO",null,"cobro-reserva-"+reservaId));
            d.reservas().actualizarEstado(reservaId,EstadoReserva.CONFIRMADA);
            Reserva confirmada = d.reservas().buscarPorId(reservaId).orElseThrow(() -> new NegocioException("No se pudo recuperar la reserva confirmada."));
            return new Pago(p.getId(),confirmada,p.getMonto(),p.getReferencia(),p.getFecha(),p.getMetodo());
        });
    }
    @Override public ResumenReserva resumen(long clienteId,long reservaId) {
        return ejecutar(d->{
            actor(d,clienteId,Rol.CLIENTE);
            Reserva r=d.reservas().buscarPorId(id(reservaId)).orElseThrow(()->new NegocioException("La reserva no existe."));
            propia(r,clienteId);
            BigDecimal pagado=d.pagos().totalPagado(reservaId);
            return new ResumenReserva(reservaId,r.getPeriodo().getNoches(),r.getTotal(),pagado,r.getTotal().subtract(pagado));
        });
    }
    @Override public List<Pago> listarPropios(long clienteId) {
        return ejecutar(d -> { actor(d,clienteId,Rol.CLIENTE); return d.pagos().listar().stream().filter(p -> p.getReserva().getCliente().getId() == clienteId).toList(); });
    }

}
