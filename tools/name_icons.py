# Gera os ícones (8x8) que aparecem antes dos nomes: Pokébola (aldeões com Pokémon), chapéu (bruxa da cabana),
# mochila (mercador) e estrela (shiny). O de casinha (Pokémon da vila) é o village_icon.py.
# Uso: python name_icons.py <pasta textures/font do mod> [prévia.png]
import os
import sys

from PIL import Image

PALETTE = {
    "R": (214, 48, 48, 255),    # vermelho da Pokébola
    "r": (150, 30, 30, 255),
    "W": (240, 240, 240, 255),  # branco
    "w": (190, 190, 196, 255),
    "K": (40, 36, 44, 255),     # contorno
    "P": (96, 52, 140, 255),    # roxo do chapéu
    "p": (64, 34, 96, 255),
    "G": (240, 192, 64, 255),   # dourado
    "B": (150, 98, 52, 255),    # couro da mochila
    "b": (104, 66, 34, 255),
    "Y": (255, 222, 70, 255),   # estrela
    "y": (214, 160, 30, 255),
}
ICONS = {
    "pokeball": [
        "..KKKK..",
        ".KRRRRK.",
        "KRRRRRRK",
        "KKKWWKKK",
        "KwWKKWwK",
        "KwWWWWwK",
        ".KwwwwK.",
        "..KKKK..",
    ],
    "witch_hat": [
        "....pp..",
        "...pPp..",
        "...PPp..",
        "..pPPPp.",
        "..GGGGG.",
        ".pPPPPPp",
        "pPPPPPPPp"[:8],
        "pppppppp",
    ],
    "backpack": [
        "..bbbb..",
        ".b....b.",
        "bBBBBBBb",
        "bBBGGBBb",
        "bBBBBBBb",
        "bBbbbbBb",
        "bBBBBBBb",
        ".bbbbbb.",
    ],
    "star": [
        "...yy...",
        "...YY...",
        "yyYYYYyy",
        ".yYYYYy.",
        "..YYYY..",
        ".yYyyYy.",
        ".Yy..yY.",
        ".y....y.",
    ],
}

out = sys.argv[1]
previews = []
for name, rows in ICONS.items():
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row[:8]):
            if c in PALETTE:
                img.putpixel((x, y), PALETTE[c])
    img.save(os.path.join(out, f"{name}.png"))
    previews.append(img)
if len(sys.argv) > 2:
    sheet = Image.new("RGBA", (10 * len(previews), 10), (90, 90, 90, 255))
    for i, img in enumerate(previews):
        sheet.alpha_composite(img, (i * 10 + 1, 1))
    sheet.resize((sheet.width * 16, sheet.height * 16), Image.NEAREST).save(sys.argv[2])
