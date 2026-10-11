package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.Servicios;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.impl.PoliticaComisionPorcentaje;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiciosTest extends SoportePostgres {
    @Test void registraYAutenticaConPbkdf2() {
        Usuario u=servicios.autenticacion.registrar("Nueva cliente"," NUEVA@JUNIT.TEST ","Segura2026!".toCharArray(),Rol.CLIENTE);
        assertEquals("nueva@junit.test",u.getCorreo()); assertTrue(u.getHashContrasena().startsWith("pbkdf2-sha256$"));
        assertTrue(servicios.autenticacion.login("nueva@junit.test","Segura2026!".toCharArray()).isCliente());
    }
    @ParameterizedTest @ValueSource(strings={"incorrecta","","otra clave"}) void loginIncorrecto(String password) {
        assertThrows(NegocioException.class,()->servicios.autenticacion.login(cliente.getCorreo(),password.toCharArray()));
    }
    @ParameterizedTest @ValueSource(strings={"mal-correo","x@y","","@dominio.test"}) void rechazaCorreoInvalido(String correo) {
        assertThrows(NegocioException.class,()->servicios.autenticacion.registrar("Nombre",correo,"Segura2026!".toCharArray(),Rol.CLIENTE));
    }
    @Test void registroNoPermiteAdministradorNiCorreoDuplicadoNiClaveCorta() {
        assertThrows(NegocioException.class,()->servicios.autenticacion.registrar("Nombre","x@junit.test","Segura2026!".toCharArray(),Rol.ADMINISTRADOR));
        assertThrows(NegocioException.class,()->servicios.autenticacion.registrar("Nombre",cliente.getCorreo(),"Segura2026!".toCharArray(),Rol.CLIENTE));
        assertThrows(NegocioException.class,()->servicios.autenticacion.registrar("Nombre","x@junit.test","corta".toCharArray(),Rol.CLIENTE));
    }
    @Test void usuarioInactivoNoOpera() {
        usuarios.desactivar(cliente.getId());
        assertThrows(NegocioException.class,()->servicios.autenticacion.login(cliente.getCorreo(),"PruebaJUnit2026!".toCharArray()));
        assertThrows(NegocioException.class,()->servicios.reservas.listarPropias(cliente.getId()));
    }
    @Test void noPermiteEditarNiDesactivarInmuebleAjeno() {
        Propietario otro=usuarios.crear(new Usuario(null,"Otro dueño","dueno@junit.test",propietario.getHashContrasena(),null,true,false,false,true)).getPropietario();
        assertThrows(NegocioException.class,()->servicios.inmuebles.editar(otro.getId(),inmueble.getId(),"X","Cali","Calle","Descripción",2,new BigDecimal("100")));
        assertThrows(NegocioException.class,()->servicios.inmuebles.desactivar(otro.getId(),inmueble.getId()));
    }
    @Test void precioYCapacidadDebenSerPositivos() {
        assertThrows(NegocioException.class,()->servicios.inmuebles.publicar(propietario.getId(),"X","Cali","Calle","Desc",0,new BigDecimal("100")));
        assertThrows(NegocioException.class,()->servicios.inmuebles.publicar(propietario.getId(),"X","Cali","Calle","Desc",2,BigDecimal.ZERO));
    }
    @Test void fechasInvalidasMismoDiaPasadoInvertidasYNulas() {
        assertThrows(NegocioException.class,()->nuevaReserva(HOY,HOY.plusDays(1)));
        assertThrows(NegocioException.class,()->nuevaReserva(HOY.minusDays(1),HOY.plusDays(1)));
        assertThrows(NegocioException.class,()->nuevaReserva(HOY.plusDays(2),HOY.plusDays(1)));
        assertThrows(NegocioException.class,()->nuevaReserva(null,HOY.plusDays(1)));
    }
    @Test void cancelacionPagadaDevuelveYLibera(){Reserva r=reservaPagada();servicios.reservas.cancelar(cliente.getId(),r.getId());assertEquals(EstadoReserva.CANCELADA,reservas.buscarPorId(r.getId()).orElseThrow().getEstado());assertEquals(0,pagos.totalPagado(r.getId()).signum());assertEquals(2,pagos.listarPorReserva(r.getId()).size());assertEquals("DEVOLUCION",pagos.listarPorReserva(r.getId()).get(1).getClase());assertNotNull(reservaFutura());}
    @Test void pagoAjenoYDuplicadoRechazados(){Reserva r=reservaFutura();assertThrows(NegocioException.class,()->servicios.pagos.registrar(otroCliente.getId(),r.getId(),MetodoPago.TARJETA,null));servicios.pagos.registrar(cliente.getId(),r.getId(),MetodoPago.TARJETA,null);assertThrows(NegocioException.class,()->servicios.pagos.registrar(cliente.getId(),r.getId(),MetodoPago.TARJETA,null));assertEquals(1,pagos.listar().size());}
    @Test void giroAntesDeSalidaOSinPermisoRechazado(){Reserva r=reservaPagada();assertThrows(NegocioException.class,()->servicios.giros.generar(admin.getId(),r.getId(),null));assertThrows(NegocioException.class,()->servicios.giros.generar(cliente.getId(),r.getId(),null));}
    @Test void giroNetoSinComisionDuplicadoRechazado(){Reserva r=reservaPagada();alFinalizar(r);Giro g=servicios.giros.generar(admin.getId(),r.getId(),"SIM-GIRO");assertEquals(r.getTotal(),g.getMonto());assertEquals(0,g.getMontoComision().signum());assertThrows(NegocioException.class,()->servicios.giros.generar(admin.getId(),r.getId(),null));}
}
