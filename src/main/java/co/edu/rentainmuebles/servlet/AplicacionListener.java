package co.edu.rentainmuebles.servlet;
import co.edu.rentainmuebles.config.Servicios;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
@WebListener
public final class AplicacionListener implements ServletContextListener {
    private java.util.concurrent.ScheduledExecutorService tarea;
    @Override public void contextInitialized(ServletContextEvent e) {
        e.getServletContext().setAttribute("servicios",Servicios.crear());
        e.getServletContext().getSessionCookieConfig().setAttribute("SameSite","Lax");
        tarea=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"DayNest-reservas");t.setDaemon(true);return t;});
        tarea.scheduleWithFixedDelay(()->{try{new co.edu.rentainmuebles.service.CicloReservas().ejecutar();}catch(RuntimeException ex){e.getServletContext().log("No pudo ejecutarse el ciclo de reservas",ex);}},0,60,java.util.concurrent.TimeUnit.SECONDS);
    }
    @Override public void contextDestroyed(ServletContextEvent e){if(tarea!=null){tarea.shutdownNow();try{tarea.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException ex){Thread.currentThread().interrupt();}}co.edu.rentainmuebles.config.ConexionBD.cerrar();}
}
