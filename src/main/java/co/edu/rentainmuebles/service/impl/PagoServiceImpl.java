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
        if(metodo!=MetodoPago.TRANSFERENCIA)throw new NegocioException("Solo se admite transferencia bancaria simulada.");
        return registrarTransferencia(clienteId,reservaId,referencia,null);
    }
    @Override public Pago registrarTransferencia(long clienteId,long reservaId,String referencia,CuentaBancaria cuentaOcasional) {
        return ejecutar(d -> {
            actor(d,clienteId,Rol.CLIENTE); Reserva r = reserva(d,reservaId); propia(r,clienteId);
            if(!r.getExpiraEn().toInstant().isAfter(reloj.instant()))throw new NegocioException("La reserva venció; cree una nueva.");
            if (r.getEstado() != EstadoReserva.PENDIENTE_PAGO) { throw new NegocioException("Solo se pueden pagar reservas pendientes."); }
            if (!r.getPeriodo().getEntrada().isAfter(hoy())) { throw new NegocioException("El pago debe realizarse antes del día de entrada."); }
            if(d.reservas().existeCruce(r.getInmueble().getId(),r.getPeriodo(),r.getId()))
                throw new NegocioException("El inmueble no está disponible en esas fechas para confirmar la reserva.");
            if(cuentaOcasional==null&&!d.pagos().tieneCuentaCliente(reservaId))throw new NegocioException("Registre una cuenta en Mis datos y cuentas o indique otra solo para este pago.");
            Pago nuevo=new Pago(null,r,r.getTotal().subtract(d.pagos().totalPagado(reservaId)),referencia,ahora(),EstadoPago.APROBADO,"COBRO",null,"cobro-reserva-"+reservaId);
            if(cuentaOcasional!=null)nuevo.conCuentaOcasional(cuentaOcasional);
            Pago p = d.pagos().crear(nuevo);
            d.reservas().actualizarEstado(reservaId,EstadoReserva.CONFIRMADA);
            return d.pagos().buscarPorId(p.getId()).orElseThrow(() -> new NegocioException("No se pudo recuperar el pago confirmado."));
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
