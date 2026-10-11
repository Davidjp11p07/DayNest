package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.NegocioException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
@WebServlet(urlPatterns={"/login","/registro","/salir"})
public final class AutenticacionServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        if("/salir".equals(r.getServletPath())) { p.sendError(405,"El cierre de sesión requiere un formulario POST."); return; }
        if(Web.usuario(r)!=null) { Web.redirect(r,p,Web.inicioRol(Web.usuario(r))); return; }
        Web.vista(r,p,"publico"+r.getServletPath());
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p) {
        if("/salir".equals(r.getServletPath())) {
            HttpSession anterior=r.getSession(false); if(anterior!=null) anterior.invalidate();
            Web.flash(r,"exito","Sesión cerrada correctamente."); Web.redirect(r,p,"/login"); return;
        }
        String password=r.getParameter("password"); char[] chars=password==null?new char[0]:password.toCharArray();
        try {
            if("/registro".equals(r.getServletPath())) {
                String perfiles=Web.opcional(r,"rol");
                if (!java.util.Set.of("CLIENTE","PROPIETARIO","AMBOS").contains(perfiles==null?"":perfiles))
                    throw new NegocioException("Seleccione cliente, propietario o ambos.");
                Web.servicios(r).autenticacion.registrar(Web.texto(r,"nombre"),Web.texto(r,"correo"),chars,Web.opcional(r,"telefono"),
                        !"PROPIETARIO".equals(perfiles),!"CLIENTE".equals(perfiles));
                Web.flash(r,"exito","Registro completado. Ya puede iniciar sesión."); Web.redirect(r,p,"/login");
            } else {
                Usuario usuario=Web.servicios(r).autenticacion.login(Web.texto(r,"correo"),chars);
                HttpSession anterior=r.getSession(false);
                String retorno=anterior==null?null:(String)anterior.getAttribute("retornoReserva");
                if(anterior!=null) anterior.invalidate();
                HttpSession nueva=r.getSession(true); nueva.setAttribute("usuario",new UsuarioSesion(usuario)); Web.token(nueva);
                Web.flash(r,"exito","Bienvenido, "+usuario.getNombre()+"."); Web.redirect(r,p,usuario.isCliente() && retorno!=null?retorno:Web.inicioRol(Web.usuario(r)));
            }
        } catch(NegocioException e) {
            Web.recordarFormulario(r,r.getServletPath(),"nombre","correo","telefono","rol");
            Web.errorFormulario(r,e); Web.redirect(r,p,r.getServletPath());
        }
        finally { Arrays.fill(chars,'\0'); }
    }
}
