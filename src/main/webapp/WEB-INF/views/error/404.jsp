<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="error-page"><p class="error-code">404</p><h1><c:out value="${textos['ui128']}"/></h1><p><c:out value="${textos['ui129']}"/></p><a class="button" href="${fn:escapeXml(pageContext.request.contextPath)}/inicio"><c:out value="${textos['ui125']}"/></a></section>
<%@ include file="../layout/pie.jspf" %>
