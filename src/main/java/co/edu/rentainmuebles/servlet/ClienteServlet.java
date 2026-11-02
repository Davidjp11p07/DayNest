package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.NegocioException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet(urlPatterns={"/cliente","/cliente/*"})
public final class ClienteServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        String ruta=r.getPathInfo()==null||"/".equals(r.getPathInfo())?"/reservas":r.getPathInfo();
        try {
            long usuario=Web.usuario(r).getId();
            switch(ruta) {
                case "/reservar" -> { r.setAttribute("inmueble",Web.servicios(r).inmuebles.obtenerPublicado(Web.id(r,"id"))); var datos=new co.edu.rentainmuebles.service.DatosCuentaService(); r.setAttribute("cuentaRegistrada",datos.consultar(usuario,Rol.CLIENTE).cuenta()); r.setAttribute("bancos",datos.bancos()); Web.vista(r,p,"cliente/reservar"); }
                case "/reservas" -> { resumenes(r,usuario); Web.vista(r,p,"cliente/reservas"); }
                case "/historial" -> { resumenes(r,usuario); r.setAttribute("pagos",Web.servicios(r).pagos.listarPropios(usuario)); Web.vista(r,p,"cliente/historial"); }
                case "/pagar" -> Web.redirect(r,p,"/cliente/reservas");
                default -> p.sendError(404);
            }
        } catch(NegocioException e) { Web.errorConsulta(r,p,e); }
    }
    private static void resumenes(HttpServletRequest r,long usuario) {
        r.setAttribute("rolReservas","CLIENTE");
        var reservas=Web.servicios(r).reservas.listarPropias(usuario);
        r.setAttribute("reservas",reservas);
        var resumenes=new java.util.HashMap<Long,co.edu.rentainmuebles.service.ResumenReserva>();
        for(Reserva reserva:reservas) resumenes.put(reserva.getId(),Web.servicios(r).pagos.resumen(usuario,reserva.getId()));
        r.setAttribute("resumenes",resumenes);
        var contenido=new co.edu.rentainmuebles.dao.jdbc.ContenidoInmuebleDaoJdbc();var calificados=new java.util.HashMap<Long,Boolean>();
        for(Reserva reserva:reservas)if(reserva.getEstado()==EstadoReserva.FINALIZADA)calificados.put(reserva.getId(),contenido.yaCalifico(usuario,"CLIENTE",reserva.getId()));
        r.setAttribute("calificados",calificados);
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p) throws IOException {
        String ruta=r.getPathInfo();
        try {
            long usuario=Web.usuario(r).getId();
            if("/reservar".equals(ruta)) {
                String opcion=Web.opcional(r,"cuentaPago");
                if(!"REGISTRADA".equals(opcion)&&!"OCASIONAL".equals(opcion))throw new NegocioException("Seleccione la cuenta para el pago inmediato.");
                CuentaBancaria ocasional=null;
                if("OCASIONAL".equals(opcion))try{ocasional=new CuentaBancaria(Web.id(r,"bancoId"),Web.texto(r,"numeroCuenta"),Web.texto(r,"tipoCuenta"));}
                catch(IllegalArgumentException e){throw new NegocioException(e.getMessage());}
                Reserva reserva=Web.servicios(r).reservas.crearConPago(usuario,Web.id(r,"inmuebleId"),Web.fecha(r,"entrada"),Web.fecha(r,"salida"),Web.requerido(r,"huespedes"),Web.opcional(r,"referencia"),ocasional);
                Web.flash(r,"exito","Pago realizado por "+reserva.getTotal()+" "+reserva.getMoneda()+". Reserva confirmada (simulación).");
            }
            else if("/cancelar".equals(ruta)) { Web.servicios(r).reservas.cancelar(usuario,Web.id(r,"id")); Web.flash(r,"exito","Reserva cancelada. Las fechas quedaron disponibles; los cobros se devolvieron en la simulación."); }
            else if("/pagar".equals(ruta)) { throw new NegocioException("El pago se realiza automáticamente al reservar. Cree una nueva reserva."); }
            else if("/calificar".equals(ruta)) {new co.edu.rentainmuebles.dao.jdbc.ContenidoInmuebleDaoJdbc().calificar(usuario,"CLIENTE",Web.id(r,"id"),Web.requerido(r,"puntuacion"),Web.opcional(r,"comentario")==null?"":Web.opcional(r,"comentario"));Web.flash(r,"exito","Gracias por calificar tu estadía.");}
            else { p.sendError(404); return; }
        } catch(NegocioException e) {
            if("/reservar".equals(ruta)) {
                String destino=Web.rutaConId(r,"/cliente/reservar","inmuebleId","/cliente/reservas");
                Web.recordarFormulario(r,destino,"entrada","salida","huespedes","referencia","cuentaPago","bancoId","numeroCuenta","tipoCuenta");
                Web.errorFormulario(r,new NegocioException("Pago/reserva fallado: "+e.getMessage(),e)); Web.redirect(r,p,destino); return;
            }
            Web.errorFormulario(r,e);
        }
        Web.redirect(r,p,"/cliente/reservas");
    }
}
