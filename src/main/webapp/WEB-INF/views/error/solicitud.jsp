<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="error-page"><h1>No pudimos completar la consulta</h1><p>Revisa los datos de la solicitud o vuelve a iniciar sesión para continuar.</p><div class="actions"><a class="button" href="${fn:escapeXml(pageContext.request.contextPath)}/inicio">Volver al inicio</a><a class="button secondary" href="${fn:escapeXml(pageContext.request.contextPath)}/login">Ir a mi cuenta</a></div></section>
<%@ include file="../layout/pie.jspf" %>
