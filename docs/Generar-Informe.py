"""Genera HTML autónomo y, opcionalmente, PDF local desde INFORME.md.

Usa solo la sintaxis Markdown del informe (títulos, párrafos, listas, tablas,
enlaces, negrita, código e imágenes). No descarga recursos ni toca la BD.
PDF: requiere Playwright para Python y Chrome instalado; ver diagramas/README.md.
"""
from pathlib import Path
import argparse
import base64
import html
import re
import sys
import xml.etree.ElementTree as ET

DOCS=Path(__file__).resolve().parent
ROOT=DOCS.parent

def inline(text):
    text=html.escape(text)
    text=re.sub(r'`([^`]+)`',r'<code>\1</code>',text)
    text=re.sub(r'\*\*([^*]+)\*\*',r'<strong>\1</strong>',text)
    text=re.sub(r'\[([^]]+)\]\(([^)]+)\)',r'<a href="\2">\1</a>',text)
    return text

def slug(text):
    return re.sub(r'[^a-z0-9-]','',re.sub(r'\s+','-',text.lower()))

def generate_html():
    lines=(DOCS/'INFORME.md').read_text(encoding='utf-8').splitlines()
    out=[];heads=[];i=0;cover=True;section=''
    out.append('<section class="cover">')
    while i<len(lines):
        line=lines[i]
        if not line.strip():i+=1;continue
        head=re.match(r'^(#{1,3}) (.+)$',line)
        if head:
            level=len(head[1]);text=head[2];ident=slug(text)
            if level==3:section=text
            if level==2 and cover:
                out.append('</section><nav><h2>Contenido</h2>TOC_PLACEHOLDER</nav>');cover=False
            if level==2:heads.append((ident,text))
            cls=' class="case-title"' if level==3 and re.match(r'6\.\d+ CU',text) else ''
            out.append(f'<h{level} id="{ident}"{cls}>{inline(text)}</h{level}>');i+=1;continue
        img=re.match(r'^!\[(.*?)\]\((.*?)\)$',line)
        if img:
            path=(DOCS/img[2]).resolve()
            if not path.is_relative_to(ROOT) or not path.is_file():raise ValueError(f'Imagen inválida: {path}')
            raw=path.read_text(encoding='utf-8')
            if 'Syntax Error' in raw:raise ValueError(f'Diagrama inválido: {path}')
            node=ET.fromstring(raw)
            width=float(re.sub(r'[^0-9.]','',node.get('width')))
            height=float(re.sub(r'[^0-9.]','',node.get('height')))
            # A3: conserva lectura de diagramas; texto del informe permanece A4.
            orient='portrait' if height/width>1.05 else 'landscape'
            raw=re.sub(r'^.*?(?=<svg\b)','',raw,flags=re.S)
            description=''
            if out and out[-1].startswith('<p>'):
                description=out.pop().replace('<p>','<p class="diagram-description">',1)
            heading=''
            if out and out[-1].startswith('<h3'):
                heading=out.pop()
            after=[]
            j=i+1
            while j<len(lines) and not lines[j].strip():j+=1
            while not section.startswith('5.2') and j<len(lines) and lines[j].strip() and not re.match(r'^(?:#|!\[|\||- )',lines[j]):
                after.append(inline(lines[j].strip()));j+=1
            if after:description+='<p class="diagram-description">'+' '.join(after)+'</p>';i=j
            else:i+=1
            out.append(f'<figure class="diagram {orient}">{heading}<figcaption>{inline(img[1])}</figcaption>{description}<div class="svg-wrap">{raw}</div></figure>')
            continue
        if line.startswith('|'):
            rows=[]
            while i<len(lines) and lines[i].startswith('|'):
                row=[x.strip() for x in lines[i].strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?',x) for x in row):rows.append(row)
                i+=1
            out.append('<table><thead><tr>'+''.join('<th>'+inline(x)+'</th>' for x in rows[0])+'</tr></thead><tbody>')
            for row in rows[1:]:out.append('<tr>'+''.join('<td>'+inline(x)+'</td>' for x in row)+'</tr>')
            out.append('</tbody></table>');continue
        if line.startswith('- '):
            out.append('<ul>')
            while i<len(lines) and lines[i].startswith('- '):out.append('<li>'+inline(lines[i][2:])+'</li>');i+=1
            out.append('</ul>');continue
        paragraph=[]
        while i<len(lines) and lines[i].strip() and not re.match(r'^(?:#|!\[|\||- )',lines[i]):
            paragraph.append(inline(lines[i].strip()));i+=1
        out.append('<p>'+'<br>'.join(paragraph)+'</p>')
    toc='<ol>'+''.join(f'<li><a href="#{ident}">{inline(re.sub(r"^\d+\.\s*", "", text))}</a></li>' for ident,text in heads)+'</ol>'
    body='\n'.join(out).replace('TOC_PLACEHOLDER',toc)
    fonts=[]
    for name,file in [('Inter','inter-latin-variable.woff2'),('Manrope','manrope-latin-variable.woff2')]:
        encoded=base64.b64encode((ROOT/'src/main/webapp/assets/fonts'/file).read_bytes()).decode()
        fonts.append(f"@font-face{{font-family:{name};src:url(data:font/woff2;base64,{encoded}) format('woff2');font-weight:100 900;}}")
    css='''
    *{box-sizing:border-box}body{margin:0;font:15px/1.55 Inter,Arial,sans-serif;color:#172d43;background:#edf0f3}
    main{max-width:1080px;margin:32px auto;padding:52px;background:white;border-radius:12px}
    h1,h2,h3{font-family:Manrope,Arial,sans-serif;line-height:1.2;color:#152d44}
    h1{font-size:38px}h2{font-size:25px;margin:40px 0 18px;border-bottom:2px solid #d66a43;padding-bottom:12px}
    h3{font-size:20px;margin:30px 0 16px}p{margin:0 0 15px}a{color:#aa4529}code{font-size:.9em}
    table{width:100%;border-collapse:collapse;margin:20px 0;font-size:13px}th,td{padding:10px;border:1px solid #d8dfe5;vertical-align:top;text-align:left}
    th{background:#eef3f6}tr:nth-child(even){background:#fafbfd}li{margin-bottom:8px}
    .cover{padding:80px 0}.cover h1{max-width:750px}.cover strong{color:#aa4529}nav{padding-bottom:20px}
    figure{margin:30px 0;padding:18px 0;break-inside:avoid}figcaption{font-weight:600;margin-bottom:18px;font-size:15px}
    .svg-wrap{overflow-x:auto}svg{display:block;width:100%!important;height:auto!important}svg text{font-family:Arial,sans-serif}
    .diagram-description{font-size:13px;margin-bottom:12px}
    @page{size:A4;margin:18mm 17mm}
    @page diagramPortrait{size:A3 portrait;margin:12mm}
    @page diagramLandscape{size:A3 landscape;margin:12mm}
    @media print{
      body{font:10.5pt/1.42 Inter,Arial,sans-serif;background:white}main{max-width:none;margin:0;padding:0;border-radius:0}
      h1{font-size:29pt}h2{font-size:18pt;break-before:page;margin:0 0 16px}h3{font-size:14pt;break-after:avoid}
      p{orphans:3;widows:3}table{font-size:9pt;margin:12px 0}th,td{padding:7px}tr{break-inside:avoid}thead{display:table-header-group}
      .cover{padding:45mm 0;break-after:page}.case-title{break-before:page;margin-top:0}nav{break-after:page}
      figure{break-before:page;break-after:page;margin:0;padding:0;border:0}
      figure.portrait{page:diagramPortrait}.portrait .svg-wrap{height:343mm;width:273mm}
      figure.landscape{page:diagramLandscape}.landscape .svg-wrap{height:218mm;width:395mm}
      .svg-wrap{overflow:visible;display:flex;align-items:flex-start;justify-content:center}
      .svg-wrap svg{width:100%!important;height:100%!important;object-fit:contain;max-width:100%;max-height:100%}
      .diagram-description{font-size:10pt;margin-bottom:3mm}
      figcaption{font-size:12pt;margin:0 0 8mm}a{text-decoration:none;color:inherit}
      figure h3{font-size:14pt;margin:0 0 4mm}
    }
    '''
    path=DOCS/'INFORME.html'
    path.write_text('<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>DayNest — Informe de diseño</title><style>'+''.join(fonts)+css+'</style><body><main>'+body+'</main></body></html>',encoding='utf-8')
    return path

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--pdf',action='store_true');ap.add_argument('--chrome',default='C:/Program Files/Google/Chrome/Application/chrome.exe');args=ap.parse_args()
    path=generate_html();print(f'HTML generado: {path}')
    if args.pdf:
        local=ROOT/'.tools/daynest-python'
        if local.is_dir():sys.path.insert(0,str(local))
        from playwright.sync_api import sync_playwright
        with sync_playwright() as pw:
            browser=pw.chromium.launch(executable_path=args.chrome,headless=True)
            page=browser.new_page()
            # Documento autónomo: se bloquea toda solicitud de red.
            page.route('http://**/*',lambda route:route.abort())
            page.route('https://**/*',lambda route:route.abort())
            page.goto(path.as_uri(),wait_until='load');page.evaluate('document.fonts.ready')
            page.pdf(path=str(DOCS/'INFORME.pdf'),print_background=True,prefer_css_page_size=True,
                     display_header_footer=True,header_template='<span></span>',
                     footer_template='<div style="font:8px Arial;width:100%;text-align:center;color:#526478">DayNest · Informe de diseño · <span class="pageNumber"></span> / <span class="totalPages"></span></div>')
            browser.close()
        print('PDF generado: docs/INFORME.pdf')

if __name__=='__main__':main()
