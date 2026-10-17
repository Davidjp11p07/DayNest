<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layout/cabecera.jspf" %>
<section class="panel narrow"><h1>Mis datos y cuentas</h1><p>Cliente y propietario tienen datos bancarios independientes. Puedes modificarlos o guardar los mismos en ambos perfiles.</p>
<p class="muted">Los cobros y giros siguen siendo simulados. Usa datos ficticios para la demostración.</p>
<c:if test="${not empty datosCliente}"><section><h2>Cliente</h2><p><c:out value="${datosCliente.nombre}"/> · <c:out value="${datosCliente.correo}"/></p>
<c:if test="${empty datosCliente.cuenta}"><p role="status">Cuenta bancaria pendiente de completar.</p></c:if>
<form class="form-stack" method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos" data-cuenta="CLIENTE">
<input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><input type="hidden" name="perfil" value="CLIENTE">
<div class="field"><label for="banco-CLIENTE">Banco</label><select id="banco-CLIENTE" name="bancoId" required><option value="">Selecciona un banco y país</option><c:forEach var="b" items="${bancos}"><option value="${b.id}" ${datosCliente.cuenta.bancoId eq b.id ? 'selected' : ''}><c:out value="${b.etiqueta}"/></option></c:forEach></select></div>
<div class="field"><label for="numero-CLIENTE">Número de cuenta</label><input id="numero-CLIENTE" name="numeroCuenta" type="text" inputmode="numeric" pattern="[0-9]{4,34}" minlength="4" maxlength="34" value="${fn:escapeXml(datosCliente.cuenta.numeroCuenta)}" required autocomplete="off"><small>De 4 a 34 dígitos. Conserva los ceros iniciales.</small></div>
<div class="field"><label for="tipo-CLIENTE">Tipo de cuenta</label><select id="tipo-CLIENTE" name="tipoCuenta" required><option value="">Selecciona un tipo</option><option value="AHORROS" ${datosCliente.cuenta.tipoCuenta eq 'AHORROS' ? 'selected' : ''}>Ahorros</option><option value="CORRIENTE" ${datosCliente.cuenta.tipoCuenta eq 'CORRIENTE' ? 'selected' : ''}>Corriente</option></select></div>
<c:if test="${not empty datosCliente and not empty datosPropietario}"><label><input type="checkbox" name="ambos" value="true" style="width:auto;min-height:auto"> Guardar este banco, número y tipo de cuenta en ambos perfiles</label></c:if>
<button type="submit">Guardar cuenta de Cliente</button></form></section>
</c:if>
<c:if test="${not empty datosPropietario}"><section><h2>Propietario</h2><p><c:out value="${datosPropietario.nombre}"/> · <c:out value="${datosPropietario.correo}"/></p>
<c:if test="${empty datosPropietario.cuenta}"><p role="status">Cuenta bancaria pendiente de completar.</p></c:if>
<form class="form-stack" method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos" data-cuenta="PROPIETARIO">
<input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><input type="hidden" name="perfil" value="PROPIETARIO">
<div class="field"><label for="banco-PROPIETARIO">Banco</label><select id="banco-PROPIETARIO" name="bancoId" required><option value="">Selecciona un banco y país</option><c:forEach var="b" items="${bancos}"><option value="${b.id}" ${datosPropietario.cuenta.bancoId eq b.id ? 'selected' : ''}><c:out value="${b.etiqueta}"/></option></c:forEach></select></div>
<div class="field"><label for="numero-PROPIETARIO">Número de cuenta</label><input id="numero-PROPIETARIO" name="numeroCuenta" type="text" inputmode="numeric" pattern="[0-9]{4,34}" minlength="4" maxlength="34" value="${fn:escapeXml(datosPropietario.cuenta.numeroCuenta)}" required autocomplete="off"><small>De 4 a 34 dígitos. Conserva los ceros iniciales.</small></div>
<div class="field"><label for="tipo-PROPIETARIO">Tipo de cuenta</label><select id="tipo-PROPIETARIO" name="tipoCuenta" required><option value="">Selecciona un tipo</option><option value="AHORROS" ${datosPropietario.cuenta.tipoCuenta eq 'AHORROS' ? 'selected' : ''}>Ahorros</option><option value="CORRIENTE" ${datosPropietario.cuenta.tipoCuenta eq 'CORRIENTE' ? 'selected' : ''}>Corriente</option></select></div>
<c:if test="${not empty datosCliente and not empty datosPropietario}"><label><input type="checkbox" name="ambos" value="true" style="width:auto;min-height:auto"> Guardar este banco, número y tipo de cuenta en ambos perfiles</label></c:if>
<button type="submit">Guardar cuenta de Propietario</button></form></section>
</c:if>
<c:if test="${not empty datosCliente}"><section><h2>Ciudad de residencia del cliente</h2>
<form class="form-stack" method="post" action="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/datos" data-residencia>
<input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}"><input type="hidden" name="accion" value="residencia">
<label for="residencia">Ciudad y país</label><select id="residencia" name="ciudadId" required><option value="">Pendiente de completar</option><c:forEach var="c" items="${ciudades}"><option value="${c.id}" ${datosCliente.ciudadId eq c.id ? 'selected' : ''}><c:out value="${c.etiqueta}"/></option></c:forEach></select><button type="submit">Guardar residencia</button></form></section></c:if>
<p><a href="${fn:escapeXml(pageContext.request.contextPath)}/cuenta/perfiles">Gestionar mis perfiles</a></p></section>
<%@ include file="../layout/pie.jspf" %>
