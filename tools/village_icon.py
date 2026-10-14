# Gera o ícone de casinha (8x8) que aparece antes do nome dos Pokémon da vila.
# Uso: python village_icon.py <saida.png>
import sys
from PIL import Image

ROOF, ROOF_D = (196, 60, 48, 255), (140, 38, 32, 255)
WALL, WALL_D = (236, 214, 168, 255), (196, 168, 120, 255)
DOOR = (96, 62, 36, 255)
_ = None
ART = [
    [_, _, _, ROOF_D, ROOF_D, _, _, _],
    [_, _, ROOF_D, ROOF, ROOF, ROOF_D, _, _],
    [_, ROOF_D, ROOF, ROOF, ROOF, ROOF, ROOF_D, _],
    [ROOF_D, ROOF, ROOF, ROOF, ROOF, ROOF, ROOF, ROOF_D],
    [_, WALL_D, WALL, WALL, WALL, WALL, WALL_D, _],
    [_, WALL_D, WALL, DOOR, DOOR, WALL, WALL_D, _],
    [_, WALL_D, WALL, DOOR, DOOR, WALL, WALL_D, _],
    [_, WALL_D, WALL_D, DOOR, DOOR, WALL_D, WALL_D, _],
]
img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
for y, row in enumerate(ART):
    for x, c in enumerate(row):
        if c: img.putpixel((x, y), c)
img.save(sys.argv[1])
