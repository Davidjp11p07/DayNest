package co.edu.rentainmuebles.dao.jdbc;

import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.dao.DaoException;
import java.sql.*;

final class Mapeador {
    private Mapeador() { }
    static Usuario usuario(Connection c, ResultSet r) throws SQLException {
        long id = r.getLong("id_usuario");
        boolean cliente, propietario, administrador;
        try (PreparedStatement s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM cliente WHERE id_usuario=?), EXISTS(SELECT 1 FROM propietario WHERE id_usuario=?), EXISTS(SELECT 1 FROM administrador WHERE id_usuario=?)")) {
            s.setLong(1,id); s.setLong(2,id); s.setLong(3,id);
            try (ResultSet perfiles=s.executeQuery()) { perfiles.next(); cliente=perfiles.getBoolean(1); propietario=perfiles.getBoolean(2); administrador=perfiles.getBoolean(3); }
        }
        return new Usuario(id,r.getString("nombre_completo"),r.getString("correo_normalizado"),r.getString("contrasena_hash"),
                r.getString("telefono"),r.getBoolean("activo"),administrador,cliente,propietario);
    }
    static Usuario usuario(Connection c, long id) throws SQLException {
        return new UsuarioDaoJdbc().consultar(c, "SELECT * FROM usuario WHERE id_usuario = ?", s -> s.setLong(1, id))
                .stream().findFirst().orElseThrow(() -> new DaoException("Usuario relacionado inexistente."));
    }
    static Inmueble inmueble(Connection c, long id) throws SQLException {
        return new InmuebleDaoJdbc().consultar(c, "SELECT * FROM inmueble_operativo WHERE id_inmueble = ?", s -> s.setLong(1, id))
                .stream().findFirst().orElseThrow(() -> new DaoException("Inmueble relacionado inexistente."));
    }
    static Reserva reserva(Connection c, long id) throws SQLException {
        return new ReservaDaoJdbc().consultar(c, ReservaDaoJdbc.SELECT+" WHERE r.id_ocupacion = ?", s -> s.setLong(1, id))
                .stream().findFirst().orElseThrow(() -> new DaoException("Reserva relacionada inexistente."));
    }
}
