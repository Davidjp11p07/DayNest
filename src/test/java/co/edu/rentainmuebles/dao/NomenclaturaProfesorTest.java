package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.verification.MigrarNomenclatura;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NomenclaturaProfesorTest extends SoportePostgres {
 @Test void claveCompuestaIncluyeFechaEIntento()throws Exception {
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT a.attname FROM pg_constraint k CROSS JOIN LATERAL unnest(k.conkey) WITH ORDINALITY AS x(attnum,orden) JOIN pg_attribute a ON a.attrelid=k.conrelid AND a.attnum=x.attnum WHERE k.contype='p' AND k.conrelid='renta'::regclass ORDER BY x.orden")){
   List<String> columnas=new ArrayList<>();while(r.next())columnas.add(r.getString(1));assertEquals(List.of("id_cl","id_i","fch_e","num_intento"),columnas);
  }
 }
 @Test void cancelarYReservarMismasFechasConservaAmbosIntentos()throws Exception {
  var primera=reservaFutura();servicios.reservas.cancelar(cliente.getId(),primera.getId());
  var segunda=nuevaReserva(primera.getPeriodo().getEntrada(),primera.getPeriodo().getSalida());
  assertNotEquals(primera.getId(),segunda.getId());
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT num_intento,estado FROM renta ORDER BY num_intento")){
   assertTrue(r.next());assertEquals(1,r.getLong(1));assertEquals("CANCELADA",r.getString(2));
   assertTrue(r.next());assertEquals(2,r.getLong(1));assertEquals("PENDIENTE_PAGO",r.getString(2));assertFalse(r.next());
  }
 }
 @Test void intentoYClaveNoSePuedenReescribir()throws Exception {
  var r=reservaFutura();
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement()){
   assertThrows(SQLException.class,()->s.executeUpdate("UPDATE renta SET num_intento=5 WHERE id_ocupacion="+r.getId()));
   assertThrows(SQLException.class,()->s.executeUpdate("UPDATE renta SET fch_e=fch_e+1 WHERE id_ocupacion="+r.getId()));
  }
 }
 @Test void identificadorTecnicoSigueReferenciandoCobros()throws Exception {
  var r=reservaPagada();
  try(Connection c=ConexionBD.abrir();Statement s=c.createStatement();ResultSet x=s.executeQuery("SELECT r.id_ocupacion,m.id_reserva FROM renta r JOIN movimiento_pago m ON m.id_reserva=r.id_ocupacion")){
   assertTrue(x.next());assertEquals(r.getId(),x.getLong(1));assertEquals(r.getId(),x.getLong(2));
  }
 }
 @Test void migracionRepetidaNoModificaElEsquemaActual()throws Exception {
  var r=reservaPagada();try(Connection c=ConexionBD.abrir()){MigrarNomenclatura.migrar(c);MigrarNomenclatura.migrar(c);}
  assertEquals(r.getId(),reservas.buscarPorId(r.getId()).orElseThrow().getId());
 }
}
