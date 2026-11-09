package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.verification.CompletarInterioresDemo;
import java.nio.file.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContenidoInmuebleTest extends SoportePostgres {
    private final ContenidoInmuebleService contenido = new ContenidoInmuebleService();
    private byte[] imagen() throws Exception { return Files.readAllBytes(Path.of("src/main/webapp/assets/img/interior-01.jpg")); }
    private long foto() throws Exception { return contenido.agregarFoto(propietario.getId(), inmueble.getId(), "INTERIOR", "image/jpeg", "Sala", imagen()); }
    private Reserva finalizada() {
        Reserva r = reservaPagada(); alFinalizar(r); new CicloReservas().ejecutar();
        assertEquals(EstadoReserva.FINALIZADA, reservas.buscarPorId(r.getId()).orElseThrow().getEstado());
        return r;
    }

    @Test void disponibilidadIncluyeReservasYBloqueosEnOrdenSinOcuparLaSalida() {
        Reserva reserva = reservaPagada();
        var bloqueos = new BloqueoService();
        bloqueos.crear(propietario.getId(), inmueble.getId(), HOY.plusDays(20), HOY.plusDays(22), "Motivo privado");
        var fechas = contenido.fechasNoDisponibles(inmueble.getId(), HOY);
        assertEquals(2, fechas.size());
        assertEquals(reserva.getPeriodo(), fechas.get(0));
        assertEquals(new PeriodoEstadia(HOY.plusDays(20), HOY.plusDays(22)), fechas.get(1));
        assertFalse(reservas.existeCruce(inmueble.getId(), new PeriodoEstadia(HOY.plusDays(13), HOY.plusDays(14)), null));
    }

    @Test void disponibilidadLiberaCancelacionesYBloqueosRetirados() {
        Reserva reserva = reservaPagada();
        var bloqueos = new BloqueoService();
        bloqueos.crear(propietario.getId(), inmueble.getId(), HOY.plusDays(20), HOY.plusDays(22), "Privado");
        servicios.reservas.cancelar(cliente.getId(), reserva.getId());
        bloqueos.liberar(propietario.getId(), bloqueos.listar(propietario.getId()).get(0).ocupacion().id());
        assertTrue(contenido.fechasNoDisponibles(inmueble.getId(), HOY).isEmpty());
    }

    @Test void disponibilidadNoPublicaInmueblesInactivosNiRangosTerminados() {
        reservaPagada();
        assertTrue(contenido.fechasNoDisponibles(inmueble.getId(), HOY.plusDays(13)).isEmpty());
        servicios.inmuebles.desactivar(propietario.getId(), inmueble.getId());
        assertTrue(contenido.fechasNoDisponibles(inmueble.getId(), HOY).isEmpty());
    }

    @Test void calendarioPropioConservaOcupacionesDelInactivoSinMostrarLasAUnAjeno() {
        reservaPagada();
        assertEquals("RESERVA",contenido.ocupacionesCalendario(inmueble.getId(),propietario.getId(),HOY).get(0).tipo());
        servicios.inmuebles.desactivar(propietario.getId(),inmueble.getId());
        assertTrue(contenido.ocupacionesCalendario(inmueble.getId(),null,HOY).isEmpty());
        assertTrue(contenido.ocupacionesCalendario(inmueble.getId(),cliente.getId(),HOY).isEmpty());
        assertEquals(1,contenido.ocupacionesCalendario(inmueble.getId(),propietario.getId(),HOY).size());
    }

    @Test void fotoVisibleDelPublicadoYEliminableSoloPorSuPropietario() throws Exception {
        long id = foto();
        assertArrayEquals(imagen(), contenido.imagenPublica(id).orElseThrow().datos());
        assertEquals(1, contenido.fotosPublicas(inmueble.getId()).size());
        assertTrue(contenido.imagenPropia(cliente.getId(), id).isEmpty());
        assertThrows(NegocioException.class, () -> contenido.eliminarFoto(cliente.getId(), id));
        contenido.eliminarFoto(propietario.getId(), id);
        assertTrue(contenido.imagenPublica(id).isEmpty());
    }

    @Test void fotosDeInmuebleDesactivadoNoSonPublicas() throws Exception {
        long id = foto(); servicios.inmuebles.desactivar(propietario.getId(), inmueble.getId());
        assertTrue(contenido.imagenPublica(id).isEmpty());
        assertTrue(contenido.fotosPublicas(inmueble.getId()).isEmpty());
        assertTrue(contenido.imagenPropia(propietario.getId(), id).isPresent());
    }

    @Test void propietarioAjenoNoPuedeSubirYLimiteDiezNoPierdeFotos() throws Exception {
        byte[] datos = imagen();
        assertThrows(NegocioException.class, () -> contenido.agregarFoto(cliente.getId(), inmueble.getId(), "INTERIOR", "image/jpeg", "", datos));
        for (int i = 0; i < 10; i++) foto();
        assertThrows(NegocioException.class, this::foto);
        assertEquals(10, contenido.fotosPropias(propietario.getId(), inmueble.getId()).size());
    }

    @Test void referenciasCompletanSinDuplicarNiReemplazarFotos() throws Exception {
        try (Connection c = ConexionBD.abrir()) {
            assertEquals(3, CompletarInterioresDemo.completar(c));
            assertEquals(0, CompletarInterioresDemo.completar(c));
        }
        assertEquals(3, contenido.fotosPublicas(inmueble.getId()).size());
        assertTrue(contenido.fotosPublicas(inmueble.getId()).stream().allMatch(f -> f.getTipo().equals("INTERIOR")));
    }

    @Test void referenciasRespetanInterioresSubidosPorPropietario() throws Exception {
        long id = foto();
        try (Connection c = ConexionBD.abrir()) { assertEquals(0, CompletarInterioresDemo.completar(c)); }
        assertEquals(1, contenido.fotosPublicas(inmueble.getId()).size());
        assertArrayEquals(imagen(), contenido.imagenPublica(id).orElseThrow().datos());
    }

    @Test void noCalificaEstanciaFuturaCanceladaOAjena() {
        Reserva r = reservaPagada();
        assertThrows(NegocioException.class, () -> contenido.calificar(cliente.getId(), "CLIENTE", r.getId(), 5, ""));
        servicios.reservas.cancelar(cliente.getId(), r.getId());
        assertThrows(NegocioException.class, () -> contenido.calificar(cliente.getId(), "CLIENTE", r.getId(), 5, ""));
        assertTrue(contenido.calificacionesInmueble(inmueble.getId()).isEmpty());
    }

    @Test void cadaParteCalificaUnaVezYSoloSuReservaFinalizada() {
        Reserva r = finalizada();
        assertThrows(NegocioException.class, () -> contenido.calificar(otroCliente.getId(), "CLIENTE", r.getId(), 5, ""));
        contenido.calificar(cliente.getId(), "CLIENTE", r.getId(), 5, "Excelente interior");
        contenido.calificar(propietario.getId(), "PROPIETARIO", r.getId(), 4, "Buen huésped");
        assertTrue(contenido.yaCalifico(cliente.getId(), "CLIENTE", r.getId()));
        assertThrows(NegocioException.class, () -> contenido.calificar(cliente.getId(), "CLIENTE", r.getId(), 1, "Duplicada"));
        assertEquals(5, contenido.calificacionesInmueble(inmueble.getId()).get(0).getPuntuacion());
        assertEquals(4, contenido.calificacionesDelCliente(cliente.getId()).get(0).getPuntuacion());
    }

    @Test void puntuacionYLongitudesInvalidasProducenErrorRecuperable() throws Exception {
        Reserva r = finalizada();
        for (int puntos : new int[]{0, 6})
            assertThrows(NegocioException.class, () -> contenido.calificar(cliente.getId(), "CLIENTE", r.getId(), puntos, ""));
        assertThrows(NegocioException.class, () -> contenido.calificar(cliente.getId(), "CLIENTE", r.getId(), 5, "x".repeat(1001)));
        byte[] datos = imagen();
        assertThrows(NegocioException.class, () -> contenido.agregarFoto(propietario.getId(), inmueble.getId(), "INTERIOR", "image/jpeg", "x".repeat(161), datos));
        assertTrue(contenido.calificacionesInmueble(inmueble.getId()).isEmpty());
    }
}
