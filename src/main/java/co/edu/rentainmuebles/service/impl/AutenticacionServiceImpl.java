package co.edu.rentainmuebles.service.impl;

import co.edu.rentainmuebles.dao.UnidadTrabajoDao;
import co.edu.rentainmuebles.service.*;
import co.edu.rentainmuebles.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class AutenticacionServiceImpl extends ServicioBase implements AutenticacionService {

    public AutenticacionServiceImpl(UnidadTrabajoDao trabajo, Clock reloj) { super(trabajo,reloj); }
    @Override public Usuario registrar(String nombre, String correo, char[] password, Rol rol) {
        if (rol != Rol.CLIENTE && rol != Rol.PROPIETARIO) throw new NegocioException("El registro público permite únicamente clientes y propietarios.");
        return registrar(nombre,correo,password,null,rol==Rol.CLIENTE,rol==Rol.PROPIETARIO);
    }
    @Override public Usuario registrar(String nombre,String correo,char[] password,String telefono,boolean cliente,boolean propietario) {
        return ejecutar(d -> {
            if (!cliente && !propietario) throw new NegocioException("Seleccione al menos un perfil para registrarse.");
            if (password == null || password.length < 8 || password.length > 128) { throw new NegocioException("La contraseña debe tener entre 8 y 128 caracteres."); }
            String normalizado = correo(correo);
            if (d.usuarios().buscarPorCorreo(normalizado).isPresent()) { throw new NegocioException("El correo ya está registrado."); }
            char[] copia = password.clone();
            try {
                String hash = co.edu.rentainmuebles.util.PasswordUtil.hash(copia);
                Usuario nuevo = new Usuario(null,nombre,normalizado,hash,telefono,true,false,cliente,propietario);
                return d.usuarios().crear(nuevo);
            } finally { java.util.Arrays.fill(copia,'\0'); }
        });
    }
    @Override public Usuario login(String correo, char[] password) {
        return ejecutar(d -> {
            String normalizado = correo(correo);
            if (password == null || password.length == 0 || password.length > 128) { throw new NegocioException("Correo o contraseña incorrectos."); }
            Usuario u = d.usuarios().buscarPorCorreo(normalizado).orElseThrow(() -> new NegocioException("Correo o contraseña incorrectos."));
            char[] copia = password.clone();
            try {
                if (!co.edu.rentainmuebles.util.PasswordUtil.verificar(copia,u.getHashContrasena())) { throw new NegocioException("Correo o contraseña incorrectos."); }
            } finally { java.util.Arrays.fill(copia,'\0'); }
            if (!u.isActivo()) { throw new NegocioException("La cuenta está desactivada."); }
            return u;
        });
    }
    private static String correo(String correo) {
        if (correo == null) { throw new NegocioException("El correo es obligatorio."); }
        String normalizado = correo.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizado.length() > 200 || !normalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) { throw new NegocioException("El correo no tiene un formato válido o excede 200 caracteres."); }
        return normalizado;
    }

}
