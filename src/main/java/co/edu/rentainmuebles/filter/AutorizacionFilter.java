package co.edu.rentainmuebles.filter;
import co.edu.rentainmuebles.servlet.Web;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
public final class AutorizacionFilter implements Filter {
    @Override public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest r=(HttpServletRequest)req; HttpServletResponse p=(HttpServletResponse)res;
        String ruta=r.getServletPath();
        String rol=(ruta.equals("/admin")||ruta.startsWith("/admin/"))?"ADMINISTRADOR":(ruta.equals("/propietario")||ruta.startsWith("/propietario/"))?"PROPIETARIO":"CLIENTE";
        if(Web.usuario(r)==null||!Web.usuario(r).tienePermiso(rol)) { p.sendError(403,"No tiene permiso para acceder a esta sección."); return; }
        chain.doFilter(req,res);
    }
}
