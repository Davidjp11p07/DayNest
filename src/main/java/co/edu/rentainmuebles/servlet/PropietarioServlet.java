package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.NegocioException;
import co.edu.rentainmuebles.service.ComisionService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet(urlPatterns={"/propietario","/propietario/*"})
public final class PropietarioServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        String ruta=r.getPathInfo()==null||"/".equals(r.getPathInfo())?"/inmuebles":r.getPathInfo();
        try {
            long usuario=Web.usuario(r).getId();
            r.setAttribute("ciudades",Web.servicios(r).catalogo.ciudades());
            switch(ruta) {
                case "/inmuebles" -> { r.setAttribute("inmuebles",Web.servicios(r).inmuebles.listarPropios(usuario)); Web.vista(r,p,"propietario/inmuebles"); }
                case "/bloqueos" -> { r.setAttribute("inmuebles",Web.servicios(r).inmuebles.listarPropios(usuario)); r.setAttribute("bloqueos",new co.edu.rentainmuebles.service.BloqueoService().listar(usuario)); Web.vista(r,p,"propietario/bloqueos"); }
                case "/publicar" -> Web.vista(r,p,"propietario/formulario");
                case "/editar" -> { long id=Web.id(r,"id"); r.setAttribute("inmueble",Web.servicios(r).inmuebles.listarPropios(usuario).stream().filter(i->i.getId()==id).findFirst().orElseThrow(()->new NegocioException("El inmueble no pertenece al propietario."))); Web.vista(r,p,"propietario/formulario"); }
                case "/reservas" -> { r.setAttribute("reservas",Web.servicios(r).reservas.listarRecibidas(usuario)); Web.vista(r,p,"propietario/reservas"); }
                case "/giros" -> { var comisiones=new ComisionService();if(comisiones.disponible()){r.setAttribute("liquidaciones",comisiones.liquidaciones(usuario,false));r.setAttribute("finanzasDisponibles",true);} r.setAttribute("giros",Web.servicios(r).giros.listarPropios(usuario)); Web.vista(r,p,"propietario/giros"); }
                default -> p.sendError(404);
            }
        } catch(NegocioException e) { Web.errorConsulta(r,p,e); }
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p) throws IOException {
        try {
            long usuario=Web.usuario(r).getId();
            r.setAttribute("ciudades",Web.servicios(r).catalogo.ciudades()); String ruta=r.getPathInfo();
            if("/bloquear".equals(ruta)) { new co.edu.rentainmuebles.service.BloqueoService().crear(usuario,Web.id(r,"inmuebleId"),Web.fecha(r,"entrada"),Web.fecha(r,"salida"),Web.texto(r,"motivo")); Web.flash(r,"exito","Bloqueo registrado."); Web.redirect(r,p,"/propietario/bloqueos"); return; }
            else if("/liberar".equals(ruta)) { new co.edu.rentainmuebles.service.BloqueoService().liberar(usuario,Web.id(r,"id")); Web.flash(r,"exito","Fechas liberadas."); Web.redirect(r,p,"/propietario/bloqueos"); return; }
            else if("/desactivar".equals(ruta)) { Web.servicios(r).inmuebles.desactivar(usuario,Web.id(r,"id")); Web.flash(r,"exito","Inmueble desactivado. Se conservan sus reservas."); }
            else if("/publicar".equals(ruta)||"/editar".equals(ruta)) {
                if("/publicar".equals(ruta)) Web.servicios(r).inmuebles.publicar(usuario,Web.texto(r,"titulo"),(Web.opcional(r,"ciudadId")==null?Web.texto(r,"ciudad"):"id:"+Web.id(r,"ciudadId")),Web.texto(r,"direccion"),Web.texto(r,"descripcion"),Web.requerido(r,"capacidad"),Web.dinero(r,"precio"),Web.opcional(r,"tipo"),Web.booleanoOpcional(r,"esRural"),Web.booleanoOpcional(r,"cercaMar"),Web.dinero(r,"area"),Web.entero(r,"habitaciones"));
                else Web.servicios(r).inmuebles.editar(usuario,Web.id(r,"id"),Web.texto(r,"titulo"),(Web.opcional(r,"ciudadId")==null?Web.texto(r,"ciudad"):"id:"+Web.id(r,"ciudadId")),Web.texto(r,"direccion"),Web.texto(r,"descripcion"),Web.requerido(r,"capacidad"),Web.dinero(r,"precio"),Web.opcional(r,"tipo"),Web.booleanoOpcional(r,"esRural"),Web.booleanoOpcional(r,"cercaMar"),Web.dinero(r,"area"),Web.entero(r,"habitaciones"));
                Web.flash(r,"exito","Inmueble guardado correctamente.");
            } else { p.sendError(404); return; }
        } catch(NegocioException e) {
            String ruta=r.getPathInfo();
            if("/publicar".equals(ruta)||"/editar".equals(ruta)) {
                String destino="/publicar".equals(ruta)?"/propietario/publicar":Web.rutaConId(r,"/propietario/editar","id","/propietario/inmuebles");
                Web.recordarFormulario(r,destino,"titulo","ciudad","ciudadId","direccion","descripcion","capacidad","precio","tipo","esRural","cercaMar","area","habitaciones");
                Web.errorFormulario(r,e); Web.redirect(r,p,destino); return;
            }
            Web.errorFormulario(r,e);
        }
        Web.redirect(r,p,"/propietario/inmuebles");
    }
}
