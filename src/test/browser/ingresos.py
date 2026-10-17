"""Recorrido de ingresos en 8081. Crear el fixture con PrepararIngresosHttp.java.
Solo admite el esquema UUID indicado en .tools/ingresos-http.json.
"""
from pathlib import Path
import sys, json, re, subprocess
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'.tools/daynest-python'))
from playwright.sync_api import sync_playwright
datos=json.loads((ROOT/'.tools/ingresos-http.json').read_text())
schema=datos['schema']
assert re.fullmatch(r'renta_http_[a-f0-9]{32}',schema)
cmd=subprocess.check_output(['powershell','-NoProfile','-Command',
 '$p=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction Stop; '
 '(Get-CimInstance Win32_Process -Filter "ProcessId=$($p[0].OwningProcess)").CommandLine'],text=True)
assert '-Drenta.db.schema='+schema in cmd
BASE='http://localhost:8081/DayNest'
checks,errors=[],[]
def digitos(texto):return re.sub(r'\D','',texto)
with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe',headless=True)
 ctx=browser.new_context(locale='es-CO')
 ctx.route('**/*',lambda r:r.continue_() if r.request.url.startswith('http://localhost:8081/') else r.abort())
 page=ctx.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
 def go(path):
  response=page.goto(BASE+path,wait_until='networkidle');assert response.status==200,(path,response.status)
 def login(email,password):
  go('/login');page.locator('[name=correo]').fill(email);page.locator('[name=password]').fill(password)
  page.locator('form[action$="/login"] button[type=submit]').click();page.wait_for_load_state('networkidle');assert '/login' not in page.url
 def salir():
  page.locator('form[action$="/salir"]').evaluate('(f)=>f.requestSubmit()');page.wait_for_url('**/login')
 go('/admin/ingresos');assert '/login' in page.url;checks.append('Ingresos exige sesion')
 login('admin@renta.test','Admin2026!');go('/admin/ingresos')
 assert page.locator('h1').inner_text()=='Comisiones e ingresos de DayNest'
 cop=page.locator('[data-resumen-moneda=COP]');eur=page.locator('[data-resumen-moneda=EUR]')
 assert digitos(cop.locator('[data-importe=comision-liquidada]').inner_text())=='18900000'
 assert digitos(eur.locator('[data-importe=comision-liquidada]').inner_text())=='6000'
 assert digitos(cop.locator('p').filter(has_text='Comisiones pendientes estimadas:').inner_text())=='6000000'
 checks.append('Comisiones liquidadas separadas de pendientes y de EUR')
 for width in [390,768,1440]:
  page.set_viewport_size({'width':width,'height':950});assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'),width
 page.screenshot(path=str(ROOT/'.tools/ingresos-admin-1440.png'),full_page=True)
 checks.append('Resumen y tablas sin desbordamiento en tres anchuras')
 go('/admin/ingresos?inmuebleId='+str(datos['liquidado']))
 assert page.locator('[data-resumen-inmuebles] tbody tr').count()==1
 assert digitos(page.locator('[data-importe=comision-liquidada]').inner_text())=='13500000'
 assert page.locator('[data-historial-ingresos] tbody tr').count()==20
 page.get_by_role('link',name='Siguiente',exact=True).click();page.wait_for_load_state('networkidle')
 assert 'inmuebleId='+str(datos['liquidado']) in page.url
 assert page.locator('[data-historial-ingresos] tbody tr').count()==2
 assert 'EJECUTADO' in page.locator('[data-historial-ingresos]').inner_text()
 assert digitos(page.locator('[data-importe=comision-liquidada]').inner_text())=='13500000'
 checks.append('Filtro por propiedad y paginacion mantienen los acumulados completos')
 salir();login('propietario1@renta.test','Propietario2026!');go('/propietario/ingresos')
 assert digitos(page.locator('[data-resumen-moneda=COP] [data-importe=neto-liquidado]').inner_text())=='31500000'
 assert digitos(page.locator('[data-resumen-moneda=EUR] [data-importe=neto-liquidado]').inner_text())=='14000'
 assert page.locator('[data-resumen-inmuebles] tbody tr').filter(has_text='Casa sin rentas').count()==1
 assert 'Casa AJENA privada' not in page.locator('main').inner_text()
 checks.append('Propietario ve neto por sus propiedades, incluyendo las que no tienen ingresos')
 go('/propietario/ingresos?inmuebleId='+str(datos['ajeno']))
 assert page.locator('[data-historial-ingresos]').count()==0
 assert 'Casa AJENA privada' not in page.locator('main').inner_text()
 assert ctx.request.get(BASE+'/admin/ingresos').status==403
 checks.append('No permite consultar ingresos de otro propietario ni administracion')
 go('/propietario/ingresos?inmuebleId='+str(datos['pendiente']))
 assert digitos(page.locator('[data-importe=neto-liquidado]').inner_text())=='000'
 assert 'CANCELADA' in page.locator('[data-historial-ingresos]').inner_text()
 assert digitos(page.locator('[data-resumen-moneda=COP] p').filter(has_text='Neto pendiente estimado:').inner_text())=='14000000'
 checks.append('Cancelacion devuelta no suma ganancias; estancia futura queda pendiente')
 for width in [390,768,1440]:
  page.set_viewport_size({'width':width,'height':950});assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'),width
 assert not errors,errors
 browser.close()
for check in checks:print('OK:',check)
print('Comprobaciones:',len(checks))
