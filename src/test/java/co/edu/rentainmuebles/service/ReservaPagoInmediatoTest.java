package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.config.ConexionBD;
import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class ReservaPagoInmediatoTest extends SoportePostgres {
    private Reserva crear(CuentaBancaria cuenta) {
        return servicios.reservas.crearConPago(cliente.getId(),inmueble.getId(),HOY.plusDays(10),HOY.plusDays(13),2,null,cuenta);
    }
    @Test void crearCobraTotalYDevuelveConfirmadaSinPasoPosterior() {
        Reserva r=servicios.reservas.crear(cliente.getId(),inmueble.getId(),HOY.plusDays(10),HOY.plusDays(13),2);
        assertEquals(EstadoReserva.CONFIRMADA,r.getEstado());
        assertEquals(0,r.getTotal().compareTo(pagos.totalPagado(r.getId())));
        assertEquals(1,pagos.listarPorReserva(r.getId()).size());
        assertEquals(EstadoPago.APROBADO,pagos.buscarPorReserva(r.getId()).orElseThrow().getEstado());
        assertTrue(reservas.comisionHistorica(r.getId()).isPresent());
        assertThrows(NegocioException.class,()->servicios.pagos.registrarTransferencia(cliente.getId(),r.getId(),null,null));
        assertEquals(1,pagos.listarPorReserva(r.getId()).size());
    }
    @Test void cuentaOcasionalSinRegistradaCobraSinModificarPerfil() throws Exception {
        try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute("DELETE FROM vincula");}
        long banco=new DatosCuentaService().bancos().get(0).id();
        Reserva r=crear(new CuentaBancaria(banco,"000011112222","AHORROS"));
        assertEquals(EstadoReserva.CONFIRMADA,r.getEstado());
        assertNull(new DatosCuentaService().consultar(cliente.getId(),Rol.CLIENTE).cuenta());
        assertEquals("OCASIONAL",pagos.buscarPorReserva(r.getId()).orElseThrow().getUsoCuenta());
    }
    @Test void faltaDeCuentaRevierteReservaYOcupacionYPermiteReintentar() throws Exception {
        try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute("DELETE FROM vincula");}
        assertThrows(NegocioException.class,()->crear(null));
        assertTrue(reservas.listar().isEmpty());assertTrue(pagos.listar().isEmpty());
        assertFalse(reservas.existeCruce(inmueble.getId(),new PeriodoEstadia(HOY.plusDays(10),HOY.plusDays(13)),null));
        long banco=new DatosCuentaService().bancos().get(0).id();
        assertEquals(EstadoReserva.CONFIRMADA,crear(new CuentaBancaria(banco,"12345678","CORRIENTE")).getEstado());
    }
    @Test void falloSqlDuranteCobroRevierteTodaLaOperacion() {
        assertThrows(NegocioException.class,()->crear(new CuentaBancaria(99999999,"12345678","AHORROS")));
        assertTrue(reservas.listar().isEmpty());assertTrue(pagos.listar().isEmpty());
        assertEquals(EstadoReserva.CONFIRMADA,crear(null).getEstado());
    }
    @Test void segundoIntentoNoDuplicaReservaNiCobroYCancelacionDevuelve() {
        Reserva r=crear(null);
        assertThrows(NegocioException.class,()->crear(null));
        assertEquals(1,reservas.listar().size());assertEquals(1,pagos.listar().size());
        servicios.reservas.cancelar(cliente.getId(),r.getId());
        assertEquals(0,pagos.totalPagado(r.getId()).signum());
        assertEquals(EstadoReserva.CONFIRMADA,crear(null).getEstado());
    }
}
