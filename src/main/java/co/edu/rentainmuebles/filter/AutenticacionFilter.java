package co.edu.rentainmuebles.filter;
import co.edu.rentainmuebles.servlet.Web;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
public final class AutenticacionFilter implements Filter {
    @Override public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest r=(HttpServletRequest)req; HttpServletResponse p=(HttpServletResponse)res;
        if(Web.usuario(r)==null) { Web.flash(r,"error","Inicie sesión para acceder a esta sección."); Web.redirect(r,p,"/login"); return; }
        chain.doFilter(req,res);
    }
}
