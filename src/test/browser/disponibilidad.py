"""Compatibilidad: el recorrido de disponibilidad ahora prueba los calendarios visuales."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name("calendario.py")), run_name="__main__")
