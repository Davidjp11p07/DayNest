package co.edu.rentainmuebles.config;

import co.edu.rentainmuebles.dao.UnidadTrabajoDao;
import co.edu.rentainmuebles.dao.jdbc.UnidadTrabajoDaoJdbc;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.service.impl.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Properties;

/** Composición de los servicios principales mediante unidad de trabajo transaccional. */
public final class Servicios {
    public final CatalogoService catalogo=new CatalogoService(new co.edu.rentainmuebles.dao.jdbc.CiudadDaoJdbc());
    private final Clock reloj;
    public LocalDate hoy() { return LocalDate.now(reloj); }
    public final AutenticacionService autenticacion;
    public final InmuebleService inmuebles;
    public final ReservaService reservas;
    public final PagoService pagos;
    public final GiroService giros;
    public final AdministracionService administracion;
    public Servicios(UnidadTrabajoDao trabajo, Clock reloj, PoliticaComision politica) {
        this.reloj = reloj;
        autenticacion = new AutenticacionServiceImpl(trabajo,reloj);
        inmuebles = new InmuebleServiceImpl(trabajo,reloj);
        reservas = new ReservaServiceImpl(trabajo,reloj);
        pagos = new PagoServiceImpl(trabajo,reloj);
        giros = new GiroServiceImpl(trabajo,reloj,politica);
        administracion = new AdministracionServiceImpl(trabajo,reloj,giros);
    }
    public static Servicios crear() {
        Properties p = new Properties();
        try (InputStream entrada = Servicios.class.getClassLoader().getResourceAsStream("negocio.properties")) {
            if (entrada == null) { throw new NegocioException("No se encontró negocio.properties en el classpath."); }
            p.load(entrada);
            return crear(p);
        } catch (IOException | IllegalArgumentException | NullPointerException | DateTimeException e) {
            throw new NegocioException("Revise la comisión y la zona horaria de negocio.properties.",e);
        }
    }
    static Servicios crear(Properties p) {
        return new Servicios(new UnidadTrabajoDaoJdbc(),Clock.system(ZoneId.of(p.getProperty("negocio.zonaHoraria"))),
                new PoliticaComisionPorcentaje(new BigDecimal(p.getProperty("negocio.comision"))));
    }

}
