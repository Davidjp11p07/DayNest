package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.model.*;
import co.edu.rentainmuebles.service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/cuenta/datos")
public final class DatosCuentaServlet extends HttpServlet {
 private final DatosCuentaService datos=new DatosCuentaService();
 @Override protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException {
  try {
   long id=Web.usuario(r).getId();
   // El servicio comprueba de nuevo permisos activos en la base.
   r.setAttribute("bancos",datos.bancos());r.setAttribute("ciudades",Web.servicios(r).catalogo.ciudades());
   if(Web.usuario(r).isCliente())r.setAttribute("datosCliente",datos.consultar(id,Rol.CLIENTE));
   if(Web.usuario(r).isPropietario())r.setAttribute("datosPropietario",datos.consultar(id,Rol.PROPIETARIO));
   Web.vista(r,p,"cuenta/datos");
  }catch(NegocioException e){Web.errorConsulta(r,p,e);}
 }
 @Override protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException {
  try {
   long id=Web.usuario(r).getId();
   if("residencia".equals(r.getParameter("accion")))datos.residencia(id,Web.id(r,"ciudadId"));
   else {
    CuentaBancaria cuenta;
    try{cuenta=new CuentaBancaria(Web.id(r,"bancoId"),Web.texto(r,"numeroCuenta"),Web.texto(r,"tipoCuenta"));}
    catch(IllegalArgumentException e){throw new NegocioException(e.getMessage());}
    datos.guardar(id,Web.estado(r,"perfil",Rol.class),cuenta,"true".equals(r.getParameter("ambos")));
   }
   Web.flash(r,"exito","Datos guardados correctamente.");
  }catch(NegocioException e){Web.errorFormulario(r,e);}
  Web.redirect(r,p,"/cuenta/datos");
 }
}
