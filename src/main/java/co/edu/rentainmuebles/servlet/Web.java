package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.config.Servicios;
import co.edu.rentainmuebles.service.NegocioException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.security.SecureRandom;
import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import co.edu.rentainmuebles.model.Identificable;

public final class Web {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Web() { }
    public static Servicios servicios(HttpServletRequest r) { return (Servicios) r.getServletContext().getAttribute("servicios"); }
    public static UsuarioSesion usuario(HttpServletRequest r) { HttpSession s=r.getSession(false); return s==null?null:(UsuarioSesion)s.getAttribute("usuario"); }
    public static String token(HttpSession s) {
        synchronized(s) {
            String token=(String)s.getAttribute("csrf");
            if(token==null) { byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes); token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); s.setAttribute("csrf",token); }
            return token;
        }
    }
    public static void vista(HttpServletRequest r,HttpServletResponse p,String nombre) throws ServletException,IOException {
        r.setAttribute("hoy",servicios(r).hoy());
        r.setAttribute("minEntrada",servicios(r).hoy().plusDays(1));
        recuperarFormulario(r);
        if(r.getAttribute("form")==null) {
            Map<String,String> seleccion=new HashMap<>();
            for(String campo:List.of("entrada","salida","huespedes")) {
                String valor=opcional(r,campo);
                if(valor!=null) seleccion.put(campo,valor);
            }
            if(!seleccion.containsKey("huespedes") && opcional(r,"capacidad")!=null)
                seleccion.put("huespedes",opcional(r,"capacidad"));
            r.setAttribute("form",seleccion);
        }
        for(String lista:List.of("inmuebles","reservas","pagos","giros")) {
            if(r.getAttribute(lista) instanceof List<?> datos) paginar(r,lista,datos);
        }
        r.getRequestDispatcher("/WEB-INF/views/"+nombre+".jsp").forward(r,p);
    }
    private static void paginar(HttpServletRequest r,String nombre,List<?> datos) {
        if(!"inmuebles".equals(nombre)) datos=datos.stream().sorted(Comparator.comparing(x->((Identificable)x).getId(),Comparator.reverseOrder())).toList();
        String parametro="pagina"+Character.toUpperCase(nombre.charAt(0))+nombre.substring(1);
        int paginas=Math.max(1,(datos.size()+9)/10),numero=1;
        try { numero=Integer.parseInt(r.getParameter(parametro)); } catch(NumberFormatException e) { /* Página inicial. */ }
        numero=Math.max(1,Math.min(numero,paginas)); int inicio=(numero-1)*10,fin=Math.min(inicio+10,datos.size());
        r.setAttribute(nombre,datos.subList(inicio,fin));
        r.setAttribute("nav"+Character.toUpperCase(nombre.charAt(0))+nombre.substring(1),
                new Paginacion(numero,paginas,datos.size(),datos.isEmpty()?0:inicio+1,fin,
                    numero>1?urlPagina(r,parametro,numero-1):null,numero<paginas?urlPagina(r,parametro,numero+1):null));
    }
    private static String urlPagina(HttpServletRequest r,String parametro,int numero) {
        List<String> query=new ArrayList<>();
        r.getParameterMap().forEach((clave,valores)->{ if(!clave.equals(parametro)) for(String v:valores) query.add(URLEncoder.encode(clave,StandardCharsets.UTF_8)+"="+URLEncoder.encode(v,StandardCharsets.UTF_8)); });
        query.add(parametro+"="+numero);
        return r.getRequestURI()+"?"+String.join("&",query);
    }
    public static void recordarFormulario(HttpServletRequest r,String destino,String... campos) {
        Map<String,String> valores=new HashMap<>();
        for(String campo:campos) { String valor=r.getParameter(campo); if(valor!=null) valores.put(campo,valor); }
        r.getSession().setAttribute("formDestino",destino);
        r.getSession().setAttribute("formValores",valores);
    }
    private static void recuperarFormulario(HttpServletRequest r) {
        HttpSession s=r.getSession(false); if(s==null) return;
        String actual=r.getServletPath()+(r.getPathInfo()==null?"":r.getPathInfo());
        String destino=(String)s.getAttribute("formDestino");
        String conId=actual+(r.getParameter("id")==null?"":"?id="+r.getParameter("id"));
        if(actual.equals(destino)||conId.equals(destino)) {
            r.setAttribute("form",s.getAttribute("formValores")); s.removeAttribute("formDestino"); s.removeAttribute("formValores");
        }
    }
    /** Retorno construido exclusivamente con una ruta local fija y parametros codificados. */
    public static String destinoReserva(HttpServletRequest r) {
        String destino="/cliente/reservar?id="+id(r,"id");
        for(String campo:List.of("entrada","salida","huespedes")) {
            String valor=opcional(r,campo);
            if(valor!=null) destino+="&"+campo+"="+URLEncoder.encode(valor,StandardCharsets.UTF_8);
        }
        return destino;
    }
    public static String rutaConId(HttpServletRequest r,String ruta,String campo,String alternativa) {
        try { return ruta+"?id="+id(r,campo); } catch(NegocioException e) { return alternativa; }
    }
    public static void redirect(HttpServletRequest r,HttpServletResponse p,String ruta) {
        p.setStatus(HttpServletResponse.SC_SEE_OTHER); p.setHeader("Location",r.getContextPath()+ruta);
    }
    public static void flash(HttpServletRequest r,String tipo,String mensaje) { r.getSession().setAttribute("flashTipo",tipo); r.getSession().setAttribute("flashMensaje",mensaje); }
    public static String texto(HttpServletRequest r,String nombre) { return r.getParameter(nombre); }
    public static String opcional(HttpServletRequest r,String nombre) { String v=texto(r,nombre); return v==null||v.isBlank()?null:v.trim(); }
    public static long id(HttpServletRequest r,String nombre) {
        try { long id=Long.parseLong(texto(r,nombre)); if(id<=0) throw new NumberFormatException(); return id; }
        catch(NumberFormatException e) { throw new NegocioException("El identificador no es válido."); }
    }
    public static Integer entero(HttpServletRequest r,String nombre) { String v=opcional(r,nombre); try { return v==null?null:Integer.valueOf(v); } catch(NumberFormatException e) { throw new NegocioException("La capacidad o cantidad de huéspedes debe ser un número entero."); } }
    public static int requerido(HttpServletRequest r,String nombre) { Integer n=entero(r,nombre); if(n==null) throw new NegocioException("Indique la capacidad o cantidad de huéspedes."); return n; }
    public static Boolean booleanoOpcional(HttpServletRequest r,String nombre){String v=opcional(r,nombre);if(v==null)return null;if("true".equals(v))return true;if("false".equals(v))return false;throw new NegocioException("Clasificación inválida: "+nombre);}
    public static Long idOpcional(HttpServletRequest r,String nombre){return opcional(r,nombre)==null?null:id(r,nombre);}
    public static BigDecimal dinero(HttpServletRequest r,String nombre) { String v=opcional(r,nombre); try { return v==null?null:new BigDecimal(v); } catch(NumberFormatException e) { throw new NegocioException("Indique un precio válido."); } }
    public static LocalDate fecha(HttpServletRequest r,String nombre) { String v=opcional(r,nombre); try { return v==null?null:LocalDate.parse(v); } catch(java.time.format.DateTimeParseException e) { throw new NegocioException("Indique una fecha válida."); } }
    public static <E extends Enum<E>> E estado(HttpServletRequest r,String nombre,Class<E> tipo) { String v=opcional(r,nombre); try { return v==null?null:Enum.valueOf(tipo,v); } catch(IllegalArgumentException e) { throw new NegocioException("El estado seleccionado no es válido."); } }
    public static void errorFormulario(HttpServletRequest r,NegocioException e) { flash(r,"error",e.getMessage()); }
    public static void errorConsulta(HttpServletRequest r,HttpServletResponse p,NegocioException e) throws ServletException,IOException {
        p.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        r.setAttribute("error",e.getMessage());
        vista(r,p,"error/solicitud");
    }
    public static String inicioRol(UsuarioSesion u) { return u.isEsAdministrador()?"/admin":u.isPropietario()?"/propietario/inmuebles":u.isCliente()?"/cliente/reservas":"/inicio"; }
}
