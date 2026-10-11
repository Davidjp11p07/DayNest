package co.edu.rentainmuebles.dao;

import co.edu.rentainmuebles.model.*;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/** Ausencia de resultados: Optional vacío o lista vacía; fallos: DaoException. */
public interface UsuarioDao {
    Optional<Usuario> buscarPorId(long id);
    List<Usuario> listar();
    Usuario crear(Usuario usuario);
    Usuario actualizar(Usuario usuario);
    /** Activación aditiva, idempotente y limitada a CLIENTE/PROPIETARIO. */
    Usuario activarPerfil(long id, Rol perfil);
    void desactivar(long id);
    Optional<Usuario> buscarPorCorreo(String correo);
    List<Usuario> buscarPorRol(Rol rol);
}
