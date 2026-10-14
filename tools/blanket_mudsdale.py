# Gera a camada da manta do Mudsdale do mercador.
# Uso: python blanket_mudsdale.py <saida.png> <mudsdale.png original> <previa.png>
import sys
from PIL import Image
out, base, preview = sys.argv[1:4]
BLUE, DARK, LIGHT = (47, 86, 176, 255), (33, 61, 128, 255), (84, 122, 214, 255)
GOLD, DGOLD = (240, 192, 64, 255), (200, 147, 42, 255)
im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
px = im.load()
# Torso 12x12x26, uv (52,39). Lombo: x 78-89, y 39-64. Manta nas 14 linhas do meio (y 45-58).
TOP_X, ROWS = range(78, 90), range(45, 59)
for y in ROWS:
    for x in TOP_X:
        if y in (45, 58): c = GOLD
        elif y in (46, 57): c = DARK
        elif x in (83, 84): c = LIGHT
        else: c = BLUE
        px[x, y] = c
# Losango dourado no meio do lombo
for dy, xs in ((-2, (83, 84)), (-1, (82, 83, 84, 85)), (0, (81, 82, 83, 84, 85, 86)), (1, (82, 83, 84, 85)), (2, (83, 84))):
    for x in xs: px[x, 51 + dy] = GOLD if abs(dy) < 2 else DGOLD
px[83, 51] = px[84, 51] = DGOLD
# Laterais (altura 12, a partir de y 65): direita x 58-71, esquerda x 96-109. Desce 8 pixels.
for x0 in (58, 96):
    for i, x in enumerate(range(x0, x0 + 14)):
        for y in range(65, 71): px[x, y] = BLUE
        px[x, 68] = GOLD if i % 4 == 1 else BLUE
        px[x, 71] = DGOLD
        px[x, 72] = GOLD if i % 2 == 0 else DARK
    for y in range(65, 72): px[x0, y] = px[x0 + 13, y] = DGOLD
im.save(out)
prev = Image.alpha_composite(Image.open(base).convert("RGBA"), im).crop((50, 37, 128, 80)).resize((78 * 6, 43 * 6), Image.NEAREST)
prev.save(preview)
