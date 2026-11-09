<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="error-page"><h1><c:out value="${textos['ui134']}"/></h1><p><c:out value="${textos['ui135']}"/></p><div class="actions"><a class="button" href="${fn:escapeXml(pageContext.request.contextPath)}/inicio"><c:out value="${textos['ui125']}"/></a><a class="button secondary" href="${fn:escapeXml(pageContext.request.contextPath)}/login"><c:out value="${textos['ui136']}"/></a></div></section>
<%@ include file="../layout/pie.jspf" %>
