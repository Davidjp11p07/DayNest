package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.SoportePostgres;
import co.edu.rentainmuebles.config.ConexionBD;
import co.edu.rentainmuebles.model.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReintentoGiroServletTest extends SoportePostgres {
 @Test void giroFallidoMantieneBotonDisponible()throws Exception {
  Reserva r=reservaPagada();alFinalizar(r);Giro g=giros.crear(new Giro(null,r,propietario,r.getTotal(),null,r.getFechaCreacion(),EstadoGiro.PENDIENTE));
  try(Connection c=ConexionBD.abrir();Statement t=c.createStatement()){t.execute("UPDATE giro SET estado='FALLIDO' WHERE id_giro="+g.getId());}
  Map<String,Object> atributos=new HashMap<>();
  HttpSession sesion=proxy(HttpSession.class,(o,m,a)->"getAttribute".equals(m.getName()) && "usuario".equals(a[0])?new UsuarioSesion(admin):null);
  ServletContext ctx=proxy(ServletContext.class,(o,m,a)->servicios);
  RequestDispatcher dispatcher=proxy(RequestDispatcher.class,(o,m,a)->null);
  HttpServletRequest req=proxy(HttpServletRequest.class,(o,m,a)->switch(m.getName()) {
   case "getSession"->sesion;case "getServletContext"->ctx;case "getPathInfo"->"/reservas";
   case "getAttribute"->atributos.get(a[0]);case "setAttribute"->{atributos.put((String)a[0],a[1]);yield null;}
   case "getRequestDispatcher"->dispatcher;case "getContextPath"->"/DayNest";case "getRequestURI"->"/DayNest/admin/reservas";
   default->null;
  });
  HttpServletResponse res=proxy(HttpServletResponse.class,(o,m,a)->{if(m.getName().equals("sendError"))fail("Error de servlet");return null;});
  new AdministradorServlet().doGet(req,res);
  assertTrue(((List<?>)atributos.get("girables")).contains(r.getId()));
  assertEquals(EstadoGiro.EJECUTADO,servicios.giros.generar(admin.getId(),r.getId(),null).getEstado());
 }
 private static <T>T proxy(Class<T> t,InvocationHandler h){return t.cast(Proxy.newProxyInstance(t.getClassLoader(),new Class<?>[]{t},h));}
}
