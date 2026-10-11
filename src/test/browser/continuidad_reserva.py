"""Recorrido de reserva con Chrome local; requiere el esquema HTTP UUID en Jetty 8081."""
from pathlib import Path
import sys,re,subprocess
root=Path(__file__).resolve().parents[3];sys.path.insert(0,str(root/'.tools/daynest-python'))
from playwright.sync_api import sync_playwright
from datetime import date,timedelta
from urllib.parse import urlencode
base='http://localhost:8081/DayNest'
schema=(root/'.tools/perfiles-http-schema.txt').read_text().strip()
assert re.fullmatch(r'renta_http_[a-f0-9]{32}',schema)
cmd=subprocess.check_output(['powershell','-NoProfile','-Command',"$listener=Get-NetTCPConnection -LocalPort 8081 -State Listen; (Get-CimInstance Win32_Process -Filter \"ProcessId=$($listener[0].OwningProcess)\").CommandLine"],text=True)
assert '-Drenta.db.schema='+schema in cmd
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='C:/Program Files/Google/Chrome/Application/chrome.exe',headless=True)
 page=b.new_page(viewport={'width':390,'height':844})
 entrada=(date.today()+timedelta(days=90)).isoformat();salida=(date.today()+timedelta(days=93)).isoformat()
 assert page.goto(base+'/buscar?'+urlencode(dict(entrada=entrada,salida=salida,capacidad=2))).status==200
 page.locator('.card-photo').first.click()
 assert page.locator('[name=entrada]').input_value()==entrada
 assert page.locator('[name=salida]').input_value()==salida
 assert page.locator('[name=huespedes]').input_value()=='2'
 page.locator('[name=huespedes]').fill('1')
 page.get_by_role('button',name='Continuar con la reserva').click()
 assert page.url.endswith('/login')
 page.locator('[name=correo]').fill('cliente1@renta.test');page.locator('[name=password]').fill('Cliente2026!')
 page.locator('form[action$="/login"] button[type=submit]').click()
 assert '/cliente/reservar?' in page.url
 assert page.locator('[name=entrada]').input_value()==entrada
 assert page.locator('[name=salida]').input_value()==salida
 assert page.locator('[name=huespedes]').input_value()=='1'
 assert '3 noche(s)' in page.locator('#total-reserva').inner_text()
 assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
 page.screenshot(path=str(root/'.tools/continuidad-reserva-390.png'),full_page=True)
 assert page.goto(base+'/inmueble?id=1').status==200
 assert page.locator('[name=entrada]').input_value()==''
 page.get_by_role('button',name='Continuar con la reserva').click()
 assert page.locator('[name=entrada]').input_value()==''
 print('OK: busqueda, detalle editable, login, reserva precargada, total, movil y exploracion sin fechas')
 b.close()

