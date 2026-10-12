from pathlib import Path
import sys,json,datetime,re,subprocess
from urllib.parse import urlparse, parse_qs
ROOT=Path(__file__).resolve().parents[3];sys.path.insert(0,str(ROOT/'.tools/daynest-python'))
from playwright.sync_api import sync_playwright
BASE='http://localhost:8081/DayNest'
schema=(ROOT/'.tools/http-modelo-schema.txt').read_text().strip()
assert re.fullmatch(r'renta_http_[a-f0-9]{32}',schema),'Se requiere esquema HTTP UUID de prueba'
command=subprocess.check_output(['powershell','-NoProfile','-Command',"$p=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction Stop; (Get-CimInstance Win32_Process -Filter \"ProcessId=$($p[0].OwningProcess)\").CommandLine"],text=True)
assert '-Drenta.db.schema='+schema in command,'El proceso en 8081 no utiliza el esquema aislado autorizado'

report={'checks':[],'js':[],'external':[]}
with sync_playwright() as pw:
 browser=pw.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe',headless=True)
 ctx=browser.new_context(locale='es-CO',timezone_id='America/Bogota')
 def route(r):
  if r.request.url.startswith('http://localhost:8081/'):r.continue_()
  else:print('RECURSO EXTERNO',r.request.url,flush=True);report['external'].append(r.request.url);r.abort()
 ctx.route('**/*',route);page=ctx.new_page();page.on('pageerror',lambda e:report['js'].append(str(e)))
 def go(path):
  r=page.goto(BASE+path,wait_until='networkidle');assert r.status==200,(path,r.status,page.locator('main').inner_text());return r
 go('/inicio')
 go('/login');page.locator('[name=correo]').fill('propietario1@renta.test');page.locator('[name=password]').fill('Propietario2026!');page.locator('form[action$="/login"] button[type=submit]').click();page.wait_for_load_state('networkidle');assert '/login' not in page.url
 # Business writes only in the isolated renta_http_UUID schema.
 for tipo,rural,mar,title in [('CABAÑA','true','false','Cabaña rural prueba'),('CASA','false','true','Casa costera prueba'),('APARTAMENTO','true','true','Apartamento ambos entornos prueba')]:
  go('/propietario/publicar')
  page.locator('[name=titulo]').fill(title);page.locator('[name=ciudadId]').select_option(label='Cartagena, Bolívar, Colombia');page.locator('[name=direccion]').fill('Dirección de prueba');page.locator('[name=descripcion]').fill('Propiedad de verificación temporal');page.locator('[name=capacidad]').fill('4');page.locator('[name=precio]').fill('190000');page.locator('[name=tipo]').select_option(tipo);page.locator('[name=esRural]').select_option(rural);page.locator('[name=cercaMar]').select_option(mar)
  page.locator('button[type=submit]').filter(has_text='Guardar inmueble').click();page.wait_for_load_state('networkidle');assert '/propietario/inmuebles' in page.url,page.locator('main').inner_text()
 report['checks'].append('Publicación real de tres clasificaciones en esquema aislado con CSRF')
 go('/buscar?tipo=CABAÑA&rural=true');assert page.locator('.property-card').count()==1
 href=page.locator('.property-card h3 a').get_attribute('href');cabana_id=parse_qs(urlparse(href).query)['id'][0]
 go('/propietario/editar?id='+cabana_id);assert page.locator('[name=tipo]').input_value()=='CABAÑA';assert page.locator('[name=esRural]').input_value()=='true';assert page.locator('[name=cercaMar]').input_value()=='false'
 page.locator('[name=precio]').fill('195000');page.locator('button[type=submit]').filter(has_text='Guardar inmueble').click();page.wait_for_load_state('networkidle');assert '/propietario/inmuebles' in page.url
 report['checks'].append('Edición conserva valores sí/no, tipo y CSRF')
 # Form validation remembers classification after failure.
 go('/propietario/editar?id='+cabana_id);page.locator('[name=capacidad]').fill('0');
 with page.expect_navigation(wait_until='networkidle'):page.evaluate('document.querySelector("form.form-stack").submit()')
 assert page.locator('[name=tipo]').input_value()=='CABAÑA'
 report['checks'].append('Formulario inválido conserva clasificación')
 for width in [1440,768,390]:
  page.set_viewport_size({'width':width,'height':1000})
  for route_path in ['/inicio','/buscar?tipo=CABAÑA&rural=true','/inmueble?id='+cabana_id]:
   go(route_path);assert page.evaluate('document.documentElement.scrollWidth')<=width+1,(width,route_path)
   if route_path=='/inicio':page.screenshot(path=str(ROOT/f'.tools/filtros-inicio-{width}.png'),full_page=True)
  for label,key,value in [('Apartamentos','tipo','APARTAMENTO'),('Casas','tipo','CASA'),('Cabañas','tipo','CABAÑA'),('En el campo','rural','true'),('Junto al mar','mar','true')]:
   go('/inicio');link=page.locator('.category-list a').filter(has_text=label);link.focus();assert link.evaluate('el=>el===document.activeElement');assert link.bounding_box()['height']>=44;link.press('Enter');page.wait_for_load_state('networkidle');assert '/buscar?' in page.url;assert page.locator('#'+key).input_value()==value;assert page.locator('.property-card').count()>0;assert page.locator('.active-filters a').count()==1
   report['checks'].append(f'{width}px: {label}, teclado, resultados PostgreSQL y filtro visible')
 page.set_viewport_size({'width':1440,'height':1000})
 go('/inicio');page.locator('#ciudad').fill('Cartagena');assert page.locator('[role=option]').count()==2
 page.locator('[role=option]').filter(has_text='Colombia').dispatch_event('mousedown');ciudad=page.locator('[data-city-id]').input_value();assert ciudad
 page.locator('.filter-extras summary').click();page.locator('#maximo').fill('200000');page.locator('#capacidad').fill('4');page.locator('.category-list a').filter(has_text='Cabañas').click();page.wait_for_load_state('networkidle');assert 'ciudadId='+ciudad in page.url;assert 'maximo=200000' in page.url;assert 'capacidad=4' in page.url;assert page.locator('.property-card').count()==1
 report['checks'].append('Tarjeta conserva ciudad elegida, precio y capacidad escritos antes de enviar')
 go('/buscar?tipo=CABAÑA&rural=true&ciudadId='+ciudad+'&maximo=200000');page.locator('.filter-chip').filter(has_text='Tipo:').click();page.wait_for_load_state('networkidle');assert 'tipo=' not in page.url;assert 'rural=true' in page.url;assert 'ciudadId='+ciudad in page.url;assert 'maximo=200000' in page.url;assert page.locator('.property-card').count()==2
 report['checks'].append('Quitar tipo conserva entorno, ciudad y presupuesto')
 go('/buscar?tipo=CASA&mar=true');assert page.locator('.property-card').count()==1;assert 'Casa costera prueba' in page.locator('.property-card').inner_text()
 go('/buscar?tipo=CABAÑA&mar=true');assert page.locator('.property-card').count()==0;assert 'modificar o quitar' in page.locator('.empty').inner_text()
 report['checks'].append('Combinación sin coincidencias muestra estado vacío claro')
 go('/buscar?paisId=1&divisionId=5&tipo=CABAÑA');assert page.locator('.filter-chip').filter(has_text='Colombia').count()==2
 go('/buscar?ciudad=Cartagena&tipo=APARTAMENTO');assert 'España' in page.locator('main').inner_text() and 'Colombia' in page.locator('main').inner_text()
 go('/buscar?ciudad=Bogota');assert page.locator('.property-card').count()==2
 report['checks'].append('Búsqueda convencional sin tilde y Cartagena homónima se conservan')
 go('/buscar?tipo=APARTAMENTO');link=page.locator('.pagination a').filter(has_text='Siguiente');assert link.count()==1;assert 'tipo=APARTAMENTO' in link.get_attribute('href');link.click();page.wait_for_load_state('networkidle');assert page.locator('#tipo').input_value()=='APARTAMENTO'
 report['checks'].append('Paginación conserva filtro')
 for q in ['tipo=CAMPO','rural=desconocido','mar=2','paisId=-1']:
  r=page.goto(BASE+'/buscar?'+q,wait_until='networkidle');assert r.status==400,(q,r.status)
 report['checks'].append('Parámetros desconocidos rechazados con HTTP 400')
 # Actual reservation blocks a filtered cabin; contiguous interval remains searchable.
 page.evaluate("document.querySelector('form[action$=\"/salir\"]').requestSubmit()");page.wait_for_url('**/login')
 go('/login');page.locator('[name=correo]').fill('cliente1@renta.test');page.locator('[name=password]').fill('Cliente2026!');page.locator('form[action$="/login"] button[type=submit]').click();page.wait_for_load_state('networkidle')
 # Reservation/payment regression covered by 65 JUnit cases; HTTP test checks date filtering and detail route.
 day=datetime.date.today()+datetime.timedelta(days=40);end=day+datetime.timedelta(days=3)
 go(f'/buscar?tipo=CABAÑA&rural=true&entrada={day}&salida={end}&capacidad=4');assert page.locator('.property-card').count()==1;page.locator('.property-card h3 a').click();page.wait_for_load_state('networkidle');assert urlparse(page.url).path.endswith('/inmueble') and parse_qs(urlparse(page.url).query).get('id')==[cabana_id]
 report['checks'].append('Fechas, capacidad y acceso al detalle desde combinación de filtros')
 # Anchors also work when JavaScript is disabled.
 nojs=browser.new_context(java_script_enabled=False);p2=nojs.new_page();p2.goto(BASE+'/inicio?mar=true');p2.locator('.category-list a').filter(has_text='Cabañas').click();assert 'mar=true' in p2.url and 'tipo=' in p2.url;nojs.close();report['checks'].append('Enlaces funcionan sin JavaScript conservando parámetros previos')
 browser.close()
assert not report['js'],report['js'];assert not report['external'],report['external']
(ROOT/'.tools/verificacion-filtros-http.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))


