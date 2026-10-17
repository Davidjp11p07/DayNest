package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.dao.UnidadTrabajoDao;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class InmuebleServiceImpl extends ServicioBase implements InmuebleService {
    @Override public Inmueble obtenerPublicado(long inmuebleId) {
        return ejecutar(d -> {
            Inmueble i = d.inmuebles().buscarPorId(id(inmuebleId)).orElseThrow(() -> new NegocioException("El inmueble no existe."));
            if (i.getEstado() != EstadoInmueble.PUBLICADO || !i.getPropietario().isActivo()) { throw new NegocioException("El inmueble no está disponible para publicación."); }
            return i;
        });
    }

    public InmuebleServiceImpl(UnidadTrabajoDao trabajo, Clock reloj) { super(trabajo,reloj); }
    @Override public Inmueble publicar(long propietarioId, String titulo, String ciudad, String direccion, String descripcion, int capacidad, BigDecimal precio) {
        return publicar(propietarioId,titulo,ciudad,direccion,descripcion,capacidad,precio,null,null,null);
    }
    @Override public Inmueble publicar(long propietarioId,String titulo,String ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,String tipo,Boolean rural,Boolean mar) {
        return publicar(propietarioId,titulo,ciudad,direccion,descripcion,capacidad,precio,tipo,rural,mar,null,null);
    }
    @Override public Inmueble publicar(long propietarioId,String titulo,String ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,String tipo,Boolean rural,Boolean mar,BigDecimal area,Integer habitaciones) {
        return ejecutar(d->{Propietario p=actor(d,propietarioId,Rol.PROPIETARIO).getPropietario();
            var ubicacion=new co.edu.rentainmuebles.dao.jdbc.CiudadDaoJdbc().resolver(ciudad);
            return d.inmuebles().crear(new Inmueble(null,p,titulo,ubicacion,direccion,descripcion,capacidad,precio,EstadoInmueble.PUBLICADO,tipo,ubicacion.getMoneda(),rural,mar).conDetalles(area,habitaciones));});
    }
    @Override public Inmueble editar(long propietarioId, long inmuebleId, String titulo, String ciudad, String direccion, String descripcion, int capacidad, BigDecimal precio) {
        return editarEn(propietarioId,inmuebleId,titulo,ciudad,direccion,descripcion,capacidad,precio,null,null,null,true,null,null,true);
    }
    @Override public Inmueble editar(long propietarioId,long inmuebleId,String titulo,String ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,String tipo,Boolean rural,Boolean mar) {
        return editarEn(propietarioId,inmuebleId,titulo,ciudad,direccion,descripcion,capacidad,precio,tipo,rural,mar,false,null,null,true);
    }
    @Override public Inmueble editar(long propietarioId,long inmuebleId,String titulo,String ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,String tipo,Boolean rural,Boolean mar,BigDecimal area,Integer habitaciones) {
        return editarEn(propietarioId,inmuebleId,titulo,ciudad,direccion,descripcion,capacidad,precio,tipo,rural,mar,false,area,habitaciones,false);
    }
    private Inmueble editarEn(long propietarioId,long inmuebleId,String titulo,String ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,String tipo,Boolean rural,Boolean mar,boolean conservar,BigDecimal area,Integer habitaciones,boolean conservarDetalles) {
        return ejecutar(d -> {
            Propietario p = actor(d,propietarioId,Rol.PROPIETARIO).getPropietario();
            Inmueble i = inmueble(d,inmuebleId); pertenece(i,propietarioId);
            if(d.reservas().buscarPorInmueble(inmuebleId).stream().anyMatch(r->r.getHuespedes()>capacidad && r.getPeriodo().getSalida().isAfter(hoy()) && r.getEstado()!=EstadoReserva.CANCELADA && r.getEstado()!=EstadoReserva.EXPIRADA))
                throw new NegocioException("La capacidad no puede ser menor que los huéspedes de reservas existentes.");
            return d.inmuebles().actualizar(new Inmueble(i.getId(),p,titulo,new co.edu.rentainmuebles.dao.jdbc.CiudadDaoJdbc().resolver(ciudad),direccion,descripcion,capacidad,precio,i.getEstado(),conservar?i.getTipo():tipo,i.getMoneda(),conservar?i.getEsRural():rural,conservar?i.getCercaMar():mar).conDetalles(conservarDetalles?i.getAreaM2():area,conservarDetalles?i.getHabitaciones():habitaciones));
        });
    }
    @Override public void desactivar(long propietarioId, long inmuebleId) {
        ejecutar(d -> { actor(d,propietarioId,Rol.PROPIETARIO); pertenece(inmueble(d,inmuebleId),propietarioId); d.inmuebles().desactivar(inmuebleId); return null; });
    }
    @Override public List<Inmueble> listarPropios(long propietarioId) {
        return ejecutar(d -> { actor(d,propietarioId,Rol.PROPIETARIO); return d.inmuebles().buscarPorPropietario(propietarioId); });
    }
    @Override public List<Inmueble> buscar(String ciudad, BigDecimal minimo, BigDecimal maximo, Integer capacidad, LocalDate entrada, LocalDate salida) {
        return buscar(ciudad,minimo,maximo,capacidad,entrada,salida,FiltrosInmueble.ninguno());
    }
    @Override public List<Inmueble> buscar(String ciudad,BigDecimal minimo,BigDecimal maximo,Integer capacidad,LocalDate entrada,LocalDate salida,FiltrosInmueble filtros) {
        return ejecutar(d -> {
            if ((entrada == null) != (salida == null)) { throw new NegocioException("Indique ambas fechas para buscar disponibilidad."); }
            PeriodoEstadia periodo = entrada == null ? null : new PeriodoEstadia(entrada,salida);
            if (entrada != null && !entrada.isAfter(hoy())) { throw new NegocioException("La entrada debe ser posterior al día de hoy."); }
            return d.inmuebles().buscar(ciudad,minimo,maximo,capacidad,periodo,filtros).stream()
                    .filter(i->i.getPropietario().isActivo()).toList();
        });
    }
    private static void pertenece(Inmueble i, long propietarioId) {
        if (i.getPropietario().getId() != propietarioId) { throw new NegocioException("El inmueble no pertenece al propietario."); }
    }

}
