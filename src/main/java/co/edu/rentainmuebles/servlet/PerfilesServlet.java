package co.edu.rentainmuebles.servlet;

import co.edu.rentainmuebles.model.Rol;
import co.edu.rentainmuebles.model.Usuario;
import co.edu.rentainmuebles.service.NegocioException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/cuenta/perfiles")
public final class PerfilesServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        Web.vista(r,p,"cuenta/perfiles");
    }

    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p) throws IOException {
        try {
            // La identidad proviene exclusivamente de la sesión autenticada.
            Usuario actualizado=Web.servicios(r).autenticacion.activarPerfil(Web.usuario(r).getId(),Web.estado(r,"perfil",Rol.class));
            // El servicio devuelve después del commit. Renueva permisos sin perder otros atributos.
            r.changeSessionId();
            r.getSession().setAttribute("usuario",new UsuarioSesion(actualizado));
            Web.flash(r,"exito","Perfil activado. Ya puede reservar y publicar con la misma cuenta.");
        } catch (NegocioException e) {
            Web.errorFormulario(r,e);
        }
        Web.redirect(r,p,"/cuenta/perfiles");
    }
}
