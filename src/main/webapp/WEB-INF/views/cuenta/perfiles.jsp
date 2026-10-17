<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="panel narrow">
  <h1>Mis perfiles</h1>
  <p>Una cuenta, dos maneras de vivir DayNest. Conserva tus reservas y alojamientos al activar el perfil complementario.</p>
  <ul>
    <li>Cliente: <strong><c:choose><c:when test="${sessionScope.usuario.cliente}">Activo</c:when><c:otherwise>Sin activar</c:otherwise></c:choose></strong></li>
    <li>Propietario: <strong><c:choose><c:when test="${sessionScope.usuario.propietario}">Activo</c:when><c:otherwise>Sin activar</c:otherwise></c:choose></strong></li>
  </ul>
  <c:choose>
    <c:when test="${sessionScope.usuario.cliente and not sessionScope.usuario.propietario}">
      <p>Activa el perfil propietario para publicar tus alojamientos. Seguirás teniendo acceso a tus reservas como cliente.</p>
      <form method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/perfiles">
        <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
        <input type="hidden" name="perfil" value="PROPIETARIO">
        <button type="submit">Activar perfil propietario</button>
      </form>
    </c:when>
    <c:when test="${sessionScope.usuario.propietario and not sessionScope.usuario.cliente}">
      <p>Activa el perfil cliente para reservar alojamientos. Seguirás gestionando tus inmuebles como propietario.</p>
      <form method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/perfiles">
        <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
        <input type="hidden" name="perfil" value="CLIENTE">
        <button type="submit">Activar perfil cliente</button>
      </form>
    </c:when>
    <c:when test="${sessionScope.usuario.cliente and sessionScope.usuario.propietario}">
      <p class="alert" role="status">Ya tienes ambos perfiles activos.</p>
    </c:when>
    <c:otherwise><p>No tienes un perfil cliente o propietario desde el que activar el complementario.</p></c:otherwise>
  </c:choose>
  <p><a href="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos">Configurar datos y cuentas bancarias</a></p>
  <p><a href="${fn:escapeXml(pageContext.request.contextPath)}/inicio">Volver al inicio</a></p>
</section>
<%@ include file="../layout/pie.jspf" %>
