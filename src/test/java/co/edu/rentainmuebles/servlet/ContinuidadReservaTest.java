package co.edu.rentainmuebles.servlet;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.filter.AutenticacionFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContinuidadReservaTest extends SoportePostgres {
    @Test void detalleYReservaPrecarganFechasYCapacidadDeLaBusqueda() throws Exception {
        Navegador n=new Navegador(); n.parametros.put("capacidad","3");
        Web.vista(n.request,n.response,"publico/detalle");
        assertEquals(Map.of("entrada",HOY.plusDays(10).toString(),"salida",HOY.plusDays(13).toString(),"huespedes","3"),n.atributos.get("form"));
        n.sesion.put("usuario",new UsuarioSesion(cliente.getUsuario()));
        n.atributos.clear();n.ruta="/cliente";n.path="/reservar";n.parametros.put("huespedes","2");
        new ClienteServlet().doGet(n.request,n.response);
        assertEquals("2",((Map<?,?>)n.atributos.get("form")).get("huespedes"));
        assertEquals("/WEB-INF/views/cliente/reservar.jsp",n.vista);
    }

    @Test void loginRenuevaSesionYRetornaAReservaConSeleccion() throws Exception {
        Navegador n=new Navegador();n.ruta="/cliente";n.path="/reservar";
        n.parametros.put("huespedes","2");
        new AutenticacionFilter().doFilter(n.request,n.response,(r,p)->fail("No debe pasar sin autenticar"));
        assertEquals("/DayNest/login",n.redirect);
        String destino=(String)n.sesion.get("retornoReserva");
        assertEquals(Web.destinoReserva(n.request),destino);
        n.ruta="/login";n.path=null;n.metodo="POST";
        n.parametros.put("correo",cliente.getUsuario().getCorreo());n.parametros.put("password","PruebaJUnit2026!");
        new AutenticacionServlet().doPost(n.request,n.response);
        assertTrue(n.invalidada);assertEquals("/DayNest"+destino,n.redirect);
        assertNotNull(n.sesion.get("csrf"));assertNull(n.sesion.get("retornoReserva"));
    }

    @Test void errorDeReservaConservaLosDatosCorregidosYNoCreaReserva() throws Exception {
        Navegador n=new Navegador();n.sesion.put("usuario",new UsuarioSesion(cliente.getUsuario()));
        n.ruta="/cliente";n.path="/reservar";n.metodo="POST";
        n.parametros.put("inmuebleId",inmueble.getId().toString());n.parametros.put("huespedes","5");
        new ClienteServlet().doPost(n.request,n.response);
        assertEquals("error",n.sesion.get("flashTipo"));
        assertTrue(servicios.reservas.listarPropias(cliente.getId()).isEmpty());
        n.metodo="GET";n.parametros.put("huespedes","1");
        new ClienteServlet().doGet(n.request,n.response);
        assertEquals("5",((Map<?,?>)n.atributos.get("form")).get("huespedes"));
    }

    @Test void retornoNoAdmiteDestinoExternoNiGuardaPost() throws Exception {
        Navegador n=new Navegador();n.ruta="/cliente";n.path="/reservar";
        n.parametros.put("retorno","https://ejemplo.test");n.parametros.put("huespedes","2&retorno=https://ejemplo.test");
        assertTrue(Web.destinoReserva(n.request).contains("huespedes=2%26retorno%3Dhttps%3A%2F%2Fejemplo.test"));
        n.metodo="POST";new AutenticacionFilter().doFilter(n.request,n.response,(r,p)->fail());
        assertNull(n.sesion.get("retornoReserva"));
    }

    private final class Navegador {
        final Map<String,Object> sesion=new HashMap<>(),atributos=new HashMap<>();
        final Map<String,String> parametros=new HashMap<>();
        String ruta="/inmueble",path,metodo="GET",redirect,vista; boolean invalidada;
        final HttpServletRequest request; final HttpServletResponse response;
        Navegador() {
            parametros.put("id",inmueble.getId().toString());parametros.put("entrada",HOY.plusDays(10).toString());parametros.put("salida",HOY.plusDays(13).toString());
            HttpSession session=proxy(HttpSession.class,(o,m,a)->switch(m.getName()) {
                case "getAttribute" -> sesion.get(a[0]);
                case "setAttribute" -> {sesion.put((String)a[0],a[1]);yield null;}
                case "removeAttribute" -> {sesion.remove(a[0]);yield null;}
                case "invalidate" -> {sesion.clear();invalidada=true;yield null;}
                default -> null;
            });
            ServletContext context=proxy(ServletContext.class,(o,m,a)->servicios);
            request=proxy(HttpServletRequest.class,(o,m,a)->switch(m.getName()) {
                case "getSession" -> session;
                case "getServletContext" -> context;
                case "getMethod" -> metodo;
                case "getServletPath" -> ruta;
                case "getPathInfo" -> path;
                case "getContextPath" -> "/DayNest";
                case "getParameter" -> parametros.get(a[0]);
                case "getAttribute" -> atributos.get(a[0]);
                case "setAttribute" -> {atributos.put((String)a[0],a[1]);yield null;}
                case "getRequestDispatcher" -> {String destino=(String)a[0];yield proxy(RequestDispatcher.class,(d,dm,da)->{vista=destino;return null;});}
                default -> null;
            });
            response=proxy(HttpServletResponse.class,(o,m,a)->{if("setHeader".equals(m.getName()) && "Location".equals(a[0]))redirect=(String)a[1];return null;});
        }
    }
    private static <T> T proxy(Class<T> tipo,InvocationHandler h) { return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(),new Class<?>[]{tipo},h)); }
}
