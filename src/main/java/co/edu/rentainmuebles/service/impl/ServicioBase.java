package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.dao.*;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.NegocioException;
import java.time.*;
import java.util.Objects;
import java.util.function.Function;

abstract class ServicioBase {
    protected final UnidadTrabajoDao trabajo;
    protected final Clock reloj;
    ServicioBase(UnidadTrabajoDao trabajo, Clock reloj) {
        this.trabajo = Objects.requireNonNull(trabajo); this.reloj = Objects.requireNonNull(reloj);
    }
    protected <T> T ejecutar(Function<UnidadTrabajoDao.Daos,T> operacion) {
        try { return trabajo.ejecutar(operacion); }
        catch (DaoException e) {
            String estado = e.estadoSql();
            String mensaje = estado == null ? "No se pudo completar la operación de persistencia." : switch (estado) {
                case "23505" -> "Ya existe un registro con ese correo, reserva o referencia; no se permiten pagos ni giros duplicados.";
                case "23P01" -> "El inmueble acaba de ser reservado en esas fechas. Elija otro período.";
                case "23503", "23001" -> "La operación referencia datos inexistentes o relacionados que no se pueden eliminar.";
                case "23514" -> "Los datos no cumplen las restricciones de fechas, importes o estados.";
                default -> "No se pudo completar la operación. Revise la conexión con la base de datos e inténtelo nuevamente.";
            };
            throw new NegocioException(mensaje,e);
        }
        catch (IllegalArgumentException | NullPointerException e) { throw new NegocioException("Datos inválidos: " + (e.getMessage() == null ? "faltan datos obligatorios." : e.getMessage()),e); }
    }
    protected static long id(long id) {
        if (id <= 0) { throw new NegocioException("El identificador debe ser positivo."); }
        return id;
    }
    protected static Usuario actor(UnidadTrabajoDao.Daos d, long id, Rol rol) {
        Usuario u = d.usuarios().buscarPorId(id(id)).orElseThrow(() -> new NegocioException("El usuario no existe."));
        if (!u.isActivo() || !u.tieneRol(rol)) { throw new NegocioException("El usuario debe estar activo y tener el permiso o perfil " + rol + "."); }
        return u;
    }
    protected static Inmueble inmueble(UnidadTrabajoDao.Daos d, long id) {
        return d.inmuebles().buscarPorIdParaActualizar(id(id)).orElseThrow(() -> new NegocioException("El inmueble no existe."));
    }
    protected static Reserva reserva(UnidadTrabajoDao.Daos d, long id) {
        return d.reservas().buscarPorIdParaActualizar(id(id)).orElseThrow(() -> new NegocioException("La reserva no existe."));
    }
    protected LocalDate hoy() { return LocalDate.now(reloj); }
    protected LocalDateTime ahora() { return LocalDateTime.now(reloj); }
    protected static void propia(Reserva r, long clienteId) {
        if (r.getCliente().getId() != clienteId) { throw new NegocioException("La reserva no pertenece al cliente."); }
    }
    protected void cancelarPendiente(UnidadTrabajoDao.Daos d, Reserva r) {
        if(r.getEstado()!=EstadoReserva.PENDIENTE_PAGO && r.getEstado()!=EstadoReserva.CONFIRMADA)throw new NegocioException("Solo puede cancelar una reserva pendiente o confirmada.");
        if (!r.getPeriodo().getEntrada().isAfter(hoy()))throw new NegocioException("La cancelación debe realizarse antes de la entrada.");
        d.reservas().actualizarEstado(r.getId(),EstadoReserva.CANCELADA);
        for(Pago cobro:d.pagos().listarPorReserva(r.getId())) {
            if(cobro.getEstado()==EstadoPago.APROBADO && cobro.getClase().equals("COBRO")) {
                java.math.BigDecimal devuelto=d.pagos().listarPorReserva(r.getId()).stream().filter(p->p.getClase().equals("DEVOLUCION")&&p.getEstado()==EstadoPago.APROBADO&&cobro.getId().equals(p.getCobroOrigen())).map(Pago::getMonto).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
                java.math.BigDecimal monto=cobro.getMonto().subtract(devuelto);
                if(monto.signum()>0)d.pagos().crear(new Pago(null,r,monto,"SIM-DEV-"+cobro.getId(),ahora(),EstadoPago.APROBADO,"DEVOLUCION",cobro.getId(),"devolucion-cobro-"+cobro.getId()));
            }
        }
    }
}
