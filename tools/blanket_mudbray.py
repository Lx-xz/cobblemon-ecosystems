# Gera a camada da manta do Mudbray do mercador.
# Uso: python blanket_mudbray.py <saida.png> <mudbray.png original> <previa.png>
import sys
from PIL import Image
out, base = sys.argv[1], sys.argv[2]
BLUE, DARK, LIGHT = (47, 86, 176, 255), (33, 61, 128, 255), (84, 122, 214, 255)
GOLD, DGOLD = (240, 192, 64, 255), (200, 147, 42, 255)
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = im.load()
# Lombo do corpo: x 12-16, y 20-30 (comprimento 11). Manta nas 7 linhas do meio (y 22-28).
for y in range(22, 29):
    for x in range(12, 17):
        px[x, y] = GOLD if y in (22, 28) else (LIGHT if x == 14 else BLUE)
px[14, 25] = GOLD; px[13, 25] = DGOLD; px[15, 25] = DGOLD
# Laterais (altura 5, a partir de y 31): direita x 3-9, esquerda x 19-25. Desce 3 pixels com franja.
for x0 in (3, 19):
    for i, x in enumerate(range(x0, x0 + 7)):
        px[x, 31] = BLUE
        px[x, 32] = DGOLD if i in (0, 6) else (GOLD if i == 3 else BLUE)
        px[x, 33] = GOLD if i % 2 == 0 else DARK
im.save(out)
# Prévia: textura original + manta, ampliada
prev = Image.alpha_composite(Image.open(base).convert("RGBA"), im).crop((0, 18, 36, 38)).resize((36 * 12, 20 * 12), Image.NEAREST)
prev.save(sys.argv[3])
