# Gera o Purrloin e o Liepard da cabana da bruxa: gato preto de olhos amarelos/laranja e, no shiny, gato branco
# de olhos roxos. Parte das texturas originais do Cobblemon, mantendo a luz e a sombra delas.
# Uso: python witch_cats.py <jar do Cobblemon> <pasta textures/pokemon do mod> [pasta da prévia]
import colorsys
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

SPECIES = {"purrloin": "0509_purrloin", "liepard": "0510_liepard"}

# Cada parte: (cor no escuro, cor no claro). O brilho do pixel original escolhe o ponto entre as duas.
PALETTES = {
    "witch": {
        "fur": ((16, 14, 22), (58, 54, 68)),          # roxo -> preto
        "marking": ((52, 48, 60), (96, 90, 104)),     # creme/amarelo -> cinza-chumbo
        "pink": ((110, 60, 82), (160, 96, 120)),      # rosa mais apagado
        "iris": ((200, 104, 16), (240, 150, 32)),     # verde -> laranja
        "sclera": ((236, 186, 40), (252, 214, 70)),   # branco -> amarelo
    },
    "witch_shiny": {
        "fur": ((196, 194, 206), (250, 250, 252)),    # roxo -> branco
        "marking": ((196, 184, 222), (230, 222, 244)),  # creme/amarelo -> lilás bem claro
        "pink": ((214, 120, 160), (250, 170, 200)),
        "iris": ((120, 40, 180), (164, 76, 226)),     # verde -> roxo
        "sclera": ((206, 172, 240), (228, 204, 252)),  # branco -> lilás
    },
}


def part(r, g, b):
    """Que parte do gato é este pixel (pela cor), ou None para deixar como está (pupila, boca...)."""
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if 0.62 < h < 0.86 and s > 0.2:
        return "fur", v, (0.35, 0.65)
    if 0.04 < h < 0.24 and s > 0.12 and v > 0.7:
        return "marking", v, (0.8, 1.0)
    if (h > 0.86 or h < 0.04) and s > 0.25 and v > 0.7:
        return "pink", v, (0.75, 1.0)
    if 0.25 < h < 0.5 and s > 0.4:
        return "iris", v, (0.5, 0.8)
    if s < 0.15 and v > 0.8:
        return "sclera", v, (0.85, 1.0)
    return None


def recolor(texture, palette):
    im = texture.convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            found = part(r, g, b)
            if not found:
                continue
            name, v, (lo, hi) = found
            t = min(1.0, max(0.0, (v - lo) / (hi - lo)))
            dark, light = palette[name]
            px[x, y] = tuple(int(c1 + (c2 - c1) * t) for c1, c2 in zip(dark, light)) + (a,)
    return im


def main():
    z = zipfile.ZipFile(sys.argv[1])
    out = Path(sys.argv[2])
    preview = Path(sys.argv[3]) if len(sys.argv) > 3 else None
    for species, folder in SPECIES.items():
        texture = Image.open(BytesIO(z.read(f"assets/cobblemon/textures/pokemon/{folder}/{species}.png")))
        variants = []
        for name, palette in PALETTES.items():
            img = recolor(texture, palette)
            img.save(out / f"{species}_{name}.png")
            variants.append(img)
        if preview:
            w, h = texture.size
            scale = 512 // w
            sheet = Image.new("RGBA", (w * 3, h), (120, 120, 120, 255))
            for i, img in enumerate([texture.convert("RGBA")] + variants):
                sheet.alpha_composite(img, (i * w, 0))
            sheet.resize((sheet.width * scale // 2, sheet.height * scale // 2), Image.NEAREST).save(preview / f"{species}_witch.png")


if __name__ == "__main__":
    main()
