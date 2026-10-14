# Gera as texturas do mercador "shiny": a roupa roxa com detalhes pretos (no lugar do azul e do dourado) e as
# mantas dos Mudbray/Mudsdale com a mesma troca. O dourado se perdia no pelo laranja dos Mudbray shiny; o roxo
# contrasta com ele (e o vermelho se confundiria com as patas).
# Uso: python trader_shiny.py <wandering_trader.png do Minecraft> <pasta textures do mod> [prévia.png]
import colorsys
import sys
from pathlib import Path

from PIL import Image

# Paleta da manta (veja blanket_mudbray.py): azul -> roxo, dourado -> preto.
BLUE, DARK, LIGHT = (47, 86, 176), (33, 61, 128), (84, 122, 214)
GOLD, DGOLD = (240, 192, 64), (200, 147, 42)
PURPLE, DPURPLE, LPURPLE = (118, 52, 178), (82, 34, 128), (160, 98, 218)
BLACK, DBLACK = (44, 38, 52), (24, 20, 30)
BLANKET_SWAP = {BLUE: PURPLE, DARK: DPURPLE, LIGHT: LPURPLE, GOLD: BLACK, DGOLD: DBLACK}


def swap_blanket(path, out):
    im = Image.open(path).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) in BLANKET_SWAP:
                px[x, y] = BLANKET_SWAP[(r, g, b)] + (a,)
    im.save(out)
    return im


def mix(c1, c2, t):
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


def swap_trader(src):
    """O manto azul vira roxo e os detalhes dourados viram pretos, mantendo a luz e a sombra do original."""
    im = src.convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if 0.55 < h < 0.7 and s > 0.3:      # azul do manto: v de ~0.29 (sombra) a ~0.57 (luz)
                t = min(1.0, max(0.0, (v - 0.29) / 0.28))
                px[x, y] = mix((56, 22, 88), (132, 66, 192), t) + (a,)
            elif 0.07 < h < 0.15 and s > 0.6:   # dourado/laranja dos detalhes: v de ~0.64 a ~0.95
                t = min(1.0, max(0.0, (v - 0.64) / 0.31))
                px[x, y] = mix(DBLACK, (66, 58, 76), t) + (a,)
    return im


def main():
    trader, textures = Path(sys.argv[1]), Path(sys.argv[2])
    shiny = swap_trader(Image.open(trader))
    (textures / "entity").mkdir(parents=True, exist_ok=True)
    shiny.save(textures / "entity/wandering_trader_shiny.png")
    blankets = [swap_blanket(textures / f"pokemon/{n}_trader_blanket.png", textures / f"pokemon/{n}_trader_blanket_shiny.png")
                for n in ("mudbray", "mudsdale")]
    if len(sys.argv) > 3:
        original = Image.open(trader).convert("RGBA")
        sheet = Image.new("RGBA", (128 + 64 + 128, 128), (120, 120, 120, 255))
        sheet.alpha_composite(original, (0, 0))
        sheet.alpha_composite(shiny, (0, 64))
        sheet.alpha_composite(blankets[0], (128, 0))
        sheet.alpha_composite(blankets[1], (192, 0))
        sheet.resize((sheet.width * 5, sheet.height * 5), Image.NEAREST).save(sys.argv[3])


if __name__ == "__main__":
    main()
