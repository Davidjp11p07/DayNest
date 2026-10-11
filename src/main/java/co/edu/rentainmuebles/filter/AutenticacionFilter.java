package co.edu.rentainmuebles.filter;
import co.edu.rentainmuebles.servlet.Web;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
public final class AutenticacionFilter implements Filter {
    @Override public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest r=(HttpServletRequest)req; HttpServletResponse p=(HttpServletResponse)res;
        if(Web.usuario(r)==null) {
            if("GET".equals(r.getMethod()) && "/cliente".equals(r.getServletPath()) && "/reservar".equals(r.getPathInfo())) {
                try { r.getSession().setAttribute("retornoReserva",Web.destinoReserva(r)); }
                catch(co.edu.rentainmuebles.service.NegocioException e) { /* No guardar identificadores invalidos. */ }
            }
            Web.flash(r,"error","Inicie sesión para acceder a esta sección."); Web.redirect(r,p,"/login"); return;
        }
        chain.doFilter(req,res);
    }
}
