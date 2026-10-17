"""Supervision y comision editables; solo permite el esquema HTTP UUID de pruebas."""
from pathlib import Path
import sys,re,subprocess
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'.tools/daynest-python'))
from playwright.sync_api import sync_playwright
BASE='http://localhost:8081/DayNest'
schema=(ROOT/'.tools/perfiles-http-schema.txt').read_text().strip()
assert re.fullmatch(r'renta_http_[a-f0-9]{32}',schema)
cmd=subprocess.check_output(['powershell','-NoProfile','-Command',"$p=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction Stop; (Get-CimInstance Win32_Process -Filter \"ProcessId=$($p[0].OwningProcess)\").CommandLine"],text=True)
assert '-Drenta.db.schema='+schema in cmd
pasada,futura,pendiente=(ROOT/'.tools/giros-http-reservas.txt').read_text().strip().split(',')
checks=[];errors=[]
with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe',headless=True)
 ctx=browser.new_context(locale='es-CO',timezone_id='America/Bogota')
 ctx.route('**/*',lambda r:r.continue_() if r.request.url.startswith('http://localhost:8081/') else r.abort())
 page=ctx.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
 def go(path):
  r=page.goto(BASE+path,wait_until='networkidle');assert r.status==200,(path,r.status);return r
 def login(email,password):
  go('/login');page.locator('[name=correo]').fill(email);page.locator('[name=password]').fill(password);page.locator('form[action$="/login"] button[type=submit]').click();page.wait_for_load_state('networkidle');assert '/login' not in page.url
 def salir():
  page.locator('form[action$="/salir"]').evaluate('(f)=>f.requestSubmit()');page.wait_for_url('**/login')
 login('admin@renta.test','Admin2026!');go('/admin/reservas')
 assert page.locator('form[action$="/admin/generar-giro"]').count()==0;checks.append('Reservas informa flujo automatico sin exigir generar cada giro')
 go('/admin/giros')
 tabla=page.locator('table').first
 assert 'EJECUTADA' in tabla.locator('tr').filter(has=page.locator('td').filter(has_text=re.compile('^#'+pasada+'$'))).inner_text()
 assert 'PENDIENTE' in tabla.locator('tr').filter(has=page.locator('td').filter(has_text=re.compile('^#'+futura+'$'))).inner_text()
 assert 'cuenta bancaria' in tabla.locator('tr').filter(has=page.locator('td').filter(has_text=re.compile('^#'+pendiente+'$'))).inner_text()
 assert 'SISTEMA' in page.locator('main').inner_text();checks.append('Arranque liquida estancia terminada, conserva futura y registra falta de cuenta')
 page.locator('[name=porcentaje]').fill('37.50');page.locator('form[action$="/admin/comision"] button').click();page.wait_for_load_state('networkidle');assert float(page.locator('[name=porcentaje]').input_value())==37.5
 assert '30' in tabla.locator('tr').filter(has=page.locator('td').filter(has_text=re.compile('^#'+pasada+'$'))).inner_text();checks.append('Administrador edita comision sin alterar operaciones ya pagadas')
 csrf=page.locator('form[action$="/admin/comision"] [name=csrf]').input_value()
 r=ctx.request.post(BASE+'/admin/comision',form={'csrf':csrf,'porcentaje':'100'});assert r.status==200
 go('/admin/giros');assert float(page.locator('[name=porcentaje]').input_value())==37.5;checks.append('Porcentaje invalido no se guarda')
 fila=tabla.locator('tr').filter(has=page.locator('td').filter(has_text=re.compile('^#'+pendiente+'$')))
 fila.get_by_role('button',name='Revisar ahora').click();page.wait_for_load_state('networkidle');assert 'cuenta bancaria' in fila.inner_text();checks.append('Revision manual informa el motivo sin fingir un giro ejecutado')
 for width in [390,768,1440]:
  page.set_viewport_size({'width':width,'height':900});assert page.evaluate('document.documentElement.scrollWidth')<=width+1
 checks.append('Pantalla de liquidaciones sin desbordamiento del documento en tres tamanos')
 salir();login('propietario1@renta.test','Propietario2026!');go('/propietario/giros')
 assert 'SISTEMA' in page.locator('main').inner_text() and '30' in page.locator('main').inner_text();assert page.locator('[name=porcentaje]').count()==0
 csrf=page.locator('form[action$="/salir"] [name=csrf]').input_value();assert ctx.request.post(BASE+'/admin/comision',form={'csrf':csrf,'porcentaje':'5'}).status==403
 checks.append('Propietario consulta neto, destino y comision; no puede cambiar la tasa')
 assert not errors,errors
 browser.close()
for check in checks:print('OK:',check)
print(f'OK: {len(checks)} comprobaciones financieras web; sin errores JavaScript')
