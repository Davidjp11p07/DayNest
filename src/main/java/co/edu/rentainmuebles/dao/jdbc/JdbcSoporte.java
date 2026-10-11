package co.edu.rentainmuebles.dao.jdbc;

import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.dao.DaoException;
import java.sql.*;
import java.util.*;

abstract class JdbcSoporte<T> {
    private final Connection prestada;
    protected JdbcSoporte() { this.prestada = null; }
    protected JdbcSoporte(Connection conexion) { this.prestada = Objects.requireNonNull(conexion); }
    @FunctionalInterface protected interface Operacion<R> { R ejecutar(Connection c) throws SQLException; }
    protected final <R> R conexion(Operacion<R> operacion) {
        try {
            if (prestada != null) { return operacion.ejecutar(prestada); }
            try (Connection c = ConexionBD.abrir()) { return operacion.ejecutar(c); }
        } catch (SQLException | IllegalArgumentException e) { throw new DaoException("Error al acceder a los datos.",e); }
    }
    @FunctionalInterface interface Parametros { void aplicar(PreparedStatement s) throws SQLException; }
    protected final <R> R transaccion(Operacion<R> op) {
        return conexion(c->{boolean propia=c.getAutoCommit();if(propia)c.setAutoCommit(false);
            try{R resultado=op.ejecutar(c);if(propia)c.commit();return resultado;}
            catch(SQLException|RuntimeException|Error e){if(propia)c.rollback();throw e;}
            finally{if(propia)c.setAutoCommit(true);}});
    }
    protected abstract T mapear(Connection c, ResultSet r) throws SQLException;
    protected List<T> consultar(String sql, Parametros parametros) {
        return conexion(c -> consultar(c, sql, parametros));
    }
    final List<T> consultar(Connection c, String sql, Parametros parametros) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            parametros.aplicar(s);
            try (ResultSet r = s.executeQuery()) {
                List<T> datos = new ArrayList<>();
                while (r.next()) { datos.add(mapear(c, r)); }
                return datos;
            }
        }
    }
    protected Optional<T> uno(String sql, Parametros parametros) {
        List<T> datos = consultar(sql, parametros);
        if (datos.size() > 1) { throw new DaoException("La consulta esperaba un único resultado."); }
        return datos.stream().findFirst();
    }
    protected T guardar(String sql, Parametros parametros) {
        return uno(sql, parametros).orElseThrow(() -> new DaoException("La escritura no devolvió la entidad."));
    }
    protected void modificar(String sql, Parametros parametros) {
        conexion(c -> {
          try (PreparedStatement s = c.prepareStatement(sql)) {
            parametros.aplicar(s);
            if (s.executeUpdate() != 1) { throw new DaoException("La entidad no existe o no pudo modificarse."); }
          }
          return null;
        });
    }
    protected static long id(Long id) {
        if (id == null || id <= 0) { throw new IllegalArgumentException("Se requiere un identificador persistido."); }
        return id;
    }
    protected static void nueva(Long id) {
        if (id != null) { throw new IllegalArgumentException("La entidad nueva no debe tener identificador."); }
    }
}
