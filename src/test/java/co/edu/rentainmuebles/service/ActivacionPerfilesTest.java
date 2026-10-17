package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ActivacionPerfilesTest extends SoportePostgres {
    @Test void clienteActivaPropietarioYConservaCuentaReservaYPago() {
        Reserva previa=reservaPagada();
        Usuario antes=usuarios.buscarPorId(cliente.getId()).orElseThrow();
        Usuario despues=servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO);
        mismaIdentidad(antes,despues);
        assertTrue(despues.isCliente()); assertTrue(despues.isPropietario()); assertFalse(despues.isEsAdministrador());
        assertEquals(previa.getId(),servicios.reservas.listarPropias(cliente.getId()).get(0).getId());
        assertEquals(EstadoReserva.CONFIRMADA,reservas.buscarPorId(previa.getId()).orElseThrow().getEstado());
        assertEquals(0,previa.getTotal().compareTo(pagos.totalPagado(previa.getId())));
        assertEquals(1,pagos.listarPorReserva(previa.getId()).size());
        Inmueble nuevo=servicios.inmuebles.publicar(despues.getId(),"Nuevo alojamiento","Cali","Calle 2","Descripción",2,new BigDecimal("120000"));
        assertEquals(despues.getId(),nuevo.getPropietario().getId());
        // El perfil cliente también sigue operativo tras adquirir el nuevo perfil.
        servicios.reservas.cancelar(despues.getId(),previa.getId());
        assertEquals(EstadoReserva.CANCELADA,reservas.buscarPorId(previa.getId()).orElseThrow().getEstado());
    }

    @Test void propietarioActivaClienteYConservaPublicacionReservaYPagoYGiro() {
        Reserva previa=reservaPagada(); alFinalizar(previa);
        Giro giro=servicios.giros.generar(admin.getId(),previa.getId(),"GIRO-PERFILES");
        Usuario antes=usuarios.buscarPorId(propietario.getId()).orElseThrow();
        Usuario despues=servicios.autenticacion.activarPerfil(propietario.getId(),Rol.CLIENTE);
        mismaIdentidad(antes,despues);
        assertTrue(despues.isCliente()); assertTrue(despues.isPropietario()); assertFalse(despues.isEsAdministrador());
        assertEquals(inmueble.getId(),servicios.inmuebles.listarPropios(despues.getId()).get(0).getId());
        assertEquals(previa.getId(),servicios.reservas.listarRecibidas(despues.getId()).get(0).getId());
        assertEquals(giro.getId(),servicios.giros.listarPropios(despues.getId()).get(0).getId());
        assertEquals(0,giro.getMonto().compareTo(giros.buscarPorId(giro.getId()).orElseThrow().getMonto()));
        assertEquals(1,pagos.listarPorReserva(previa.getId()).size());
        var datos=new DatosCuentaService();datos.guardar(despues.getId(),Rol.CLIENTE,datos.consultar(despues.getId(),Rol.PROPIETARIO).cuenta(),false);
        Reserva nueva=servicios.reservas.crear(despues.getId(),inmueble.getId(),HOY.plusDays(20),HOY.plusDays(22),1);
        assertEquals(despues.getId(),nueva.getCliente().getId());
        assertEquals(EstadoReserva.CONFIRMADA,servicios.pagos.registrar(despues.getId(),nueva.getId(),MetodoPago.TARJETA,null).getReserva().getEstado());
    }

    @Test void activacionRepetidaEnAmbosSentidosNoDuplicaPerfiles() throws Exception {
        servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO);
        servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO);
        servicios.autenticacion.activarPerfil(cliente.getId(),Rol.CLIENTE);
        servicios.autenticacion.activarPerfil(propietario.getId(),Rol.CLIENTE);
        servicios.autenticacion.activarPerfil(propietario.getId(),Rol.CLIENTE);
        servicios.autenticacion.activarPerfil(propietario.getId(),Rol.PROPIETARIO);
        assertEquals(1,cuentaPerfil("cliente",cliente.getId()));
        assertEquals(1,cuentaPerfil("propietario",cliente.getId()));
        assertEquals(1,cuentaPerfil("cliente",propietario.getId()));
        assertEquals(1,cuentaPerfil("propietario",propietario.getId()));
        assertEquals(4,usuarios.listar().size());
    }

    @Test void preservaAdministradorExistenteYSusDatos() {
        Usuario previo=usuarios.crear(new Usuario(null,"Cuenta mixta","mixta@junit.test",admin.getHashContrasena(),"3001234567",true,true,true,false));
        Usuario despues=servicios.autenticacion.activarPerfil(previo.getId(),Rol.PROPIETARIO);
        mismaIdentidad(previo,despues);
        assertTrue(despues.isEsAdministrador()); assertTrue(despues.isCliente()); assertTrue(despues.isPropietario());
    }

    @Test void noPermiteAsignarAdministradorNiPerfilNuloNiCuentaSinRolComplementario() {
        assertThrows(NegocioException.class,()->servicios.autenticacion.activarPerfil(cliente.getId(),Rol.ADMINISTRADOR));
        assertThrows(NegocioException.class,()->servicios.autenticacion.activarPerfil(cliente.getId(),null));
        assertThrows(NegocioException.class,()->servicios.autenticacion.activarPerfil(admin.getId(),Rol.CLIENTE));
        assertThrows(NegocioException.class,()->servicios.autenticacion.activarPerfil(admin.getId(),Rol.PROPIETARIO));
        assertThrows(IllegalArgumentException.class,()->usuarios.activarPerfil(cliente.getId(),Rol.ADMINISTRADOR));
        assertFalse(usuarios.buscarPorId(cliente.getId()).orElseThrow().isEsAdministrador());
        assertFalse(usuarios.buscarPorId(admin.getId()).orElseThrow().isCliente());
    }

    @Test void cuentaInactivaOInexistenteNoAdquierePerfil() {
        usuarios.desactivar(cliente.getId());
        assertThrows(NegocioException.class,()->servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO));
        assertThrows(NegocioException.class,()->servicios.autenticacion.activarPerfil(999999,Rol.CLIENTE));
        assertFalse(usuarios.buscarPorId(cliente.getId()).orElseThrow().isPropietario());
    }

    @Test void falloPosteriorRevierteInsercionSinEliminarPerfilOriginal() {
        assertThrows(IllegalStateException.class,()->unidad.ejecutar(d -> {
            d.usuarios().activarPerfil(cliente.getId(),Rol.PROPIETARIO);
            throw new IllegalStateException("Fallo posterior dentro de la misma transacción");
        }));
        Usuario despues=usuarios.buscarPorId(cliente.getId()).orElseThrow();
        assertTrue(despues.isCliente()); assertFalse(despues.isPropietario());
    }

    @Test void activacionesConcurrentesNoDuplicanNiPierdenPerfiles() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch inicio=new CountDownLatch(1);
        try {
            Callable<Usuario> activar=()->{inicio.await();return servicios.autenticacion.activarPerfil(cliente.getId(),Rol.PROPIETARIO);};
            Future<Usuario> a=pool.submit(activar),b=pool.submit(activar); inicio.countDown();
            assertTrue(a.get(10,TimeUnit.SECONDS).isPropietario());
            assertTrue(b.get(10,TimeUnit.SECONDS).isCliente());
            assertEquals(1,cuentaPerfil("cliente",cliente.getId()));
            assertEquals(1,cuentaPerfil("propietario",cliente.getId()));
        } finally {pool.shutdownNow();}
    }

    private static void mismaIdentidad(Usuario antes,Usuario despues) {
        assertEquals(antes.getId(),despues.getId()); assertEquals(antes.getNombre(),despues.getNombre());
        assertEquals(antes.getCorreo(),despues.getCorreo()); assertEquals(antes.getTelefono(),despues.getTelefono());
        assertEquals(antes.getHashContrasena(),despues.getHashContrasena()); assertEquals(antes.isActivo(),despues.isActivo());
        assertEquals(antes.isEsAdministrador(),despues.isEsAdministrador());
    }
    private int cuentaPerfil(String tabla,long id) throws Exception {
        try (Connection c=ConexionBD.abrir();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM "+tabla+" WHERE id_usuario=?")) {
            s.setLong(1,id);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}
        }
    }
}
