package co.edu.rentainmuebles.model;

import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalendarioDisponibilidadTest {
    private String estado(CalendarioDisponibilidad c, LocalDate fecha) {
        return c.getDias().stream().filter(d->fecha.equals(d.fecha())).findFirst().orElseThrow().estado();
    }
    @Test void distingueReservasBloqueosYSalidaLibre() {
        LocalDate hoy=LocalDate.of(2026,11,8);
        var c=new CalendarioDisponibilidad(YearMonth.of(2026,11),hoy,List.of(
                new Ocupacion(1,1,hoy.plusDays(2),hoy.plusDays(5),"RESERVA",true),
                new Ocupacion(2,1,hoy.plusDays(7),hoy.plusDays(9),"BLOQUEO",true),
                new Ocupacion(3,1,hoy.plusDays(12),hoy.plusDays(13),"RESERVA",false)));
        assertEquals("PASADO",estado(c,hoy.minusDays(1)));
        assertEquals("RESERVADO",estado(c,hoy.plusDays(2)));
        assertEquals("DISPONIBLE",estado(c,hoy.plusDays(5)));
        assertEquals("BLOQUEADO",estado(c,hoy.plusDays(7)));
        assertEquals("DISPONIBLE",estado(c,hoy.plusDays(9)));
        assertEquals("DISPONIBLE",estado(c,hoy.plusDays(12)));
    }
    @Test void febreroBisiestoYMesesConCambioDeAnio() {
        var c=new CalendarioDisponibilidad(YearMonth.of(2028,2),LocalDate.of(2028,2,1),List.of());
        assertEquals(29,c.getDias().stream().filter(d->d.fecha()!=null).count());
        assertEquals(0,c.getDias().size()%7);
        assertEquals(1,c.getDias().stream().takeWhile(d->d.fecha()==null).count());
        var diciembre=new CalendarioDisponibilidad(YearMonth.of(2026,12),LocalDate.of(2026,12,1),List.of());
        assertEquals("2027-01",diciembre.getSiguiente());
        assertEquals("2026-11",diciembre.getAnterior());
    }
    @Test void clienteNoPuedeEntrarHoyPeroPropietarioPuedeBloquearHoy() {
        LocalDate hoy=LocalDate.of(2026,11,8);
        assertEquals("PASADO",estado(new CalendarioDisponibilidad(YearMonth.from(hoy),hoy,List.of(),true),hoy));
        assertEquals("DISPONIBLE",estado(new CalendarioDisponibilidad(YearMonth.from(hoy),hoy,List.of()),hoy));
    }
    @Test void rechazaMesesMalformados() {
        LocalDate hoy=LocalDate.of(2026,11,8);
        assertEquals(YearMonth.of(2026,11),CalendarioDisponibilidad.mes(null,hoy));
        for(String valor:List.of("2026-13","ayer","0000-01","2026-1"))
            assertThrows(IllegalArgumentException.class,()->CalendarioDisponibilidad.mes(valor,hoy));
    }
}
