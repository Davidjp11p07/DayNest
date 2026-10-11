package co.edu.rentainmuebles.dao;

/** Error de persistencia; conserva la causa JDBC y su SQLState. */
public class DaoException extends RuntimeException {
    public DaoException(String mensaje, Throwable causa) { super(mensaje, causa); }
    public DaoException(String mensaje) { super(mensaje); }
    /** Evita que los servicios necesiten conocer SQLException. */
    public String estadoSql() {
        for (Throwable causa = getCause(); causa != null; causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLException sql) { return sql.getSQLState(); }
        }
        return null;
    }
}
