# Farms do pasto

**Marque cada linha** e me avise: eu passo as mudanças para o jogo.

| Marca | Significa |
|---|---|
| `[x]` | já implementada |
| `[+]` | vamos fazer |
| `[-]` | rejeitada |
| `[ ]` | ainda não decidida |

Já marquei `[x]` no que existe e `[-]` no que cai nas regras abaixo (órgão ou repetido do Cobblemon). Pode mudar
qualquer marca.

## Como funcionam hoje

- O Pokémon precisa estar no **pasto** e pelo menos **Confortável** no habitat. **Feliz** produz mais rápido (60% do
  tempo).
- Ele **não quebra nada**: só solta o item no chão. Com um **container encostado no bloco do pasto** (baú, barril
  ou de outro mod), os itens e os ovos vão para lá; o que não couber cai no chão.
- Condições possíveis: **pisando em** um grupo de blocos (`on:`), **o habitat tem** um grupo (`near:`) e a **madeira
  do bioma** (`from:biome_log`).
- Na configuração, cada farm é uma linha de `pasture_farm_list`: `quem = item xQuantidade, every:minutos, on:grupo,
  near:grupo`. Quem pode ser uma espécie (`bibarel`) ou um tipo (`type:fire`); vale a primeira linha que serve.
- **Botão de cesta** no menu radial do Pokémon (agachar + clique direito): liga e desliga a coleta dele (a farm e os
  ovos). Desligada, a cesta aparece cortada. Os botões de habitat e de coleta só aparecem com o Pokémon no pasto.

Grupos de blocos (`habitat_features`): calor, água, plantas, flores, metal, pedra, terra, neve e gelo, eletricidade,
mato e flores (insetos), lama e cogumelos, almas e velas, ametista e livros, flores e ametista (fada), treino,
aconchego, pedra do End, árvores. Prontos: céu aberto, teto, escuro, luz forte, bioma frio, bioma quente, chuva.

## Regras da análise

1. **Lógica do Minecraft — órgão só morto.** Item que no jogo só cai matando o mob, por ser parte do corpo (bolsa de
   tinta, couro, olho de aranha, pólvora, vara de blaze, lágrima de ghast, membrana de phantom, pérola do End,
   casca de shulker, carne, ossos do esqueleto, cristal de prismarinho...), **não** vira farm. Polvo não dá tinta.
2. **Lógica do Minecraft — o que sai vivo, vale.** Os precedentes do jogo: galinha bota **ovo**, tartaruga solta
   **escama** ao crescer, tatu solta **escama** de tempos em tempos, panda **espirra slimeball**, farejador **cava
   sementes**, ovelha dá **lã**, vaca dá **leite**, abelha faz **mel**, gato traz **presentes**, ametista **cresce**
   sozinha no geodo, compostor transforma planta em **farinha de osso**.
3. **Lógica real.** Vale o que o animal produz ou larga sem se machucar: troca de pele/pelo/pena/casca, seda, ovo,
   secreção (muco, mel, leite), fruto que cresce no corpo e cai, adubo, o que ele cava, junta ou carrega.
4. **Lógica Pokémon.** A Pokédex e as habilidades ajudam a desempatar (Pickup, Pay Day, Honey Gather...).
5. **Não repetir o Cobblemon 1.8** (interação com item na mão, lista no fim). Se ele já dá o item àquela espécie, a
   farm é `[-]`.
6. **Balanceamento.** Minério sai como **pepita/pedaço**, nunca lingote ou diamante. Itens raros e valiosos (itens de
   segurar do Cobblemon, ovo da sorte) só com tempo longo.

## Recursos de mineração

| | Quem | Item | A cada | Condição | Por quê |
|---|---|---|---|---|---|
| [x] | Torkoal | Carvão | 15 min | pisando em calor | Pokédex: cava carvão nas montanhas e queima no casco. O Cobblemon dá carvão só a Rolycoly/Carkol/Coalossal |
| [-] | Charmander, Charmeleon, Charizard | Carvão vegetal | 15 min | habitat com árvores | Real: madeira queimada devagar vira carvão. Precisa vir antes de `type:fire` na lista |
| [-] | Diglett, Dugtrio | Sílex | 10 min | pisando em terra | Mine: sílex sai do cascalho que se cava. Real: cavando acha pedra lascada |
| [x] | Drilbur, Excadrill | Pepita de ferro (raro: ferro bruto) | 20 min | habitat com pedra | Pokédex: Excadrill cava túneis nas minas e ajuda mineiros |
| [-] | Tinkatink, Tinkatuff, Tinkaton | Pepita de ferro | 15 min | habitat com metal | Pokédex: junta sucata de metal para o martelo |
| [-] | Gimmighoul | Moeda Relíquia (Cobblemon) ou pepita de ouro | 20 min | | Pokédex: anda juntando moedas. O item existe no Cobblemon |
| [+] | Meowth, Persian | Pepita de ouro | 20 min | | Golpe Pay Day (espalha moedas); Pokédex: adora coisas brilhantes |
| [-] | Gholdengo | Pepita de ouro | 15 min | | Corpo de mil moedas: solta moedas, não órgão |
| [+] | Carbink, Glimmet, Glimmora | Fragmento de ametista | 20 min | habitat com ametista | Mine: ametista cresce sozinha no geodo. Real: cristal cresce |
| [-] | Roggenrola, Boldore, Gigalith | Tumblestone (Cobblemon) | 20 min | habitat com pedra | Tumblestone cresce em bloco como ametista; Roggenrola é rocha viva |
| [-] | Nosepass, Probopass, Magnemite | Ímã (Magnet, Cobblemon) | 60 min | habitat com metal | Corpo magnético atrai limalha. Item de segurar: tempo longo |
| [+] | Wooper, Quagsire, Mudkip, Marshtomp | Bola de argila | 10 min | habitat com água e terra | Mine: argila fica no fundo da água. Real: cavam lama de rio |
| [-] | Mudbray, Mudsdale | Bloco de lama | 10 min | habitat com água e terra | Pokédex: brincam e se cobrem de lama |
| [ ] | Sableye | Minério pequeno | | | Real/Pokédex: **come** pedras preciosas, não produz. Se fizer, só "devolve" algo |
| [-] | Diancie, Carbink | Diamante | | | Balanceamento: diamante não sai de farm |
| [-] | Elétricos (Pikachu, Magnemite...) | Pó de redstone | | | Nem o Mine nem a vida real têm criatura que faça redstone; só minério |
| [-] | Qualquer | Lingotes, esmeralda, lápis, quartzo | | | Só mineração/comércio no Mine; balanceamento |

## Fogo, gelo e calor

| | Quem | Item | A cada | Condição | Por quê |
|---|---|---|---|---|---|
| [x] | Tipo Fogo | Pó de blaze | 10 min | pisando em calor | Já implementada (o pó, não a vara: a vara é o corpo do blaze) |
| [-] | Slugma, Magcargo | Bloco de magma em volta | | pisando em pedra | Muda bloco: decidir se pode. O Cobblemon já dá lava no balde |
| [-] | Slugma, Magcargo, Magmar | Creme de magma | | | Mine: só matando o cubo de magma (órgão) |
| [-] | Snorunt, Glalie, Froslass | Bola de neve | 10 min | bioma frio | Real/Pokédex: vive na neve e sopra ar gelado. Vanillite já tem no Cobblemon |
| [-] | Cubchoo, Beartic | Slimeball ou bola de neve | 15 min | bioma frio | Mine: panda espirra slimeball. Pokédex: Cubchoo vive com o nariz escorrendo |
| [+] | Snom, Frosmoth | Linha | 15 min | bioma frio | Pokédex: Snom solta seda coberta de gelo |
| [+] | Bergmite, Avalugg | Gelo | 20 min | bioma frio | Corpo de gelo que se refaz; muda pouco o equilíbrio |
| [-] | Vanillite, Vanillish, Vanilluxe | Bola de neve | | | Repetido: o Cobblemon dá com a escova |

## Seda, muco e secreções

| | Quem | Item | A cada | Condição | Por quê |
|---|---|---|---|---|---|
| [+] | Spinarak, Ariados, Joltik, Galvantula | Linha | 10 min | habitat escuro, com árvores | Mine: teia vira linha. Real: aranha fia seda sem se machucar |
| [+] | Spinarak, Ariados | Teia (bloco) num canto | | habitat escuro | Muda bloco: decidir se pode |
| [+] | Caterpie, Weedle, Wurmple, Sewaddle, Burmy, Larvesta | Linha | 15 min | habitat com árvores | Real: lagarta fia o casulo |
| [+] | Grimer, Muk, Solosis | Slimeball | 15 min | habitat com lama | Mine: panda espirra slimeball (secreção, não órgão). Goomy já tem no Cobblemon |
| [ ] | Shuckle | Suco de Fruta (Berry Juice, Cobblemon) | 30 min | fruta Pokémon num baú perto | Mainline: fermenta frutas no casco. O item existe no Cobblemon |
| [-] | Butterfree, Beautifly, Dustox | Silver Powder (Cobblemon) | 60 min | habitat com flores | Real: asa de borboleta solta escamas (pó). Item de segurar: tempo longo |
| [-] | Octillery, Tentacool, Inkay, Malamar, Clauncher, Squid... | Bolsa de tinta (e brilhante) | | | Mine: órgão da lula, só matando |
| [+] | Goomy, Sliggoo, Goodra | Slimeball | | | Repetido: o Cobblemon dá com a escova |
| [+] | Cottonee, Whimsicott, Eldegoss, Tarountula, Spidops | Linha | | | Repetido: o Cobblemon dá com a escova |
| [-] | Drifloon, Shuppet, fantasmas | Lágrima de ghast | | | Mine: só matando o ghast; item raro de poção |
| [-] | Gastly, Haunter, Gengar | Membrana de phantom | | | Mine: órgão do phantom. Dreepy já dá com a escova |

## Ovos, penas, pelo e escamas

| | Quem | Item | A cada | Condição | Por quê |
|---|---|---|---|---|---|
| [x] | Fêmeas de Doduo, Psyduck, Torchic, Delibird, Piplup e evoluções | Ovo | | | Já implementado (como a galinha; `egg_laying_species`); desliga no botão de cesta |
| [-] | Chansey, Blissey (fêmeas) | Ovo; raro: Ovo da Sorte (Cobblemon) | 10 min / raro | | Pokédex: bota ovos nutritivos e dá a quem precisa |
| [+] | Squirtle, Wartortle, Blastoise, Tirtouga | Escama de tartaruga | 40 min | habitat com água | Mine: a tartaruga solta a escama ao crescer. Real: casco troca placas |
| [-] | Mareep, Wooloo, Dubwool | Lã | 15 min | | Real: algumas ovelhas soltam a lã sozinhas. **Conferir** se o Cobblemon já tosquia essas espécies |
| [-] | Decidueye | Flecha | 15 min | | Pokédex: atira penas como flechas. A pena já sai na escova |
| [+] | Mandibuzz | Osso | 20 min | | Pokédex: junta ossos e se enfeita com eles. Real: o abutre-barbudo junta ossos. Não é órgão dele |
| [-] | Cubone, Marowak | Osso | | | O osso é da mãe: pesado demais |
| [-] | Aves da escova (Pidgey, Rowlet, Starly...) | Pena | | | Repetido: o Cobblemon dá com a escova |
| [-] | Sandshrew, Sandslash, Jangmo-o... | Escama de tatu | | | Repetido: o Cobblemon dá com a escova |
| [-] | Milotic, Huntail, Dragonair... | Prism Scale, Deep Sea Scale, Dragon Scale | | | Escama de evolução: é o corpo; e quebraria o balanceamento |
| [-] | Qualquer | Couro, pele de coelho, pé de coelho | | | Mine: só matando |

## Plantas, frutas e adubo

| | Quem | Item | A cada | Condição | Por quê |
|---|---|---|---|---|---|
| [x] | Bibarel | Tronco da madeira do bioma | 15 min | habitat com água, terra e árvores; bioma de floresta | Já implementada (castor rói a madeira) |
| [x] | Bidoof | 2 gravetos | 10 min | habitat com água e árvores | Já implementada |
| [-] | Tropius | Maçã | 15 min | | Real/Pokédex: o cacho de frutas do pescoço cresce e cai, e cresce de novo |
| [+] | Paras, Parasect | Cogumelo vermelho | 15 min | habitat escuro, com lama e cogumelos | Real: o fungo das costas solta corpos de frutificação |
| [+] | Shroomish, Breloom, Foongus, Amoonguss | Cogumelo marrom ou vermelho | 15 min | habitat com lama e cogumelos | Mesma lógica. Toedscool já tem no Cobblemon |
| [+] | Morelull, Shiinotic | Líquen brilhante | 20 min | habitat escuro | Mine: o líquen cresce sozinho na caverna. Real: fungo que brilha |
| [+] | Corsola | Leque de coral (do bioma quente) | 20 min | habitat com água, bioma quente | Pokédex: os galhos quebram e crescem de novo |
| [+] | Corsola de Galar | Leque de coral morto | 20 min | habitat com água | Mesma lógica, coral morto |
| [-] | Herbívoros grandes (Tauros, Miltank, Mudsdale, Rhyhorn...) | Farinha de osso | 15 min | habitat com plantas | Mine: o compostor faz farinha de planta. Real: esterco aduba |
| [-] | Swinub, Piloswine, Mamoswine | Semente de tocha-flor ou vagem de jarro | 40 min | pisando em terra | Mine: o farejador cava essas sementes antigas. Pokédex: fuçam o chão com o focinho |
| [-] | Lechonk, Oinkologne | Batata ou cenoura | 15 min | pisando em terra | Real: porco fuça raízes. Pokédex: acha comida pelo cheiro |
| [-] | Pancham, Pangoro | Bambu | | | Comem bambu, não produzem |
| [-] | Applin e evoluções | Maçã / Sweet e Tart Apple | | | A maçã é a casa/o corpo dele |
| [-] | Grotle, Lotad, Tangela, Cacnea, Hoppip... | Frutas, vitória-régia, vinha, cacto, flores | | | Repetido: o Cobblemon dá com farinha de osso |

## Achados (habilidade Pickup)

| | Quem | Item | A cada | Condição | Por quê |
|---|---|---|---|---|---|
| [+] | Zigzagoon, Linoone, Aipom, Teddiursa, Phanpy, Munchlax, Pachirisu, Lillipup, Bunnelby, Dedenne, Pikipek | Item comum sorteado (graveto, sílex, osso, linha, pena, semente, pepita...) | 20 min | | Habilidade Pickup: acha itens por aí. Mine: o gato traz presentes. Precisa de lista de sorteio nova na configuração |

## Ideias com outras condições

| | Quem | Item | Condição | Observação |
|---|---|---|---|---|
| [+] | Garbodor | Couro | carne podre | ideia do ROADMAP |
| [+] | Sapos (Froakie, Politoed...) + Slugma | Anfibrilho | os dois no mesmo pasto | Mine: sapo come cubo de magma pequeno e solta anfibrilho; cor pelo bioma |
| [-] | Clamperl, Shellder | Pérola / cristal de prismarinho | | Não há pérola no Mine nem no Cobblemon 1.8; prismarinho é corpo do guardião |
| [+] | Koffing | Pólvora | | Mine: órgão do creeper |
| [-] | Combusken | Ovo | | Já entra no ovo das aves (`egg_laying_species`) |

## O que o Cobblemon 1.8 já dá por interação (não repetir)

- **Escova**: penas (aves), lã (Altaria, Swablu, Wigglytuff), linha (Cottonee, Whimsicott, Eldegoss, Tarountula,
  Spidops, Wyrdeer), Shed Shell (cobras, lagartos, Dratini), escama de tatu (Sandshrew, Jangmo-o), slimeball (Goomy),
  carvão (Rolycoly), areia (Hippopotas, Sandygast), açúcar (Swirlix, Alcremie), bola de neve (Vanillite), cogumelo
  marrom (Toedscool), pó de blaze (Volcarona), membrana de phantom (Dreepy)
- **Farinha de osso**: mudas (Abomasnow, Exeggutor, Trevenant), cacto (Cacnea), alga (Dhelmise), frutas vermelhas
  (Grotle, Snover), dente-de-leão (Hoppip), vitória-régia (Lotad), roseira (Roselia), girassol (Sunflora), vinha
  (Tangela), Revival Herb (Petilil)
- **Balde**: leite (Miltank, Bouffalant, Gogoat, Skiddo), lava (Numel, Camerupt, Slugma, Magcargo), neve fofa (Rotom,
  Vanilluxe)
- **Garrafa**: Moomoo Milk (Miltank) e mel (Vespiquen)
- **Tesoura**: Furfrou, lã (Mareep, Wooloo)
