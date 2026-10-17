"""Recorrido del modelo del profesor. Solo admite el esquema HTTP temporal de pruebas."""
from pathlib import Path
import sys, re, subprocess, uuid
from decimal import Decimal
from urllib.parse import urlparse, parse_qs
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'.tools/daynest-python'))
from playwright.sync_api import sync_playwright
BASE='http://localhost:8081/DayNest'
schema=(ROOT/'.tools/perfiles-http-schema.txt').read_text().strip()
assert re.fullmatch(r'renta_http_[a-f0-9]{32}',schema)
cmd=subprocess.check_output(['powershell','-NoProfile','-Command',"$p=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction Stop; (Get-CimInstance Win32_Process -Filter \"ProcessId=$($p[0].OwningProcess)\").CommandLine"],text=True)
assert '-Drenta.db.schema='+schema in cmd
checks=[];errors=[]
with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe',headless=True)
 ctx=browser.new_context(locale='es-CO',timezone_id='America/Bogota')
 ctx.route('**/*',lambda route:route.continue_() if route.request.url.startswith('http://localhost:8081/') else route.abort())
 page=ctx.new_page();page.on('pageerror',lambda error:errors.append(str(error)))
 def go(path):
  response=page.goto(BASE+path,wait_until='networkidle');assert response.status==200,(path,response.status);return response
 def login(correo,password):
  go('/login');page.locator('[name=correo]').fill(correo);page.locator('[name=password]').fill(password);page.locator('form[action$="/login"] button[type=submit]').click();page.wait_for_load_state('networkidle');assert '/login' not in page.url
 go('/cuenta/datos');assert page.url.endswith('/login');checks.append('Pantalla de cuentas exige sesion')
 login('propietario1@renta.test','Propietario2026!');go('/cuenta/datos')
 cliente=page.locator('form[data-cuenta=CLIENTE]');propietario=page.locator('form[data-cuenta=PROPIETARIO]')
 assert cliente.count()==1 and propietario.count()==1
 banco=cliente.locator('[name=bancoId] option').nth(1).get_attribute('value')
 banco2=cliente.locator('[name=bancoId] option').nth(2).get_attribute('value')
 cliente.locator('[name=bancoId]').select_option(banco);cliente.locator('[name=numeroCuenta]').fill('0000123456');cliente.locator('[name=tipoCuenta]').select_option('AHORROS');cliente.locator('[name=ambos]').check();cliente.locator('button[type=submit]').click();page.wait_for_load_state('networkidle')
 for form in [cliente,propietario]:
  assert form.locator('[name=numeroCuenta]').input_value()=='0000123456';assert form.locator('[name=bancoId]').input_value()==banco;assert form.locator('[name=tipoCuenta]').input_value()=='AHORROS'
 checks.append('Copia banco, numero y tipo a ambas entidades conservando ceros')
 propietario.locator('[name=bancoId]').select_option(banco2);propietario.locator('[name=numeroCuenta]').fill('0000998877');propietario.locator('[name=tipoCuenta]').select_option('CORRIENTE');assert not propietario.locator('[name=ambos]').is_checked();propietario.locator('button[type=submit]').click();page.wait_for_load_state('networkidle')
 assert cliente.locator('[name=numeroCuenta]').input_value()=='0000123456';assert propietario.locator('[name=numeroCuenta]').input_value()=='0000998877';checks.append('Modificar propietario no altera cliente')
 csrf=propietario.locator('[name=csrf]').input_value()
 response=ctx.request.post(BASE+'/cuenta/datos',form={'csrf':csrf,'perfil':'PROPIETARIO','bancoId':'99999999','numeroCuenta':'12345678','tipoCuenta':'AHORROS','ambos':'true','usuarioId':'999999'})
 assert response.status==200 and 'no existe' in response.text().lower();go('/cuenta/datos')
 assert cliente.locator('[name=numeroCuenta]').input_value()=='0000123456';assert propietario.locator('[name=numeroCuenta]').input_value()=='0000998877';checks.append('Banco invalido no modifica ninguno de los perfiles')
 form=page.locator('form[data-residencia]');ciudad=form.locator('[name=ciudadId] option').nth(1).get_attribute('value');form.locator('[name=ciudadId]').select_option(ciudad);form.locator('button[type=submit]').click();page.wait_for_load_state('networkidle');assert form.locator('[name=ciudadId]').input_value()==ciudad;checks.append('Residencia del cliente persistida')
 go('/propietario/publicar')
 titulo='Piloto modelo profesor '+uuid.uuid4().hex[:8]
 for name,value in [('titulo',titulo),('direccion','Direccion de prueba'),('descripcion','Inmueble temporal del modelo del profesor'),('capacidad','4'),('precio','180000'),('area','87.50'),('habitaciones','3')]:page.locator('[name='+name+']').fill(value)
 page.locator('[name=ciudadId]').select_option(ciudad);page.locator('[name=tipo]').select_option('CASA');page.locator('[name=esRural]').select_option('false');page.locator('[name=cercaMar]').select_option('false');page.locator('button[type=submit]').filter(has_text='Guardar inmueble').click();page.wait_for_load_state('networkidle');assert '/propietario/inmuebles' in page.url
 for paso in range(30):
  tarjeta=page.locator('article.card').filter(has_text=titulo)
  if tarjeta.count():break
  siguiente=page.locator('.pagination a').filter(has_text='Siguiente');assert siguiente.count()==1; siguiente.click();page.wait_for_load_state('networkidle')
 assert tarjeta.count()==1
 enlace=tarjeta.locator('a[href*="/propietario/editar?id="]').get_attribute('href');page.goto('http://localhost:8081'+enlace,wait_until='networkidle');assert Decimal(page.locator('[name=area]').input_value())==Decimal('87.50');assert page.locator('[name=habitaciones]').input_value()=='3';inmueble=parse_qs(urlparse(page.url).query)['id'][0]
 go('/inmueble?id='+inmueble);assert '87' in page.locator('main').inner_text() and 'Habitaciones: 3' in page.locator('main').inner_text();checks.append('Publicar, editar y consultar area y habitaciones')
 go('/cuenta/datos')
 for width in [390,768,1440]:page.set_viewport_size({'width':width,'height':900});assert page.evaluate('document.documentElement.scrollWidth')<=width+1
 checks.append('Formularios de cuentas sin desbordamiento en movil, tableta y escritorio')
 csrf=cliente.locator('[name=csrf]').input_value();r=ctx.request.post(BASE+'/admin/bancos',form={'csrf':csrf,'nombre':'No autorizado','paisId':'1'});assert r.status==403;checks.append('Usuario regular no puede crear bancos')
 page.evaluate("document.querySelector('form[action$=\"/salir\"]').requestSubmit()");page.wait_for_url('**/login')
 login('admin@renta.test','Admin2026!');go('/admin/bancos');page.locator('[name=nombre]').fill('Banco piloto ficticio');page.locator('form button[type=submit]').filter(has_text='Registrar banco').click();page.wait_for_load_state('networkidle');assert page.locator('main').inner_text().count('Banco piloto ficticio')==1;checks.append('Administracion registra banco con su pais')
 assert not errors,errors
 browser.close()
for check in checks:print('OK:',check)
print(f'OK: {len(checks)} comprobaciones del modelo del profesor; sin errores JavaScript')
