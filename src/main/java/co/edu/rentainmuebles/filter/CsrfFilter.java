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
            if(r.getContentType()!=null && r.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
                if(r.getContentLengthLong()>6_000_000) {
                    p.sendError(413,"La carga supera el límite permitido. Selecciona una imagen de hasta 5 MB."); return;
                }
                try { r.getParts(); }
                catch(IllegalStateException e) {
                    p.sendError(413,"La carga supera el límite permitido. Selecciona una imagen de hasta 5 MB."); return;
                }
                catch(ServletException e) {
                    p.sendError(400,"No se pudo leer la imagen. Vuelve al formulario y selecciona un archivo JPEG o PNG."); return;
                }
            }
            HttpSession s=r.getSession(false); String recibido;
            try { recibido=r.getParameter("csrf"); }
            catch(IllegalStateException e) {
                if(r.getContentType()!=null && r.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
                    p.sendError(413,"La carga supera el límite permitido. Selecciona una imagen de hasta 5 MB."); return;
                }
                throw e;
            }
            String esperado=s==null?null:(String)s.getAttribute("csrf");
            if(esperado==null||recibido==null||!MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),recibido.getBytes(StandardCharsets.UTF_8))) {
                p.sendError(403,"El formulario venció o no es válido. Recargue la página e inténtelo otra vez."); return;
            }
        } else if("GET".equals(r.getMethod())&&!r.getRequestURI().startsWith(r.getContextPath()+"/assets/")) { Web.token(r.getSession()); }
        if(!r.getRequestURI().startsWith(r.getContextPath()+"/assets/")) p.setHeader("Cache-Control","no-store");
        chain.doFilter(req,res);
    }
}
