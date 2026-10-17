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
                case "/reservar" -> { r.setAttribute("inmueble",Web.servicios(r).inmuebles.obtenerPublicado(Web.id(r,"id"))); Web.vista(r,p,"cliente/reservar"); }
                case "/reservas" -> { resumenes(r,usuario); Web.vista(r,p,"cliente/reservas"); }
                case "/historial" -> { resumenes(r,usuario); r.setAttribute("pagos",Web.servicios(r).pagos.listarPropios(usuario)); Web.vista(r,p,"cliente/historial"); }
                case "/pagar" -> { long id=Web.id(r,"id"); Reserva reserva=Web.servicios(r).reservas.listarPropias(usuario).stream().filter(x->x.getId()==id).findFirst().orElseThrow(()->new NegocioException("La reserva no pertenece al cliente.")); r.setAttribute("reserva",reserva); r.setAttribute("resumen",Web.servicios(r).pagos.resumen(usuario,id)); var datos=new co.edu.rentainmuebles.service.DatosCuentaService(); r.setAttribute("cuentaRegistrada",datos.consultar(usuario,Rol.CLIENTE).cuenta()); r.setAttribute("bancos",datos.bancos()); Web.vista(r,p,"cliente/pagar"); }
                default -> p.sendError(404);
            }
        } catch(NegocioException e) { Web.errorConsulta(r,p,e); }
    }
    private static void resumenes(HttpServletRequest r,long usuario) {
        var reservas=Web.servicios(r).reservas.listarPropias(usuario);
        r.setAttribute("reservas",reservas);
        var resumenes=new java.util.HashMap<Long,co.edu.rentainmuebles.service.ResumenReserva>();
        for(Reserva reserva:reservas) resumenes.put(reserva.getId(),Web.servicios(r).pagos.resumen(usuario,reserva.getId()));
        r.setAttribute("resumenes",resumenes);
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p) throws IOException {
        String ruta=r.getPathInfo();
        try {
            long usuario=Web.usuario(r).getId();
            if("/reservar".equals(ruta)) { Reserva reserva=Web.servicios(r).reservas.crear(usuario,Web.id(r,"inmuebleId"),Web.fecha(r,"entrada"),Web.fecha(r,"salida"),Web.requerido(r,"huespedes")); Web.flash(r,"exito","Reserva creada por "+reserva.getTotal()+" "+reserva.getMoneda()+". Tiene 30 minutos para pagar (simulación)."); }
            else if("/cancelar".equals(ruta)) { Web.servicios(r).reservas.cancelar(usuario,Web.id(r,"id")); Web.flash(r,"exito","Reserva cancelada. Las fechas quedaron disponibles; los cobros se devolvieron en la simulación."); }
            else if("/pagar".equals(ruta)) {
                String opcion=Web.opcional(r,"cuentaPago");
                if(!"REGISTRADA".equals(opcion)&&!"OCASIONAL".equals(opcion))throw new NegocioException("Seleccione la cuenta registrada u otra solo para este pago.");
                String metodo=Web.opcional(r,"metodo");
                if(metodo!=null&&!"TRANSFERENCIA".equals(metodo))throw new NegocioException("Solo se admite transferencia bancaria simulada.");
                CuentaBancaria ocasional=null;
                if("OCASIONAL".equals(opcion))try{ocasional=new CuentaBancaria(Web.id(r,"bancoId"),Web.texto(r,"numeroCuenta"),Web.texto(r,"tipoCuenta"));}
                catch(IllegalArgumentException e){throw new NegocioException(e.getMessage());}
                Web.servicios(r).pagos.registrarTransferencia(usuario,Web.id(r,"id"),Web.opcional(r,"referencia"),ocasional);
                Web.flash(r,"exito","Transferencia simulada registrada y reserva confirmada. Su cuenta predeterminada no cambio.");
            }
            else { p.sendError(404); return; }
        } catch(NegocioException e) {
            if("/reservar".equals(ruta)||"/pagar".equals(ruta)) {
                String destino="/reservar".equals(ruta)?Web.rutaConId(r,"/cliente/reservar","inmuebleId","/cliente/reservas"):Web.rutaConId(r,"/cliente/pagar","id","/cliente/reservas");
                Web.recordarFormulario(r,destino,"entrada","salida","huespedes","referencia","cuentaPago","bancoId","numeroCuenta","tipoCuenta");
                Web.errorFormulario(r,e); Web.redirect(r,p,destino); return;
            }
            Web.errorFormulario(r,e);
        }
        Web.redirect(r,p,"/cliente/reservas");
    }
}
