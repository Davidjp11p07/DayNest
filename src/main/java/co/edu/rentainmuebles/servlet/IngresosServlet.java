package co.edu.rentainmuebles.servlet;

import co.edu.rentainmuebles.service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/admin/ingresos","/propietario/ingresos"})
public final class IngresosServlet extends HttpServlet {
 @Override protected void doGet(HttpServletRequest r,HttpServletResponse p)throws ServletException,IOException {
  try {
   boolean admin=r.getServletPath().startsWith("/admin/");
   long filtro=Web.opcional(r,"inmuebleId")==null?0:Web.id(r,"inmuebleId");
   int pagina=1;
   if(Web.opcional(r,"pagina")!=null)try{pagina=Integer.parseInt(Web.opcional(r,"pagina"));}catch(NumberFormatException e){throw new NegocioException("La pagina debe ser un numero entero.");}
   var informe=new IngresosService().consultar(Web.usuario(r).getId(),admin,filtro,pagina);
   r.setAttribute("ingresos",informe);r.setAttribute("ingresosAdmin",admin);
   r.setAttribute("rutaIngresos",r.getContextPath()+(admin?"/admin/ingresos":"/propietario/ingresos"));
   Web.vista(r,p,"layout/ingresos");
  }catch(NegocioException e){Web.errorConsulta(r,p,e);}
 }
}
