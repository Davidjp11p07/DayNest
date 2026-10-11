<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="error-page"><p class="error-code">403</p><h1>Acceso no permitido</h1><p>No tienes permiso para esta sección o tu formulario ha vencido. Vuelve a iniciar sesión o recarga la página.</p><a class="button" href="${fn:escapeXml(pageContext.request.contextPath)}/inicio">Volver al inicio</a></section>
<%@ include file="../layout/pie.jspf" %>
