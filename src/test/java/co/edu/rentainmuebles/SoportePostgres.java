package co.edu.rentainmuebles;

import co.edu.rentainmuebles.config.*;
import co.edu.rentainmuebles.dao.*;
import co.edu.rentainmuebles.dao.jdbc.*;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.impl.PoliticaComisionPorcentaje;
import co.edu.rentainmuebles.util.PasswordUtil;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.UUID;

/** Cada clase prueba en un esquema propio. Nunca trunca tablas de la aplicación. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SoportePostgres {
    protected static final ZoneId ZONA = ZoneId.of("America/Bogota");
    protected static final LocalDate HOY = LocalDate.now(ZONA);
    protected static final Clock RELOJ = Clock.system(ZONA);
    protected final UsuarioDao usuarios = new UsuarioDaoJdbc();
    protected final InmuebleDao inmuebles = new InmuebleDaoJdbc();
    protected final ReservaDao reservas = new ReservaDaoJdbc();
    protected final PagoDao pagos = new PagoDaoJdbc();
    protected final GiroDao giros = new GiroDaoJdbc();
    protected final UnidadTrabajoDao unidad = new UnidadTrabajoDaoJdbc();
    protected Servicios servicios;
    protected Usuario admin;
    protected Propietario propietario;
    protected Cliente cliente,otroCliente;
    protected Inmueble inmueble;
    private String esquema,anterior,hash;

    @BeforeAll final void crearEsquema() throws Exception {
        anterior = System.getProperty("renta.db.schema");
        if(anterior != null) throw new IllegalStateException("No establezca renta.db.schema al ejecutar JUnit.");
        esquema="renta_test_"+UUID.randomUUID().toString().replace("-","");
        try(Connection c=ConexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                ejecutar(c,"CREATE SCHEMA \""+esquema+"\"");
                ejecutar(c,"SET search_path TO \""+esquema+"\",public");
                ejecutar(c,Files.readString(Path.of("schema.sql")).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$",""));
                c.commit();
            } catch(Exception e) { c.rollback(); esquema=null; throw e; }
        }
        System.setProperty("renta.db.schema",esquema);
        hash=PasswordUtil.hash("PruebaJUnit2026!".toCharArray());
    }
    @BeforeEach final void datosLimpios() throws Exception {
        try(Connection c=ConexionBD.abrir()) {
            if(!esquema.equals(c.getSchema())) throw new IllegalStateException("Se rechazó el reinicio fuera del esquema de pruebas.");
            ejecutar(c,"TRUNCATE giro,movimiento_pago,bloqueo,reserva,ocupacion,inmueble,administrador,cliente,propietario,usuario,pais RESTART IDENTITY CASCADE");
        }
        try(Connection c=ConexionBD.abrir()){ejecutar(c,Files.readString(Path.of("geografia.sql")));}
        admin=usuarios.crear(new Usuario(null,"Administradora prueba","admin@junit.test",hash,null,true,true,false,false));
        propietario=usuarios.crear(new Usuario(null,"Propietario prueba","propietario@junit.test",hash,null,true,false,false,true)).getPropietario();
        cliente=usuarios.crear(new Usuario(null,"Cliente prueba","cliente@junit.test",hash,null,true,false,true,false)).getCliente();
        otroCliente=usuarios.crear(new Usuario(null,"Otro cliente","otro@junit.test",hash,null,true,false,true,false)).getCliente();
        inmueble=inmuebles.crear(new Inmueble(null,propietario,"Casa de prueba","Bogotá","Calle 10","Descripción de prueba",4,new BigDecimal("150000"),EstadoInmueble.PUBLICADO));
        servicios=conReloj(RELOJ);
    }
    protected void vencerReservaParaPrueba(long id) throws Exception {
        try(Connection c=ConexionBD.abrir()) {
            if(!c.getSchema().matches("renta_test_[a-f0-9]{32}")) throw new IllegalStateException("Fixture fuera de pruebas");
            c.setAutoCommit(false);
            try(Statement t=c.createStatement()) {
                t.execute("ALTER TABLE reserva DISABLE TRIGGER tg_validar_reserva");
                try(PreparedStatement q=c.prepareStatement("UPDATE reserva SET creada_en=clock_timestamp()-interval '31 minutes',expira_en=clock_timestamp()-interval '1 minute' WHERE id_ocupacion=?")){q.setLong(1,id);q.executeUpdate();}
                t.execute("SET CONSTRAINTS ALL IMMEDIATE");
                t.execute("ALTER TABLE reserva ENABLE TRIGGER tg_validar_reserva");c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    protected Servicios conReloj(Clock reloj) { return new Servicios(unidad,reloj,new PoliticaComisionPorcentaje(BigDecimal.ZERO)); }
    protected Reserva nuevaReserva(LocalDate entrada,LocalDate salida) { return servicios.reservas.crear(cliente.getId(),inmueble.getId(),entrada,salida,2); }
    protected Reserva reservaFutura() { return nuevaReserva(HOY.plusDays(10),HOY.plusDays(13)); }
    protected Reserva reservaPagada() {
        Reserva r=reservaFutura();
        return servicios.pagos.registrar(cliente.getId(),r.getId(),MetodoPago.TARJETA,"PAGO-JUNIT").getReserva();
    }
    protected Servicios alFinalizar(Reserva r) {
        // Fixture histórico SOLO en esquema UUID de prueba, con igual duración y cobro.
        try(Connection c=ConexionBD.abrir()){
            if(!c.getSchema().startsWith("renta_test_"))throw new IllegalStateException("Fixture fuera de pruebas");
            c.setAutoCommit(false);
            try(Statement s=c.createStatement()){
                s.execute("ALTER TABLE ocupacion DISABLE TRIGGER tg_ocupacion_inmutable");
                s.execute("UPDATE ocupacion SET fecha_inicio=CURRENT_DATE-"+r.getPeriodo().getNoches()+",fecha_fin=CURRENT_DATE WHERE id_ocupacion="+r.getId());
                s.execute("SET CONSTRAINTS ALL IMMEDIATE");
                s.execute("ALTER TABLE ocupacion ENABLE TRIGGER tg_ocupacion_inmutable");c.commit();
            }
        }catch(Exception e){throw new RuntimeException(e);}return servicios;
    }
    @AfterAll final void borrarEsquema() throws Exception {
        System.clearProperty("renta.db.schema");
        if(esquema!=null) {
            try(Connection c=ConexionBD.abrir()) { ejecutar(c,"DROP SCHEMA \""+esquema+"\" CASCADE"); }
        }
        if(anterior!=null) System.setProperty("renta.db.schema",anterior);
    }
    private static void ejecutar(Connection c,String sql) throws SQLException {
        // Únicamente scripts locales o identificadores UUID generados por esta clase.
        try(PreparedStatement s=c.prepareStatement(sql)) { s.execute(); }
    }
}
