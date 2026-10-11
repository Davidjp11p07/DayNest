package co.edu.rentainmuebles.web;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import java.net.*;import java.net.http.*;
class WebHttpIT {
 @Test void pantallasPublicasYRecursosLocales()throws Exception{HttpClient c=HttpClient.newHttpClient();for(String ruta:new String[]{"/inicio","/buscar?ciudad=Bogota","/buscar?ciudad=Cartagena","/login","/registro","/assets/daynest.css","/assets/daynest.js","/assets/img/daynest-logo.svg"}){var r=c.send(HttpRequest.newBuilder(URI.create("http://localhost:8080/DayNest"+ruta)).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,r.statusCode(),ruta);assertFalse(r.body().contains("Exception"),ruta);}}
}
