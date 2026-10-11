package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InmuebleService {
    Inmueble obtenerPublicado(long inmuebleId);
    Inmueble publicar(long propietarioId, String titulo, String ciudad, String direccion, String descripcion, int capacidad, BigDecimal precio);
    Inmueble editar(long propietarioId, long inmuebleId, String titulo, String ciudad, String direccion, String descripcion, int capacidad, BigDecimal precio);
    void desactivar(long propietarioId, long inmuebleId);
    List<Inmueble> buscar(String ciudad, BigDecimal minimo, BigDecimal maximo, Integer capacidad, LocalDate entrada, LocalDate salida);
    List<Inmueble> buscar(String ciudad,BigDecimal minimo,BigDecimal maximo,Integer capacidad,LocalDate entrada,LocalDate salida,FiltrosInmueble filtros);
    Inmueble publicar(long propietarioId,String titulo,String ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,String tipo,Boolean rural,Boolean mar);
    Inmueble editar(long propietarioId,long inmuebleId,String titulo,String ciudad,String direccion,String descripcion,int capacidad,BigDecimal precio,String tipo,Boolean rural,Boolean mar);
    List<Inmueble> listarPropios(long propietarioId);
}
