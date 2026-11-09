package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.service.NegocioException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet(urlPatterns={"","/inicio","/buscar","/inmueble","/foto"})
public final class PublicoServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        String ruta=r.getServletPath();
        var contenido=new co.edu.rentainmuebles.service.ContenidoInmuebleService();
        if("/foto".equals(ruta)) {
            try {
            var foto=contenido.imagenPublica(Web.id(r,"id"));
            if(foto.isEmpty()){p.sendError(404);return;}
            p.setContentType(foto.get().mime());p.setHeader("X-Content-Type-Options","nosniff");p.setHeader("Cache-Control","public, max-age=3600");
            p.getOutputStream().write(foto.get().datos());
            } catch(NegocioException e) { p.sendError(404,"La foto no está disponible."); }
            return;
        }
        r.setAttribute("ciudades",Web.servicios(r).catalogo.ciudades());
        r.setAttribute("paises",Web.servicios(r).catalogo.paises());
        r.setAttribute("divisiones",Web.servicios(r).catalogo.divisiones());
        if("/inmueble".equals(ruta)) {
            try {
                long id=Web.id(r,"id");r.setAttribute("inmueble",Web.servicios(r).inmuebles.obtenerPublicado(id));
                Web.calendario(r,id,null,"/inmueble");
                var calificaciones=contenido.calificacionesInmueble(id);r.setAttribute("fotosInmueble",contenido.fotosPublicas(id));r.setAttribute("calificaciones",calificaciones);
                r.setAttribute("promedioCalificacion",calificaciones.isEmpty()?null:calificaciones.stream().mapToInt(co.edu.rentainmuebles.model.Calificacion::getPuntuacion).average().orElse(0));
                Web.vista(r,p,"publico/detalle");
            }
            catch(NegocioException e) { p.sendError(404,"El inmueble no está disponible."); }
        } else {
            try { r.setAttribute("paisActivo",Web.servicios(r).catalogo.paises().get(Web.idOpcional(r,"paisId")));r.setAttribute("divisionActiva",Web.servicios(r).catalogo.divisiones().get(Web.idOpcional(r,"divisionId")));r.setAttribute("inmuebles",Web.servicios(r).inmuebles.buscar((Web.opcional(r,"ciudadId")==null?Web.opcional(r,"ciudad"):"id:"+Web.id(r,"ciudadId")),Web.dinero(r,"minimo"),Web.dinero(r,"maximo"),Web.entero(r,"capacidad"),Web.fecha(r,"entrada"),Web.fecha(r,"salida"),new co.edu.rentainmuebles.model.FiltrosInmueble(Web.idOpcional(r,"paisId"),Web.idOpcional(r,"divisionId"),Web.opcional(r,"tipo"),Web.booleanoOpcional(r,"rural"),Web.booleanoOpcional(r,"mar")))); }
            catch(NegocioException | IllegalArgumentException e) { p.setStatus(400);r.setAttribute("error",e.getMessage()); }
            Web.vista(r,p,"/buscar".equals(ruta)?"publico/resultados":"publico/inicio");
        }
    }
}
