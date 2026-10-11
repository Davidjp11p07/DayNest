<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="error-page"><p class="error-code">404</p><h1>No encontramos esta página</h1><p>El enlace puede haber cambiado o el inmueble ya no está disponible.</p><a class="button" href="${fn:escapeXml(pageContext.request.contextPath)}/inicio">Volver al inicio</a></section>
<%@ include file="../layout/pie.jspf" %>
