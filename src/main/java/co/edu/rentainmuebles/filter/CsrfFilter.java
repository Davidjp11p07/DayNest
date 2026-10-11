package co.edu.rentainmuebles.filter;
import co.edu.rentainmuebles.servlet.Web;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public final class CsrfFilter implements Filter {
    @Override public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest r=(HttpServletRequest)req; HttpServletResponse p=(HttpServletResponse)res;
        if("POST".equals(r.getMethod())) {
            HttpSession s=r.getSession(false); String recibido=r.getParameter("csrf");
            String esperado=s==null?null:(String)s.getAttribute("csrf");
            if(esperado==null||recibido==null||!MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),recibido.getBytes(StandardCharsets.UTF_8))) {
                p.sendError(403,"El formulario venció o no es válido. Recargue la página e inténtelo otra vez."); return;
            }
        } else if("GET".equals(r.getMethod())&&!r.getRequestURI().startsWith(r.getContextPath()+"/assets/")) { Web.token(r.getSession()); }
        if(!r.getRequestURI().startsWith(r.getContextPath()+"/assets/")) p.setHeader("Cache-Control","no-store");
        chain.doFilter(req,res);
    }
}
