package co.edu.rentainmuebles.servlet;

import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.filter.*;
import co.edu.rentainmuebles.model.Usuario;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Filtros y servlet reales, sesión HTTP simulada y PostgreSQL temporal. */
class PerfilesServletTest extends SoportePostgres {
    @Test void clienteObtienePermisosPropietarioEnLaMismaSesion() throws Exception {
        Navegador n=new Navegador(cliente.getUsuario()); n.parametros.put("perfil","PROPIETARIO");
        n.parametros.put("usuarioId",String.valueOf(otroCliente.getId()));
        n.enviar();
        assertEquals("/DayNest/cuenta/perfiles",n.redirect);
        assertTrue(n.renovada); assertTrue(n.usuario().isCliente()); assertTrue(n.usuario().isPropietario());
        assertFalse(n.usuario().isEsAdministrador()); assertEquals(cliente.getId().longValue(),n.usuario().getId());
        assertEquals("valor conservado",n.sesion.get("otroAtributo")); assertEquals("TOKEN-PRUEBA",n.sesion.get("csrf"));
        assertFalse(usuarios.buscarPorId(otroCliente.getId()).orElseThrow().isPropietario());
        n.puedeEntrar("/cliente/reservas",true); n.puedeEntrar("/propietario/inmuebles",true); n.puedeEntrar("/admin",false);
    }

    @Test void propietarioObtienePermisosClienteEnLaMismaSesion() throws Exception {
        Navegador n=new Navegador(propietario.getUsuario()); n.parametros.put("perfil","CLIENTE"); n.enviar();
        assertTrue(n.renovada); assertTrue(n.usuario().isCliente()); assertTrue(n.usuario().isPropietario());
        assertFalse(n.usuario().isEsAdministrador());
        n.puedeEntrar("/cliente/reservas",true); n.puedeEntrar("/propietario/inmuebles",true);
    }

    @Test void csrfAusenteOIncorrectoNoActivaNiRenuevaSesion() throws Exception {
        for (String csrf:new String[]{null,"OTRO-TOKEN"}) {
            Navegador n=new Navegador(cliente.getUsuario()); n.parametros.put("perfil","PROPIETARIO");
            if(csrf==null)n.parametros.remove("csrf");else n.parametros.put("csrf",csrf);
            n.enviar(); assertEquals(403,n.status); assertFalse(n.renovada);
            assertFalse(usuarios.buscarPorId(cliente.getId()).orElseThrow().isPropietario());
        }
    }

    @Test void sinAutenticacionNoActivaPerfil() throws Exception {
        Navegador n=new Navegador(null); n.parametros.put("perfil","PROPIETARIO"); n.enviar();
        assertEquals("/DayNest/login",n.redirect); assertFalse(n.renovada);
        assertFalse(usuarios.buscarPorId(cliente.getId()).orElseThrow().isPropietario());
    }

    @Test void perfilAdministradorOInvalidoNoModificaPermisos() throws Exception {
        for(String perfil:new String[]{"ADMINISTRADOR","OTRO",""}) {
            Navegador n=new Navegador(cliente.getUsuario()); n.parametros.put("perfil",perfil); n.enviar();
            assertEquals("error",n.sesion.get("flashTipo")); assertFalse(n.renovada);
            assertFalse(n.usuario().isEsAdministrador()); assertFalse(n.usuario().isPropietario());
        }
    }

    @Test void getSoloMuestraPaginaSinActivar() throws Exception {
        Navegador n=new Navegador(cliente.getUsuario()); n.metodo="GET";
        n.parametros.put("perfil","PROPIETARIO"); n.enviar();
        assertEquals("/WEB-INF/views/cuenta/perfiles.jsp",n.vista);
        assertFalse(n.renovada); assertFalse(usuarios.buscarPorId(cliente.getId()).orElseThrow().isPropietario());
    }

    @Test void cuentaDesactivadaDespuesDelLoginNoAdquierePerfil() throws Exception {
        Navegador n=new Navegador(cliente.getUsuario()); usuarios.desactivar(cliente.getId());
        n.parametros.put("perfil","PROPIETARIO"); n.enviar();
        assertEquals("error",n.sesion.get("flashTipo")); assertFalse(n.renovada);
        assertFalse(usuarios.buscarPorId(cliente.getId()).orElseThrow().isPropietario());
    }

    private final class Navegador {
        final Map<String,Object> sesion=new HashMap<>(),atributos=new HashMap<>();
        final Map<String,String> parametros=new HashMap<>();
        String metodo="POST",ruta="/cuenta/perfiles",redirect,vista;
        int status=200; boolean renovada;
        final HttpServletRequest request; final HttpServletResponse response;
        Navegador(Usuario usuario) {
            if(usuario!=null)sesion.put("usuario",new UsuarioSesion(usuario));
            sesion.put("csrf","TOKEN-PRUEBA");sesion.put("otroAtributo","valor conservado");parametros.put("csrf","TOKEN-PRUEBA");
            HttpSession session=proxy(HttpSession.class,(o,m,a)->switch(m.getName()) {
                case "getAttribute" -> sesion.get(a[0]);
                case "setAttribute" -> {sesion.put((String)a[0],a[1]);yield null;}
                default -> valorInicial(m.getReturnType());
            });
            ServletContext context=proxy(ServletContext.class,(o,m,a)->"getAttribute".equals(m.getName())?servicios:valorInicial(m.getReturnType()));
            request=proxy(HttpServletRequest.class,(o,m,a)->switch(m.getName()) {
                case "getSession" -> session;
                case "getServletContext" -> context;
                case "getMethod" -> metodo;
                case "getServletPath" -> ruta;
                case "getContextPath" -> "/DayNest";
                case "getRequestURI" -> "/DayNest"+ruta;
                case "getParameter" -> parametros.get(a[0]);
                case "getAttribute" -> atributos.get(a[0]);
                case "setAttribute" -> {atributos.put((String)a[0],a[1]);yield null;}
                case "changeSessionId" -> {renovada=true;yield "SESION-RENOVADA";}
                case "getRequestDispatcher" -> {
                    String destino=(String)a[0];
                    yield proxy(RequestDispatcher.class,(d,dm,da)->{if("forward".equals(dm.getName()))vista=destino;return null;});
                }
                default -> valorInicial(m.getReturnType());
            });
            response=proxy(HttpServletResponse.class,(o,m,a)->switch(m.getName()) {
                case "sendRedirect" -> {redirect=(String)a[0];yield null;}
                case "setHeader" -> {if("Location".equals(a[0]))redirect=(String)a[1];yield null;}
                case "sendError","setStatus" -> {status=(Integer)a[0];yield null;}
                case "encodeRedirectURL" -> a[0];
                default -> valorInicial(m.getReturnType());
            });
        }
        void enviar() throws Exception {
            new CsrfFilter().doFilter(request,response,(r,p)->new AutenticacionFilter().doFilter(r,p,(rr,pp)-> {
                PerfilesServlet servlet=new PerfilesServlet();
                if("POST".equals(metodo))servlet.doPost(request,response);else servlet.doGet(request,response);
            }));
        }
        UsuarioSesion usuario(){return (UsuarioSesion)sesion.get("usuario");}
        void puedeEntrar(String destino,boolean permitido) throws Exception {
            ruta=destino; boolean[] paso={false};status=200;
            new AutorizacionFilter().doFilter(request,response,(r,p)->paso[0]=true);
            assertEquals(permitido,paso[0],destino);if(!permitido)assertEquals(403,status);
        }
    }
    private static <T> T proxy(Class<T> tipo,InvocationHandler handler) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(),new Class<?>[]{tipo},handler));
    }
    private static Object valorInicial(Class<?> tipo) {
        if(tipo==boolean.class)return false;if(tipo==int.class)return 0;if(tipo==long.class)return 0L;return null;
    }
}
