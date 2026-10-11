package co.edu.rentainmuebles.service;

import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AutenticacionService {
    Usuario registrar(String nombre, String correo, char[] password, Rol rol);
    Usuario registrar(String nombre,String correo,char[] password,String telefono,boolean cliente,boolean propietario);
    Usuario login(String correo, char[] password);
    /** Añade el perfil complementario sin retirar perfiles ni cambiar la identidad. */
    Usuario activarPerfil(long usuarioId, Rol perfil);
}
