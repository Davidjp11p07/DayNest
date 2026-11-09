package co.edu.rentainmuebles.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

/** Preferencia de idioma independiente de la sesión y de la moneda del alojamiento. */
public final class IdiomaFilter implements Filter {
    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        var r=(HttpServletRequest)req;
        var p=(HttpServletResponse)res;
        String idioma="es";
        if(r.getCookies()!=null) for(Cookie cookie:r.getCookies()) {
            if("daynest_idioma".equals(cookie.getName()) && "en".equals(cookie.getValue())) idioma="en";
        }
        String elegido="GET".equals(r.getMethod())?r.getParameter("lang"):null;
        if("es".equals(elegido)||"en".equals(elegido)) {
            idioma=elegido;
            Cookie cookie=new Cookie("daynest_idioma",idioma);
            cookie.setPath(r.getContextPath().isEmpty()?"/":r.getContextPath());
            cookie.setMaxAge(365*24*60*60);
            cookie.setHttpOnly(true);
            cookie.setSecure(r.isSecure());
            cookie.setAttribute("SameSite","Lax");
            p.addCookie(cookie);
        }
        r.setAttribute("idioma",idioma);
        r.setAttribute("localeIdioma","en".equals(idioma)?"en_US":"es_CO");
        var bundle=ResourceBundle.getBundle("i18n.mensajes","en".equals(idioma)?Locale.ENGLISH:Locale.ROOT);
        Map<String,String> textos=new HashMap<>();
        for(String key:bundle.keySet()) textos.put(key,bundle.getString(key));
        r.setAttribute("textos",Collections.unmodifiableMap(textos));
        r.setAttribute("traductor",new co.edu.rentainmuebles.servlet.Traductor(idioma));
        p.setHeader("Content-Language",idioma);
        chain.doFilter(req,res);
    }
}
