package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
@WebServlet(urlPatterns={"/admin","/admin/*"})
public final class AdministradorServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse p) throws ServletException,IOException {
        String ruta=r.getPathInfo()==null||"/".equals(r.getPathInfo())?"/panel":r.getPathInfo();
        try {
            long usuario=Web.usuario(r).getId(); AdministracionService s=Web.servicios(r).administracion;
            switch(ruta) {
                case "/historial-anterior" -> { r.setAttribute("historialAnterior",new co.edu.rentainmuebles.service.HistorialAnteriorService().consultar(usuario)); Web.vista(r,p,"admin/historial-anterior"); }
                case "/bancos" -> { r.setAttribute("bancos",new DatosCuentaService().bancos());r.setAttribute("paises",Web.servicios(r).catalogo.paises());Web.vista(r,p,"admin/bancos"); }
                case "/panel" -> {
                    List<Inmueble> inmuebles=s.listarInmuebles(usuario,null); List<Reserva> reservas=s.listarReservas(usuario,null);
                    List<Pago> pagos=s.listarPagos(usuario,null); List<Giro> giros=s.listarGiros(usuario,null);
                    r.setAttribute("totalInmuebles",inmuebles.size()); r.setAttribute("totalReservas",reservas.size());
                    r.setAttribute("totalPagos",pagos.size()); r.setAttribute("totalGiros",giros.size());
                    r.setAttribute("pagosPorMoneda",pagos.stream().filter(x->x.getEstado()==EstadoPago.APROBADO).collect(java.util.stream.Collectors.groupingBy(Pago::getMoneda,java.util.stream.Collectors.reducing(BigDecimal.ZERO,x->x.getClase().equals("COBRO")?x.getMonto():x.getMonto().negate(),BigDecimal::add))));
                    r.setAttribute("girosPorMoneda",giros.stream().filter(x->x.getEstado()==EstadoGiro.EJECUTADO).collect(java.util.stream.Collectors.groupingBy(Giro::getMoneda,java.util.stream.Collectors.reducing(BigDecimal.ZERO,Giro::getMonto,BigDecimal::add))));
                    Web.vista(r,p,"admin/panel");
                }
                case "/inmuebles" -> { r.setAttribute("inmuebles",s.listarInmuebles(usuario,Web.estado(r,"estado",EstadoInmueble.class))); Web.vista(r,p,"admin/inmuebles"); }
                case "/reservas" -> {
                    r.setAttribute("reservas",s.listarReservas(usuario,Web.estado(r,"estado",EstadoReserva.class)));
                    var hoy=Web.servicios(r).hoy();
                    var giradas=s.listarGiros(usuario,null).stream().filter(g->g.getEstado()!=EstadoGiro.FALLIDO).map(g->g.getReserva().getId()).collect(java.util.stream.Collectors.toSet());
                    var pagadas=s.listarPagos(usuario,null).stream().filter(x->x.getEstado()==EstadoPago.APROBADO)
                            .collect(java.util.stream.Collectors.groupingBy(x->x.getReserva().getId(),java.util.stream.Collectors.reducing(BigDecimal.ZERO,x->x.getClase().equals("COBRO")?x.getMonto():x.getMonto().negate(),BigDecimal::add)));
                    r.setAttribute("girables",s.listarReservas(usuario,null).stream().filter(x->!x.getPeriodo().getSalida().isAfter(hoy)&&(x.getEstado()==EstadoReserva.CONFIRMADA||x.getEstado()==EstadoReserva.FINALIZADA)&&pagadas.getOrDefault(x.getId(),BigDecimal.ZERO).compareTo(x.getTotal())>=0&&!giradas.contains(x.getId())).map(Reserva::getId).toList());
                    r.setAttribute("girosAutomaticos",new ComisionService().disponible());
                    Web.vista(r,p,"admin/reservas");
                }
                case "/pagos" -> { r.setAttribute("pagos",s.listarPagos(usuario,Web.estado(r,"estado",EstadoReserva.class))); Web.vista(r,p,"admin/pagos"); }
                case "/giros" -> { var comisiones=new ComisionService();if(comisiones.disponible()){r.setAttribute("comisionActual",comisiones.porcentaje(usuario).movePointRight(2));r.setAttribute("liquidaciones",comisiones.liquidaciones(usuario,true));r.setAttribute("finanzasDisponibles",true);} r.setAttribute("giros",s.listarGiros(usuario,Web.estado(r,"estado",EstadoReserva.class))); Web.vista(r,p,"admin/giros"); }
                default -> p.sendError(404);
            }
        } catch(NegocioException e) { Web.errorConsulta(r,p,e); }
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse p) throws IOException {
        String destino="/admin/reservas";
        try {
            long usuario=Web.usuario(r).getId(); AdministracionService s=Web.servicios(r).administracion;
            String ruta=r.getPathInfo();
            if("/bancos".equals(ruta)) { destino="/admin/bancos";new DatosCuentaService().crearBanco(usuario,Web.texto(r,"nombre"),Web.id(r,"paisId"));Web.flash(r,"exito","Banco registrado."); }
            else if("/comision".equals(ruta)){destino="/admin/giros";new ComisionService().cambiar(usuario,Web.dinero(r,"porcentaje"));Web.flash(r,"exito","Comision actualizada para los siguientes pagos; el historial se conserva.");}
            else if("/reintentar-liquidacion".equals(ruta)){destino="/admin/giros";new ComisionService().reintentar(usuario,Web.id(r,"id"));Web.flash(r,"exito","Revision completada. Consulte el estado y el motivo de la liquidacion.");}
            else if("/generar-giro".equals(ruta)) { s.finalizarYGenerarGiro(usuario,Web.id(r,"id"),Web.opcional(r,"referencia")); Web.flash(r,"exito","Estadía finalizada y giro generado correctamente."); }
            else if("/cancelar".equals(ruta)) { s.cancelarReserva(usuario,Web.id(r,"id")); Web.flash(r,"exito","Reserva cancelada."); }
            else if("/desactivar".equals(ruta)) { destino="/admin/inmuebles"; s.desactivarInmueble(usuario,Web.id(r,"id")); Web.flash(r,"exito","Inmueble desactivado."); }
            else { p.sendError(404); return; }
        } catch(NegocioException e) { Web.errorFormulario(r,e); }
        Web.redirect(r,p,destino);
    }
}
