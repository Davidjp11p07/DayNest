package co.edu.rentainmuebles.filter;

import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IdiomaFilterTest {
    @Test void usaEspanolPorDefectoYRechazaIdiomasNoAdmitidos() throws Exception {
        var navegador=new Navegador("fr",null);
        navegador.ejecutar();
        assertEquals("es",navegador.atributos.get("idioma"));
        assertEquals("Mis reservas",navegador.textos().get("ui098"));
        assertTrue(navegador.cookies.isEmpty());
    }

    @Test void seleccionInglesPersisteYSePuedeCambiarAEspanol() throws Exception {
        var navegador=new Navegador("en",null);
        navegador.ejecutar();
        assertEquals("en",navegador.atributos.get("idioma"));
        assertEquals("My bookings",navegador.textos().get("ui098"));
        assertEquals("DayNest · Your space, your days.",navegador.textos().get("ui160"));
        Cookie cookie=navegador.cookies.get(0);
        assertEquals("/DayNest",cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getMaxAge()>0);
        var siguiente=new Navegador(null,new Cookie[]{cookie});
        siguiente.ejecutar();
        assertEquals("en",siguiente.atributos.get("idioma"));
        var espanol=new Navegador("es",new Cookie[]{cookie});
        espanol.ejecutar();
        assertEquals("es",espanol.atributos.get("idioma"));
        assertEquals("es",espanol.cookies.get(0).getValue());
    }

    @Test void catalogosTienenLasMismasClavesYTraduccionesNoVacias() {
        var es=ResourceBundle.getBundle("i18n.mensajes",Locale.ROOT);
        var en=ResourceBundle.getBundle("i18n.mensajes",Locale.ENGLISH);
        assertEquals(es.keySet(),en.keySet());
        for(String key:es.keySet()) {
            assertFalse(es.getString(key).isBlank(),key);
            assertFalse(en.getString(key).isBlank(),key);
        }
    }

    @Test void localizaMensajesSinTraducirNombresNiCambiarImportes() {
        var traductor=new co.edu.rentainmuebles.servlet.Traductor("en");
        assertEquals("Incorrect email or password.",traductor.traducir("Correo o contraseña incorrectos."));
        assertEquals("Welcome, Casa del Sol.",traductor.traducir("Bienvenido, Casa del Sol."));
        assertEquals("Payment completed for 375000.00 COP. Booking confirmed (simulation).",
            traductor.traducir("Pago realizado por 375000.00 COP. Reserva confirmada (simulación)."));
        assertEquals("Apartamento de David",traductor.traducir("Apartamento de David"));
        assertEquals("DayNest",traductor.traducir("DayNest"));
    }

    @Test void espanolNoDependeDelIdiomaDelServidor() throws Exception {
        Locale anterior=Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            var navegador=new Navegador("es",null);
            navegador.ejecutar();
            assertEquals("Mis reservas",navegador.textos().get("ui098"));
        } finally { Locale.setDefault(anterior); }
    }

    private static final class Navegador {
        final Map<String,Object> atributos=new HashMap<>();
        final List<Cookie> cookies=new ArrayList<>();
        final String lang;
        final Cookie[] anteriores;
        Navegador(String lang,Cookie[] anteriores){this.lang=lang;this.anteriores=anteriores;}
        @SuppressWarnings("unchecked") Map<String,String> textos(){return (Map<String,String>)atributos.get("textos");}
        void ejecutar() throws Exception {
            var request=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()) {
                case "getParameter" -> lang;
                case "getMethod" -> "GET";
                case "getCookies" -> anteriores;
                case "getContextPath" -> "/DayNest";
                case "isSecure" -> false;
                case "setAttribute" -> {atributos.put((String)a[0],a[1]);yield null;}
                default -> null;
            });
            var response=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
                if(m.getName().equals("addCookie"))cookies.add((Cookie)a[0]);
                return null;
            });
            new IdiomaFilter().doFilter(request,response,(r,p)->assertNotNull(atributos.get("textos")));
        }
    }
}
