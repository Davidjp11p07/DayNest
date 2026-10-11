<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="error-page"><p class="error-code">500</p><h1>No pudimos completar la solicitud</h1><p>Ocurrió un problema temporal. Vuelve al inicio e inténtalo de nuevo en unos momentos.</p><a class="button" href="${fn:escapeXml(pageContext.request.contextPath)}/inicio">Volver al inicio</a></section>
<%@ include file="../layout/pie.jspf" %>
