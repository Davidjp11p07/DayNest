package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.verification.MigrarModeloSvg;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MigracionSvgTest extends SoportePostgres {
    @Override protected String esquemaSql(){return "src/test/resources/schema-pre-profesor.sql";}
    private static void script(Connection c,String ruta) throws Exception {
        String sql=Files.readString(Path.of(ruta)).replaceAll("(?im)^\\s*(BEGIN|COMMIT);\\s*$","");
        c.setAutoCommit(false);
        try(Statement s=c.createStatement()){s.execute(sql);c.commit();}
        catch(Exception e){c.rollback();throw e;}
        finally{c.setAutoCommit(true);}
    }
    @Test void conservaCobroGiroEjecutadoBeneficiarioYReversion() throws Exception {
        Reserva r=reservaPagada();alFinalizar(r);
        Giro g=servicios.giros.generar(admin.getId(),r.getId(),"SIM-SVG");
        try(Connection c=ConexionBD.abrir()) {
            var antes=MigrarModeloSvg.huellas(c);
            script(c,"revertir-svg.sql");
            assertEquals(antes,MigrarModeloSvg.huellas(c));
            MigrarModeloSvg.migrar(c);MigrarModeloSvg.migrar(c);
            assertEquals(antes,MigrarModeloSvg.huellas(c));
        }
        Giro leido=giros.buscarPorId(g.getId()).orElseThrow();
        assertEquals(g.getMonto(),leido.getMonto());assertEquals(propietario.getId(),leido.getPropietario().getId());
        assertEquals("COP",leido.getMoneda());assertEquals(EstadoGiro.EJECUTADO,leido.getEstado());
    }
    @Test void migracionDesdeDDLAnteriorEquivaleAInstalacionNueva() throws Exception {
        try(Connection c=ConexionBD.abrir()) {
            var esperado=estructura(c);String esquema="renta_test_"+UUID.randomUUID().toString().replace("-","");
            try(Statement s=c.createStatement()) {
                s.execute("CREATE SCHEMA "+esquema);s.execute("SET search_path TO "+esquema+",public");
                script(c,"src/test/resources/schema-anterior-svg.sql");
                MigrarModeloSvg.migrar(c);co.edu.rentainmuebles.verification.MigrarFiltros.migrar(c);script(c,"src/test/resources/compatibilidad-pre-profesor.sql");assertEquals(esperado,estructura(c));
            } finally {
                try(Statement s=c.createStatement()){s.execute("SET search_path TO public");s.execute("DROP SCHEMA "+esquema+" CASCADE");}
            }
        }
    }
    private static Set<String> estructura(Connection c) throws Exception {
        Set<String> out=new TreeSet<>();
        for(String sql:List.of(
            "SELECT table_name,column_name,data_type,is_nullable FROM information_schema.columns WHERE table_schema=current_schema()",
            "SELECT t.relname,x.conname,pg_get_constraintdef(x.oid),x.convalidated FROM pg_constraint x JOIN pg_class t ON t.oid=x.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace WHERE n.nspname=current_schema()",
            "SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname=current_schema()")) {
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) {
                while(r.next()){StringJoiner j=new StringJoiner("|");for(int i=1;i<=r.getMetaData().getColumnCount();i++)j.add(r.getString(i));out.add(j.toString().replace(c.getSchema()+".",""));}
            }
        }
        return out;
    }
    @Test void monedaInconsistenteRechazaMigracionSinCambiarFilas() throws Exception {
        reservaPagada();
        try(Connection c=ConexionBD.abrir()) {
            script(c,"revertir-svg.sql");
            try(Statement s=c.createStatement()) {
                c.setAutoCommit(false);s.execute("ALTER TABLE movimiento_pago DISABLE TRIGGER tg_validar_movimiento");
                s.execute("UPDATE movimiento_pago SET moneda='EUR'");s.execute("ALTER TABLE movimiento_pago ENABLE TRIGGER tg_validar_movimiento");c.commit();c.setAutoCommit(true);
            }
            var antes=MigrarModeloSvg.huellas(c);
            assertThrows(SQLException.class,()->MigrarModeloSvg.migrar(c));assertEquals(antes,MigrarModeloSvg.huellas(c));
            // Reparar únicamente el fixture aislado y restablecer el esquema para el siguiente caso.
            try(Statement s=c.createStatement()){c.setAutoCommit(false);s.execute("ALTER TABLE movimiento_pago DISABLE TRIGGER tg_validar_movimiento");s.execute("UPDATE movimiento_pago SET moneda='COP'");s.execute("ALTER TABLE movimiento_pago ENABLE TRIGGER tg_validar_movimiento");c.commit();c.setAutoCommit(true);}
            MigrarModeloSvg.migrar(c);
        }
    }
    @Test void correoNoCanonicoSeRechazaSinSobrescribir() throws Exception {
        try(Connection c=ConexionBD.abrir()) {
            script(c,"revertir-svg.sql");
            try(Statement s=c.createStatement()){s.execute("UPDATE usuario SET correo=upper(correo) WHERE id_usuario="+cliente.getId());}
            var antes=MigrarModeloSvg.huellas(c);assertThrows(SQLException.class,()->MigrarModeloSvg.migrar(c));assertEquals(antes,MigrarModeloSvg.huellas(c));
            try(Statement s=c.createStatement()){s.execute("UPDATE usuario SET correo=lower(correo)");}MigrarModeloSvg.migrar(c);
        }
    }
    @Test void mismaFechaTrasCancelacionTieneIdentidadFinancieraDistinta() {
        Reserva anterior=reservaPagada();servicios.reservas.cancelar(cliente.getId(),anterior.getId());
        Reserva nueva=reservaFutura();assertNotEquals(anterior.getId(),nueva.getId());
        assertEquals(anterior.getPeriodo(),nueva.getPeriodo());assertEquals(2,pagos.listarPorReserva(anterior.getId()).size());
        assertTrue(pagos.listarPorReserva(nueva.getId()).isEmpty());
    }
    @Test void monedaHistoricaNoDependeDeTarifaMonedaActual() throws Exception {
        Reserva r=reservaPagada();
        try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){s.execute("UPDATE inmueble_operativo SET moneda='EUR',tarifa_noche=95 WHERE id_inmueble="+inmueble.getId());}
        assertEquals("COP",pagos.listarPorReserva(r.getId()).get(0).getMoneda());
        assertEquals(r.getPrecioPorNoche(),reservas.buscarPorId(r.getId()).orElseThrow().getPrecioPorNoche());
    }
    @Test void correoCanonicoExigidoEnSQLYPerfilesOpcionales() throws Exception {
        try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()) {
            assertThrows(SQLException.class,()->s.execute("UPDATE usuario SET correo_normalizado=upper(correo_normalizado) WHERE id_usuario="+cliente.getId()));
        }
        Usuario u=usuarios.crear(new Usuario(null,"Sin perfiles","sinperfil@junit.test",cliente.getHashContrasena(),null,true,false,false,false));
        assertTrue(usuarios.buscarPorId(u.getId()).orElseThrow().getPerfiles().isEmpty());
    }
}
