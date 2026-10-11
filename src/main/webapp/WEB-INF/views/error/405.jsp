<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="error-page"><p class="error-code">405</p><h1>Usa el formulario de la página</h1><p>Esta acción requiere enviar un formulario. Vuelve a la página anterior para continuar.</p><a class="button" href="${fn:escapeXml(pageContext.request.contextPath)}/inicio">Volver al inicio</a></section>
<%@ include file="../layout/pie.jspf" %>
