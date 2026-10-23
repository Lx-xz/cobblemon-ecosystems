# Gera os ícones do botão de farm no menu radial do Cobblemon (32x32, branco, no estilo dos ícones dele):
# uma cesta com frutas (farm ligada; o botão desliga) e a mesma cesta riscada (farm desligada; o botão liga).
# Uso: python farm_icon.py <ligada.png> <desligada.png> [previa.png]
import sys

from PIL import Image

W = (255, 255, 255, 255)
CLEAR = (255, 255, 255, 0)


def basket():
    im = Image.new("RGBA", (32, 32), CLEAR)
    px = im.load()
    # Alça: arco do lado esquerdo ao direito da cesta.
    for x in range(3, 29):
        for y in range(1, 16):
            d = ((x + 0.5 - 16) / 11.5) ** 2 + ((y + 0.5 - 16.5) / 15) ** 2
            if 0.80 <= d <= 1.0:
                px[x, y] = W
    # Frutas em cima da cesta, cada uma com uma margem transparente em volta para não grudar.
    for cx, cy, r in ((10, 13.5, 3.0), (22, 13.5, 3.0), (16, 11, 3.3)):
        for x in range(32):
            for y in range(32):
                d = (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2
                if d <= (r + 1.2) ** 2:
                    px[x, y] = CLEAR
                if d <= r * r:
                    px[x, y] = W
    # Corpo da cesta: trapézio com a borda de cima e frestas do trançado.
    for y in range(17, 29):
        inset = (y - 17) // 4
        for x in range(3 + inset, 29 - inset):
            px[x, y] = W
    for y in (17,):
        for x in range(2, 30):
            px[x, y] = W
    for y in (21, 25):
        for x in range(5, 27):
            px[x, y] = CLEAR
    for y in range(18, 29):
        for x in (9, 16, 23):
            if y not in (21, 25):
                px[x, y] = CLEAR
    # Separa as frutas da borda da cesta.
    for x in range(2, 30):
        px[x, 16] = CLEAR
    return im


def slashed(im):
    im = im.copy()
    px = im.load()
    # Risco da diagonal, com uma margem transparente em volta para destacar.
    for x in range(32):
        for y in range(32):
            d = abs((x + 0.5) + (y + 0.5) - 32) / 1.4142
            if d <= 3.6:
                px[x, y] = CLEAR
            if d <= 2.0 and 2 <= x <= 29 and 2 <= y <= 29:
                px[x, y] = W
    return im


on = basket()
off = slashed(on)
on.save(sys.argv[1])
off.save(sys.argv[2])
if len(sys.argv) > 3:
    bg = Image.new("RGBA", (72, 32), (60, 60, 60, 255))
    bg.alpha_composite(on, (0, 0))
    bg.alpha_composite(off, (40, 0))
    bg.resize((576, 256), Image.NEAREST).save(sys.argv[3])
