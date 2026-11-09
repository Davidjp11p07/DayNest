<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="panel narrow"><h1><c:out value="${textos['ui100']}"/></h1><p><c:out value="${textos['ui101']}"/></p>
<p class="muted"><c:out value="${textos['ui102']}"/></p>
<c:if test="${not empty datosCliente}"><section><h2><c:out value="${textos['ui025']}"/></h2><p><c:out value="${datosCliente.nombre}"/> · <c:out value="${datosCliente.correo}"/></p>
<c:if test="${empty datosCliente.cuenta}"><p role="status"><c:out value="${textos['ui103']}"/></p></c:if>
<form class="form-stack" method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos" data-cuenta="CLIENTE">
<input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><input type="hidden" name="perfil" value="CLIENTE">
<div class="field"><label for="banco-CLIENTE"><c:out value="${textos['ui062']}"/></label><select id="banco-CLIENTE" name="bancoId" required><option value=""><c:out value="${textos['ui063']}"/></option><c:forEach var="b" items="${bancos}"><option value="${b.id}" ${datosCliente.cuenta.bancoId eq b.id ? 'selected' : ''}><c:out value="${b.etiqueta}"/></option></c:forEach></select></div>
<div class="field"><label for="numero-CLIENTE"><c:out value="${textos['ui064']}"/></label><input id="numero-CLIENTE" name="numeroCuenta" type="text" inputmode="numeric" pattern="[0-9]{4,34}" minlength="4" maxlength="34" value="${fn:escapeXml(datosCliente.cuenta.numeroCuenta)}" required autocomplete="off"><small><c:out value="${textos['ui104']}"/></small></div>
<div class="field"><label for="tipo-CLIENTE"><c:out value="${textos['ui065']}"/></label><select id="tipo-CLIENTE" name="tipoCuenta" required><option value=""><c:out value="${textos['ui066']}"/></option><option value="AHORROS" ${datosCliente.cuenta.tipoCuenta eq 'AHORROS' ? 'selected' : ''}><c:out value="${textos['ui067']}"/></option><option value="CORRIENTE" ${datosCliente.cuenta.tipoCuenta eq 'CORRIENTE' ? 'selected' : ''}><c:out value="${textos['ui068']}"/></option></select></div>
<c:if test="${not empty datosCliente and not empty datosPropietario}"><label><input type="checkbox" name="ambos" value="true" style="width:auto;min-height:auto"> <c:out value="${textos['ui105']}"/></label></c:if>
<button type="submit"><c:out value="${textos['ui106']}"/></button></form></section>
</c:if>
<c:if test="${not empty datosPropietario}"><section><h2><c:out value="${textos['ui036']}"/></h2><p><c:out value="${datosPropietario.nombre}"/> · <c:out value="${datosPropietario.correo}"/></p>
<c:if test="${empty datosPropietario.cuenta}"><p role="status"><c:out value="${textos['ui103']}"/></p></c:if>
<form class="form-stack" method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos" data-cuenta="PROPIETARIO">
<input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><input type="hidden" name="perfil" value="PROPIETARIO">
<div class="field"><label for="banco-PROPIETARIO"><c:out value="${textos['ui062']}"/></label><select id="banco-PROPIETARIO" name="bancoId" required><option value=""><c:out value="${textos['ui063']}"/></option><c:forEach var="b" items="${bancos}"><option value="${b.id}" ${datosPropietario.cuenta.bancoId eq b.id ? 'selected' : ''}><c:out value="${b.etiqueta}"/></option></c:forEach></select></div>
<div class="field"><label for="numero-PROPIETARIO"><c:out value="${textos['ui064']}"/></label><input id="numero-PROPIETARIO" name="numeroCuenta" type="text" inputmode="numeric" pattern="[0-9]{4,34}" minlength="4" maxlength="34" value="${fn:escapeXml(datosPropietario.cuenta.numeroCuenta)}" required autocomplete="off"><small><c:out value="${textos['ui104']}"/></small></div>
<div class="field"><label for="tipo-PROPIETARIO"><c:out value="${textos['ui065']}"/></label><select id="tipo-PROPIETARIO" name="tipoCuenta" required><option value=""><c:out value="${textos['ui066']}"/></option><option value="AHORROS" ${datosPropietario.cuenta.tipoCuenta eq 'AHORROS' ? 'selected' : ''}><c:out value="${textos['ui067']}"/></option><option value="CORRIENTE" ${datosPropietario.cuenta.tipoCuenta eq 'CORRIENTE' ? 'selected' : ''}><c:out value="${textos['ui068']}"/></option></select></div>
<c:if test="${not empty datosCliente and not empty datosPropietario}"><label><input type="checkbox" name="ambos" value="true" style="width:auto;min-height:auto"> <c:out value="${textos['ui105']}"/></label></c:if>
<button type="submit"><c:out value="${textos['ui107']}"/></button></form></section>
</c:if>
<c:if test="${not empty datosCliente}"><section><h2><c:out value="${textos['ui108']}"/></h2>
<form class="form-stack" method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos" data-residencia>
<input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><input type="hidden" name="accion" value="residencia">
<label for="residencia"><c:out value="${textos['ui109']}"/></label><select id="residencia" name="ciudadId" required><option value=""><c:out value="${textos['ui110']}"/></option><c:forEach var="c" items="${ciudades}"><option value="${c.id}" ${datosCliente.ciudadId eq c.id ? 'selected' : ''}><c:out value="${c.etiqueta}"/></option></c:forEach></select><button type="submit"><c:out value="${textos['ui111']}"/></button></form></section></c:if>
<p><a href="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/perfiles"><c:out value="${textos['ui112']}"/></a></p></section>
<%@ include file="../layout/pie.jspf" %>
