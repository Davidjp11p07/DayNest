package co.edu.rentainmuebles.dao.jdbc;

import co.edu.rentainmuebles.dao.UsuarioDao;
import co.edu.rentainmuebles.model.*;
import java.sql.*;
import java.util.*;

public final class UsuarioDaoJdbc extends JdbcSoporte<Usuario> implements UsuarioDao {
    public UsuarioDaoJdbc() { super(); }
    UsuarioDaoJdbc(Connection c) { super(c); }
    @Override protected Usuario mapear(Connection c, ResultSet r) throws SQLException { return Mapeador.usuario(c,r); }
    @Override public Optional<Usuario> buscarPorId(long id) { return uno("SELECT * FROM usuario WHERE id_usuario=?",s->s.setLong(1,id(id))); }
    @Override public Optional<Usuario> buscarPorCorreo(String correo) {
        Objects.requireNonNull(correo);
        return uno("SELECT * FROM usuario WHERE correo_normalizado=?",s->s.setString(1,correo.trim().toLowerCase(Locale.ROOT)));
    }
    @Override public List<Usuario> listar() { return consultar("SELECT * FROM usuario ORDER BY id_usuario",s->{}); }
    @Override public List<Usuario> buscarPorRol(Rol rol) {
        Objects.requireNonNull(rol);
        String condicion=switch(rol) {
            case ADMINISTRADOR -> "EXISTS(SELECT 1 FROM administrador a WHERE a.id_usuario=u.id_usuario)";
            case CLIENTE -> "EXISTS(SELECT 1 FROM cliente c WHERE c.id_usuario=u.id_usuario)";
            case PROPIETARIO -> "EXISTS(SELECT 1 FROM propietario p WHERE p.id_usuario=u.id_usuario)";
        };
        return consultar("SELECT u.* FROM usuario u WHERE "+condicion+" ORDER BY u.id_usuario",s->{});
    }
    @Override public Usuario crear(Usuario u) { nueva(u.getId()); return escribir(u,false); }
    @Override public Usuario actualizar(Usuario u) { id(u.getId()); return escribir(u,true); }
    private Usuario escribir(Usuario u,boolean actualizar) {
        Objects.requireNonNull(u);
        return conexion(c->{
            boolean propia=c.getAutoCommit();
            if(propia) c.setAutoCommit(false);
            try {
                String sql=actualizar
                    ? "UPDATE usuario SET nombre_completo=?,correo_normalizado=?,contrasena_hash=?,telefono=?,activo=? WHERE id_usuario=? RETURNING id_usuario"
                    : "INSERT INTO usuario(nombre_completo,correo_normalizado,contrasena_hash,telefono,activo) VALUES(?,?,?,?,?) RETURNING id_usuario";
                long id;
                try(PreparedStatement s=c.prepareStatement(sql)) {
                    s.setString(1,u.getNombre()); s.setString(2,u.getCorreo()); s.setString(3,u.getHashContrasena());
                    s.setString(4,u.getTelefono()); s.setBoolean(5,u.isActivo());
                    if(actualizar) s.setLong(6,u.getId());
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new co.edu.rentainmuebles.dao.DaoException("El usuario no existe.");
                        id=r.getLong(1);
                    }
                }
                perfil(c,"administrador",id,u.isEsAdministrador()); perfil(c,"cliente",id,u.isCliente()); perfil(c,"propietario",id,u.isPropietario());
                Usuario resultado=consultar(c,"SELECT * FROM usuario WHERE id_usuario=?",s->s.setLong(1,id)).get(0);
                if(propia) c.commit();
                return resultado;
            } catch(SQLException | RuntimeException | Error e) {
                if(propia) { try { c.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); } }
                throw e;
            } finally { if(propia) c.setAutoCommit(true); }
        });
    }
    private static void perfil(Connection c,String tabla,long id,boolean presente) throws SQLException {
        // tabla proviene exclusivamente de las dos constantes internas anteriores.
        String sql=presente ? "INSERT INTO "+tabla+"(id_usuario) VALUES(?) ON CONFLICT DO NOTHING" : "DELETE FROM "+tabla+" WHERE id_usuario=?";
        try(PreparedStatement s=c.prepareStatement(sql)) { s.setLong(1,id); s.executeUpdate(); }
    }
    @Override public void desactivar(long id) { modificar("UPDATE usuario SET activo=FALSE WHERE id_usuario=?",s->s.setLong(1,id(id))); }
}