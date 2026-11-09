<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<h1><c:out value="${textos['ui173']}"/></h1><p><a class="button secondary" href="${fn:escapeXml(pageContext.request.contextPath)}/propietario/ingresos"><c:out value="${textos['ui345']}"/></a></p><p class="muted"><c:out value="${textos['ui346']}"/></p><c:if test="${finanzasDisponibles}"><h2><c:out value="${textos['ui347']}"/></h2><%@ include file="../layout/liquidaciones.jspf" %></c:if><h2><c:out value="${textos['ui348']}"/></h2><%@ include file="../layout/giros.jspf" %>
<%@ include file="../layout/pie.jspf" %>
