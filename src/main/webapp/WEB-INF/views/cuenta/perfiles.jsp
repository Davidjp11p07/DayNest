<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="panel narrow">
  <h1><c:out value="${textos['ui113']}"/></h1>
  <p><c:out value="${textos['ui114']}"/></p>
  <ul>
    <li><c:out value="${textos['ui115']}"/> <strong><c:choose><c:when test="${sessionScope.usuario.cliente}"><c:out value="${textos['ui033']}"/></c:when><c:otherwise><c:out value="${textos['ui116']}"/></c:otherwise></c:choose></strong></li>
    <li><c:out value="${textos['ui117']}"/> <strong><c:choose><c:when test="${sessionScope.usuario.propietario}"><c:out value="${textos['ui033']}"/></c:when><c:otherwise><c:out value="${textos['ui116']}"/></c:otherwise></c:choose></strong></li>
  </ul>
  <c:choose>
    <c:when test="${sessionScope.usuario.cliente and not sessionScope.usuario.propietario}">
      <p><c:out value="${textos['ui118']}"/></p>
      <form method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/perfiles">
        <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
        <input type="hidden" name="perfil" value="PROPIETARIO">
        <button type="submit"><c:out value="${textos['ui119']}"/></button>
      </form>
    </c:when>
    <c:when test="${sessionScope.usuario.propietario and not sessionScope.usuario.cliente}">
      <p><c:out value="${textos['ui120']}"/></p>
      <form method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/perfiles">
        <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
        <input type="hidden" name="perfil" value="CLIENTE">
        <button type="submit"><c:out value="${textos['ui121']}"/></button>
      </form>
    </c:when>
    <c:when test="${sessionScope.usuario.cliente and sessionScope.usuario.propietario}">
      <p class="alert" role="status"><c:out value="${textos['ui122']}"/></p>
    </c:when>
    <c:otherwise><p><c:out value="${textos['ui123']}"/></p></c:otherwise>
  </c:choose>
  <p><a href="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos"><c:out value="${textos['ui124']}"/></a></p>
  <p><a href="${fn:escapeXml(pageContext.request.contextPath)}/inicio"><c:out value="${textos['ui125']}"/></a></p>
</section>
<%@ include file="../layout/pie.jspf" %>
