package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.NegocioException;
import co.edu.rentainmuebles.service.ComisionService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
@WebServlet(urlPatterns={"/propietario","/propietario/*"})
@MultipartConfig(maxFileSize=5_242_880, maxRequestSize=6_000_000, fileSizeThreshold=1_048_576)
public final class PropietarioServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        String ruta=r.getPathInfo()==null||"/".equals(r.getPathInfo())?"/inmuebles":r.getPathInfo();
        try {
            long usuario=Web.usuario(r).getId();
            if("/foto/ver".equals(ruta)){var imagen=contenido().imagenPropia(usuario,Web.id(r,"id"));if(imagen.isEmpty()){p.sendError(404);return;}p.setContentType(imagen.get().mime());p.setHeader("X-Content-Type-Options","nosniff");p.setHeader("Cache-Control","private, max-age=300");p.getOutputStream().write(imagen.get().datos());return;}
            r.setAttribute("ciudades",Web.servicios(r).catalogo.ciudades());
            switch(ruta) {
                case "/inmuebles" -> { r.setAttribute("inmuebles",Web.servicios(r).inmuebles.listarPropios(usuario)); Web.vista(r,p,"propietario/inmuebles"); }
                case "/bloqueos" -> { r.setAttribute("inmueblesDisponibles",Web.servicios(r).inmuebles.listarPropios(usuario)); r.setAttribute("bloqueos",new co.edu.rentainmuebles.service.BloqueoService().listar(usuario)); Web.vista(r,p,"propietario/bloqueos"); }
                case "/calendario" -> {
                    long id=Web.id(r,"id");
                    r.setAttribute("inmueble",Web.servicios(r).inmuebles.listarPropios(usuario).stream().filter(i->i.getId()==id).findFirst().orElseThrow(()->new NegocioException("El inmueble no pertenece al propietario.")));
                    Web.calendario(r,id,usuario,"/propietario/calendario");
                    r.setAttribute("bloqueos",new co.edu.rentainmuebles.service.BloqueoService().listar(usuario).stream().filter(b->b.ocupacion().inmuebleId()==id && b.ocupacion().activa()).toList());
                    Web.vista(r,p,"propietario/calendario");
                }
                case "/publicar" -> Web.vista(r,p,"propietario/formulario");
                case "/editar" -> { long id=Web.id(r,"id"); r.setAttribute("inmueble",Web.servicios(r).inmuebles.listarPropios(usuario).stream().filter(i->i.getId()==id).findFirst().orElseThrow(()->new NegocioException("El inmueble no pertenece al propietario."))); r.setAttribute("fotosPropias",contenido().fotosPropias(usuario,id)); Web.vista(r,p,"propietario/formulario"); }
                case "/reservas" -> { r.setAttribute("rolReservas","PROPIETARIO");var reservas=Web.servicios(r).reservas.listarRecibidas(usuario);r.setAttribute("reservas",reservas);var contenido=contenido();var calificados=new java.util.HashMap<Long,Boolean>();var calificacionesClientes=new java.util.HashMap<Long,java.util.List<Calificacion>>();for(Reserva reserva:reservas){if(reserva.getEstado()==EstadoReserva.FINALIZADA)calificados.put(reserva.getId(),contenido.yaCalifico(usuario,"PROPIETARIO",reserva.getId()));calificacionesClientes.computeIfAbsent(reserva.getCliente().getId(),contenido::calificacionesDelCliente);}r.setAttribute("calificados",calificados);r.setAttribute("calificacionesClientes",calificacionesClientes);Web.vista(r,p,"propietario/reservas"); }
                case "/giros" -> { var comisiones=new ComisionService();if(comisiones.disponible()){r.setAttribute("liquidaciones",comisiones.liquidaciones(usuario,false));r.setAttribute("finanzasDisponibles",true);} r.setAttribute("giros",Web.servicios(r).giros.listarPropios(usuario)); Web.vista(r,p,"propietario/giros"); }
                default -> p.sendError(404);
            }
        } catch(NegocioException e) { Web.errorConsulta(r,p,e); }
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        try {
            long usuario=Web.usuario(r).getId();
            r.setAttribute("ciudades",Web.servicios(r).catalogo.ciudades()); String ruta=r.getPathInfo();
            if("/foto/subir".equals(ruta)) {
                long id=Web.id(r,"id");Part parte=r.getPart("foto");if(parte==null||parte.getSize()<1||parte.getSize()>5_242_880)throw new NegocioException("Selecciona una imagen JPEG o PNG de hasta 5 MB.");
                byte[] datos;try(InputStream in=parte.getInputStream()){datos=in.readNBytes(5_242_881);}if(datos.length>5_242_880)throw new NegocioException("La imagen supera el límite de 5 MB.");
                String mime=tipoImagen(datos);String tipo=Web.opcional(r,"tipoFoto");if(!"INTERIOR".equals(tipo)&&!"EXTERIOR".equals(tipo))throw new NegocioException("Selecciona si la foto es del interior o exterior.");
                String descripcion=Web.opcional(r,"descripcionFoto");if(descripcion==null)descripcion="";if(descripcion.length()>160)throw new NegocioException("La descripción admite hasta 160 caracteres.");
                contenido().agregarFoto(usuario,id,tipo,mime,descripcion,datos);Web.flash(r,"exito","Foto agregada al inmueble.");Web.redirect(r,p,"/propietario/editar?id="+id);return;
            }
            if("/foto/eliminar".equals(ruta)) {long id=Web.id(r,"id"),foto=Web.id(r,"fotoId");contenido().eliminarFoto(usuario,foto);Web.flash(r,"exito","Foto eliminada.");Web.redirect(r,p,"/propietario/editar?id="+id);return;}
            if("/calificar".equals(ruta)){contenido().calificar(usuario,"PROPIETARIO",Web.id(r,"id"),Web.requerido(r,"puntuacion"),Web.opcional(r,"comentario")==null?"":Web.opcional(r,"comentario"));Web.flash(r,"exito","Gracias por calificar al huésped.");Web.redirect(r,p,"/propietario/reservas");return;}
            if("/bloquear".equals(ruta)) { new co.edu.rentainmuebles.service.BloqueoService().crear(usuario,Web.id(r,"inmuebleId"),Web.fecha(r,"entrada"),Web.fecha(r,"salida"),Web.texto(r,"motivo")); Web.flash(r,"exito","Bloqueo registrado."); Web.redirect(r,p,destinoBloqueos(r)); return; }
            else if("/liberar".equals(ruta)) { new co.edu.rentainmuebles.service.BloqueoService().liberar(usuario,Web.id(r,"id")); Web.flash(r,"exito","Fechas liberadas."); Web.redirect(r,p,destinoBloqueos(r)); return; }
            else if("/desactivar".equals(ruta)) { Web.servicios(r).inmuebles.desactivar(usuario,Web.id(r,"id")); Web.flash(r,"exito","Inmueble desactivado. Se conservan sus reservas."); }
            else if("/publicar".equals(ruta)||"/editar".equals(ruta)) {
                if("/publicar".equals(ruta)) Web.servicios(r).inmuebles.publicar(usuario,Web.texto(r,"titulo"),(Web.opcional(r,"ciudadId")==null?Web.texto(r,"ciudad"):"id:"+Web.id(r,"ciudadId")),Web.texto(r,"direccion"),Web.texto(r,"descripcion"),Web.requerido(r,"capacidad"),Web.dinero(r,"precio"),Web.opcional(r,"tipo"),Web.booleanoOpcional(r,"esRural"),Web.booleanoOpcional(r,"cercaMar"),Web.dinero(r,"area"),Web.entero(r,"habitaciones"));
                else Web.servicios(r).inmuebles.editar(usuario,Web.id(r,"id"),Web.texto(r,"titulo"),(Web.opcional(r,"ciudadId")==null?Web.texto(r,"ciudad"):"id:"+Web.id(r,"ciudadId")),Web.texto(r,"direccion"),Web.texto(r,"descripcion"),Web.requerido(r,"capacidad"),Web.dinero(r,"precio"),Web.opcional(r,"tipo"),Web.booleanoOpcional(r,"esRural"),Web.booleanoOpcional(r,"cercaMar"),Web.dinero(r,"area"),Web.entero(r,"habitaciones"));
                Web.flash(r,"exito","Inmueble guardado correctamente.");
            } else { p.sendError(404); return; }
        } catch(NegocioException e) {
            String ruta=r.getPathInfo();
            if("/bloquear".equals(ruta)||"/liberar".equals(ruta)){Web.errorFormulario(r,e);Web.redirect(r,p,destinoBloqueos(r));return;}
            if("/foto/subir".equals(ruta)||"/foto/eliminar".equals(ruta)){Web.errorFormulario(r,e);String id=Web.opcional(r,"id");Web.redirect(r,p,id==null?"/propietario/inmuebles":"/propietario/editar?id="+id);return;}
            if("/calificar".equals(ruta)){Web.errorFormulario(r,e);Web.redirect(r,p,"/propietario/reservas");return;}
            if("/publicar".equals(ruta)||"/editar".equals(ruta)) {
                String destino="/publicar".equals(ruta)?"/propietario/publicar":Web.rutaConId(r,"/propietario/editar","id","/propietario/inmuebles");
                Web.recordarFormulario(r,destino,"titulo","ciudad","ciudadId","direccion","descripcion","capacidad","precio","tipo","esRural","cercaMar","area","habitaciones");
                Web.errorFormulario(r,e); Web.redirect(r,p,destino); return;
            }
            Web.errorFormulario(r,e);
        }
        Web.redirect(r,p,"/propietario/inmuebles");
    }
    private static String destinoBloqueos(HttpServletRequest r) {
        String id=Web.opcional(r,"inmuebleId"),mes=Web.opcional(r,"mes");
        if(!"true".equals(Web.opcional(r,"volverCalendario"))||id==null||!id.matches("[1-9][0-9]{0,17}"))return "/propietario/bloqueos";
        return "/propietario/calendario?id="+id+(mes==null?"":"&mes="+java.net.URLEncoder.encode(mes,java.nio.charset.StandardCharsets.UTF_8));
    }
    private static co.edu.rentainmuebles.service.ContenidoInmuebleService contenido(){return new co.edu.rentainmuebles.service.ContenidoInmuebleService();}
    private static String tipoImagen(byte[] datos) throws IOException {
        try(ImageInputStream stream=ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(datos))) {
            if(stream==null)throw new NegocioException("El archivo seleccionado no es una imagen válida.");
            var lectores=ImageIO.getImageReaders(stream);if(!lectores.hasNext())throw new NegocioException("Solo se admiten imágenes JPEG o PNG.");
            ImageReader lector=lectores.next();try{lector.setInput(stream,true,true);String formato=lector.getFormatName().toLowerCase(Locale.ROOT);int w=lector.getWidth(0),h=lector.getHeight(0);if(w<1||h<1||(long)w*h>25_000_000L)throw new NegocioException("La imagen tiene dimensiones demasiado grandes.");if(formato.equals("jpeg")||formato.equals("jpg"))return "image/jpeg";if(formato.equals("png"))return "image/png";throw new NegocioException("Solo se admiten imágenes JPEG o PNG.");}finally{lector.dispose();}
        }
    }
}
