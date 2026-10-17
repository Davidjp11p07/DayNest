<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="panel narrow"><h1>Catálogo de bancos</h1><p>Los bancos demo son ficticios. Registra los bancos necesarios con su país.</p>
<form class="form-stack" method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/admin/bancos"><input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><label for="nombre-banco">Nombre del banco</label><input id="nombre-banco" name="nombre" maxlength="160" required><label for="pais-banco">País</label><select id="pais-banco" name="paisId" required><c:forEach var="p" items="${paises}"><option value="${p.key}"><c:out value="${p.value}"/></option></c:forEach></select><button type="submit">Registrar banco</button></form>
<ul><c:forEach var="b" items="${bancos}"><li><c:out value="${b.etiqueta}"/></li></c:forEach></ul></section>
<%@ include file="../layout/pie.jspf" %>
