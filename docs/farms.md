# Farms do pasto

**Edite à vontade** e me avise: eu passo as mudanças para o jogo.

Como funcionam hoje:
- O Pokémon precisa estar no **pasto** e pelo menos **Confortável** no habitat. **Feliz** produz mais rápido (60% do tempo).
- Ele **não quebra nada**: só solta o item no chão.
- Condições possíveis:
  - **pisando em** um grupo de blocos (`on:`), como calor;
  - **o habitat tem** um grupo de blocos (`near:`), como água, terra ou árvores;
  - **bioma** (a madeira do Bibarel).
- Na configuração, cada farm é uma linha de `pasture_farm_list`:
  `quem = item xQuantidade, every:minutos, on:grupo, near:grupo`.
- Não repetir o que o Cobblemon 1.8 já dá por **interação com item na mão** (escova, balde, farinha de osso, garrafa).
  Ver a seção no fim.

Grupos de blocos que existem (`habitat_features`): calor, água, plantas, flores, metal, pedra, terra, neve e gelo,
eletricidade, mato e flores (insetos), lama e cogumelos, almas e velas, ametista e livros, flores e ametista (fada),
treino, aconchego, pedra do End, árvores. Prontos: céu aberto, teto, escuro, luz forte, bioma frio, bioma quente, chuva.

## Já funcionando

| Quem | Item | A cada | Condição |
|---|---|---|---|
| Tipo Fogo | Pó de blaze | 10 min | pisando em calor (magma, netherrack...) |
| Bibarel | Tronco da madeira do bioma | 15 min | habitat com água, terra e árvores; só em bioma de floresta |
| Bidoof | 2 gravetos | 10 min | habitat com água e árvores |

Madeira por bioma: bétula (florestas de bétula), carvalho-escuro (floresta escura), cerejeira (bosque de cerejeiras),
mangue (pântano de mangue), abeto (taigas), selva (selvas), acácia (savanas), carvalho (florestas e pântano).

## Ideias

| Quem | Item | A cada | Condição | Observação |
|---|---|---|---|---|
| Spinarak, Ariados, Joltik, Galvantula | Teia nos cantos / linha | | habitat escuro, com troncos | a teia poderia aparecer como bloco num canto |
| Solosis, Grimer, Goomy | Slimeball | | lama ou água | |
| Magcargo, Slugma | Pedra em volta vira bloco de magma | | pisando em pedra | muda bloco: decidir se pode |
| Snorunt, Vanillite | Neve e gelo em volta | | bioma frio | muda bloco: decidir se pode |
| Shuckle | Suco de Fruta (fermenta frutas guardadas) | | baú com frutas por perto | como no jogo original |
| Clamperl, Shellder | Pérola / cristal de prismarinho | | habitat com água | |
| Tropius | Fruta do pescoço (maçã ou fruta Pokémon) | | | |
| Gastly, Haunter | Membrana de phantom | | à noite, escuro | |
| Sableye, Carbink | Minérios pequenos | | habitat de pedra | cuidado com balanceamento |
| Cottonee, Whimsicott | Linha (algodão) | | céu aberto | |
| Wooper, Mudkip | Bola de argila | | lama ou beira da água | |
| Diglett, Drilbur | Sílex | | pisando em cascalho | |
| Combusken | Ovo | | | Torchic não (é pintinho) |
| Garbodor | Couro sintético (item novo) | | carne podre num baú | ideia do ROADMAP |
| Sapos (Froakie, Politoed...) + Slugma | Anfibrilho | | os dois no mesmo pasto | cor pelo bioma |

## O que o Cobblemon 1.8 já dá por interação (não repetir)

- **Escova** (136 espécies): penas, lã, Shed Shell, açúcar...
- **Farinha de osso** (20): mudas, cacto, alga, frutas vermelhas...
- **Balde**: leite (Miltank, Bouffalant, Gogoat), lava (Numel, Camerupt, Magcargo), neve fofa (Rotom)
- **Garrafa**: Moomoo Milk (Miltank) e mel (Vespiquen)
- **Tesoura**: Furfrou
