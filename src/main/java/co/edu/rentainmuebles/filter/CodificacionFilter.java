package co.edu.rentainmuebles.filter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
public final class CodificacionFilter implements Filter {
    @Override public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain) throws IOException,ServletException {
        req.setCharacterEncoding("UTF-8"); res.setCharacterEncoding("UTF-8");
        HttpServletResponse p=(HttpServletResponse)res;
        p.setHeader("X-Content-Type-Options","nosniff"); p.setHeader("Referrer-Policy","same-origin");
        p.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        chain.doFilter(req,res);
    }
}
