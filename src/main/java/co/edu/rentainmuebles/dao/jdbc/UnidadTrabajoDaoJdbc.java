package co.edu.rentainmuebles.dao.jdbc;

import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.dao.*;
import java.sql.*;
import java.util.Objects;
import java.util.function.Function;

/** El contexto y los DAO prestados solo son válidos durante ejecutar(). */
public final class UnidadTrabajoDaoJdbc implements UnidadTrabajoDao {
    @Override public <T> T ejecutar(Function<Daos, T> operacion) {
        Objects.requireNonNull(operacion);
        try (Connection c = ConexionBD.abrir()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            Daos contexto = new Contexto(new UsuarioDaoJdbc(c),new InmuebleDaoJdbc(c),
                    new ReservaDaoJdbc(c),new PagoDaoJdbc(c),new GiroDaoJdbc(c));
            try {
                T resultado = operacion.apply(contexto);
                c.commit();
                return resultado;
            } catch (RuntimeException | Error | SQLException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        } catch (SQLException e) { throw new DaoException("No se pudo completar la transacción.",e); }
    }
    private record Contexto(UsuarioDao usuarios, InmuebleDao inmuebles, ReservaDao reservas,
                            PagoDao pagos, GiroDao giros) implements Daos { }
}
