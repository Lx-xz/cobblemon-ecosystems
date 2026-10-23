# Cobblemon Ecosystems — Roadmap

Ideia central: os Pokémon substituem os animais do Minecraft. O mundo não pode perder nenhuma mecânica do
vanilla por causa disso, e os Pokémon devem parecer criaturas vivas, não só "mobs de batalha".

Este arquivo guarda só o que **falta fazer**, o que **falta testar** e as **decisões** que guiam o mod.
O que já foi feito está no [README.md](README.md) e no [CHANGELOG.md](CHANGELOG.md).

Legenda: `[ ]` a fazer · **(decidir)** precisa de decisão antes · ⭐ prioridade

---

## Regras do projeto

- **Remoção de mobs**: só sai o mob que tem uma contraparte Pokémon clara. Mobs fantasiosos, ou que sustentam
  alguma mecânica sem substituto, ficam.
- **Nenhuma mecânica perdida**: o que o animal fazia (ovos, mel, lã, leite...) tem que voltar por algum Pokémon.
- **Sem farm infinita**: produção lenta, com tempo de espera; spawners viram ninhos com número fixo de Pokémon.
- **Pokémon de habitantes** (aldeões, mercador, bruxa) são incapturáveis; ganhar um é por batalha ou missão.
- Cada recurso tem um liga/desliga na configuração.

| Mob | Decisão | Motivo |
|---|---|---|
| Guardião / Guardião Ancião | **Fica** | Peixe fantasioso, e a única fonte de prismarinho e esponja |
| Endermite | **Fica** | Sem contraparte Pokémon, e as farms de enderman dependem dele |
| Devastador (ravager) | **Fica, por enquanto** | Só sai se um Pokémon casar com os saqueadores (seção 7) |
| Phantom | **Fica** | Morto-vivo fantasioso |
| Cavalo esqueleto / zumbi, Zoglin | **Ficam** | Mortos-vivos |
| Slime / Magma Cube | **Sai** (`remove_slimes`, ligada por padrão) | Grimer, Goomy e Solosis dão slimeball; Slugma, Magcargo, Numel e Camerupt dão creme de magma |

### O que o Cobblemon 1.8.2 já faz (não repetir)
- **Sono por espécie** (`behaviour.resting`): hora de dormir, luz e blocos. Os noturnos (Oddish, Hoothoot) dormem
  de dia; o **Oddish dorme enterrado** quando está em terra ou grama. O habitat usa esses horários para chamar os
  Pokémon de volta para casa.
- **Bandos** (`behaviour.herd`): Pokémon da mesma linha andam juntos, seguindo um líder.
- **Interações com item na mão** (`pokemon_interactions`), com tempo de espera:
  - **Escova** (136 espécies): penas, lã, Shed Shell, açúcar...
  - **Farinha de osso** (20): mudas, cacto, alga, frutas vermelhas...
  - **Balde**: leite (Miltank, Bouffalant, Gogoat), lava (Numel, Camerupt, Magcargo), neve fofa (Rotom)
  - **Garrafa**: Moomoo Milk (Miltank) e mel (Vespiquen); **tesoura** no Furfrou
  - As nossas farms são passivas (no pasto, ligadas ao habitat) e não devem repetir esses itens.
- **Habitat pools**: estruturas naturais onde Pokémon nascem (montes de insetos, colmeias de terra, círculos de fada...).
- Não achei Kakuna pendurado na 1.8.2 (o Kakuna só tem as poses em pé e dormindo). O Zubat tem uma animação de
  "roost" (de cabeça para baixo) que o Cobblemon ainda não usa.

---

## 1. A testar no jogo

Feito, mas ainda sem teste no jogo.

Nada pendente: tudo o que estava aqui já foi testado.

Comandos de teste:
```
/summon minecraft:villager ~ ~ ~ {NeoForgeData:{"cobblemon_ecosystems:adopt":1b,"cobblemon_ecosystems:adopt_species":"pichu"}}
/summon iron_golem ~ ~ ~ {NeoForgeData:{"cobblemon_ecosystems:guardian_replace":"STRUCTURE","cobblemon_ecosystems:guardian_species":"golurk"}}
/summon witch ~ ~ ~ {NeoForgeData:{"cobblemon_ecosystems:hut_witch":1b}}
/summon cat ~ ~ ~ {NeoForgeData:{"cobblemon_ecosystems:replace":1b,"cobblemon_ecosystems:witch_cat":1b,"cobblemon_ecosystems:replace_shiny":1b}}
/summon wandering_trader ~ ~ ~ {UUID:[I;1,2,1,514]}
```
O `replace_shiny` e o `replace_evolved` valem para qualquer animal de estrutura (cavalo, vaca, galinha...).
O UUID do mercador cai no shiny.

---

## 2. Vilas

### Currais ⭐
- [ ] **Aldeões pastoreando os Pokémon**: pastor escova e tosquia Wooloo e Mareep, fazendeiro ordenha Miltank e
      recolhe os ovos, Pokémon dos cercados comem feno e seguem o aldeão no horário de trabalho
- [ ] Pokémon dos cercados **incapturáveis**, com uma **missão** para ganhar um (ex.: vencer o Combusken do fazendeiro
      para levar um Torchic), em vez de vários de graça
- [ ] Pokémon da vila ficam calmos perto de aldeões e fogem de zumbis

### Aldeões com Pokémon
- [ ] **Nomes de aldeões**: cada aldeão com pet ganha um nome ("Ash, o Ferreiro") e o pet leva o nome dele
      ("Aron do Ash"), para saber qual Pokémon é de qual aldeão
- [ ] Pokémon do aldeão ajudam na profissão (Timburr carrega blocos, Bulbasaur acelera plantações)
- [ ] Máquina de cura: tocar a animação e o som de cura de verdade quando o aldeão cura o pet
- [ ] Aldeões de reprodução também adotarem (hoje só os nascidos com a vila)

### Guardiões
- [ ] Golett e Golurk desafiáveis como "guardião da vila"
- [ ] Craft do Golett (ex.: abóbora sobre blocos de argila ou terra, como o golem de barro da lenda)
- [ ] Golurk "cansado" volta para o centro da vila (sino) em vez do ponto onde nasceu
- [ ] Growlithe ajudando os guardiões (como os cães policiais do anime)

---

## 3. Devolver mecânicas que os animais faziam

### Abelhas
- [ ] Mexer na colmeia sem fogueira embaixo deixa os moradores bravos (depende do revide, seção 6)
- [ ] Toque Suave: levar a colmeia com os Pokémon dentro (hoje quebrar sempre solta todos)
- [ ] **(decidir)** Beedrill fora das colmeias (é vespa, não faz mel)
  - Kakuna penduradas nas árvores de florestas, e Beedrill defendendo a área (território, seção 6)
  - Beedrill como predador que de vez em quando ataca colmeias de Combee, e a Vespiquen defende

### Galinhas
- [ ] **Combusken** bota ovos de tempos em tempos (pasto e currais). Torchic não (é pintinho); Chansey não
      (o ovo dela não é de galinha). O 1.21.1 só tem o ovo comum

### Gatos
- [ ] Presente da manhã: o Pokémon gato do jogador traz um item ao acordar (Meowth traz pepita de ouro)

### Sapos e anfibrilho (froglight)
O Cobblemon já dá anfibrilho ao derrotar Froakie, Frogadier, Greninja, Politoed, Tadbulb e Bellibolt.
- [ ] **Na batalha**: Slugma derrotado por um Pokémon sapo solta anfibrilho com 100% de chance (como o sapo
      vanilla com o magma cube)
- [ ] **No pasto**: o sapo lambe o Slugma (Lick) e cospe um anfibrilho, mais devagar, sem machucar
- [ ] Cor pelo bioma, como no vanilla: temperado = ocre, frio = verdejante, quente = perolado
- [ ] Também com magma cube pequeno, se os slimes estiverem ligados

### Outros animais
- [ ] **Sniffer**: Swinub, Piloswine e Lechonk desenterram sementes antigas (torchflower, pitcher) e cogumelos
- [ ] **Cabras**: Skiddo e Gogoat dão chifre de cabra ao dar cabeçada em pedra ou madeira
      (tentado e tirado: no teste nenhum dava cabeçada)
- [ ] **Golfinhos**: Lapras, Wailmer, Dewgong e Primarina dão "graça do golfinho"; Wailmer e Lapras levam a naufrágios
- [ ] **Axolotes**: Wooper e Mudkip ajudam a lutar contra afogados embaixo d'água
- [ ] **Papagaios**: Chatot imita sons de mobs hostis por perto; Oricorio e Chatot dançam perto de jukebox
- [ ] **Tartarugas**: Squirtle, Tirtouga e Chewtle voltam à praia onde nasceram
- [ ] **Raposas**: Nickit e Vulpix roubam comida ou itens do chão e fogem; dormem de dia
- [ ] **Pandas**: Pancham e Pangoro comem bambu e espirram (slimeball, como o panda filhote)
- [ ] **Jóquei de aranha**: o esqueleto montar num Ariados?

---

## 4. Fazendas no pasto (Pasture Block do Cobblemon)

As ideias de farms, a regra que elas seguem e o que já está feito ficam em [docs/farms.md](docs/farms.md).

---

## 5. Flora — Pokémon planta

- [ ] Tipo Planta aceleram plantações em volta (Bulbasaur, Oddish, Bellsprout, Chikorita, Sunkern)
- [ ] Seedot e Sunkern plantam mudas e sementes por onde andam
- [ ] **Fotossíntese**: se curam devagar sob o sol e ficam mais lentos à noite
- [ ] Sunflora vira o rosto para o sol; Bellossom dança quando faz sol
- [ ] Cherrim muda de forma com o tempo (sol ou nublado)
- [ ] Sudowoodo finge ser árvore: congela quando o jogador olha e foge da água
      (tentado e tirado: parava de andar, mas sem a pose)
- [ ] Shroomish, Breloom, Paras e Parasect espalham cogumelos e micélio
- [ ] Foongus e Amoonguss se disfarçam de Pokébola no chão
- [ ] Lotad cria vitórias-régias na água
- [ ] Sewaddle e Swadloon comem folhas de árvores
- [ ] Phantump e Trevenant perto de árvores mortas ou florestas escuras
- [ ] Pokémon planta evitam fogo e Pokémon tipo Fogo
- [ ] Snover e Abomasnow deixam neve por onde passam em biomas frios

---

## 6. Combate e vida selvagem

O Cobblemon marca 564 espécies como "se defendem" (`willDefendSelf`), mas só 3 brigam corpo a corpo. O mod já
faz Pokémon caçarem com os próprios golpes (`Hunts`), então dá para reaproveitar.

- [ ] **Revide**: Pokémon selvagens que apanham do jogador revidam
- [ ] **Temperamento por espécie**: agressivos (Tauros, Gyarados, Beedrill, Mankey), medrosos (Pidgey, Rattata, Wooloo) e neutros
- [ ] Natureza influencia o temperamento (Adamant e Brave mais bravos; Timid foge)
- [ ] Bando defende o filhote: atacar um bebê deixa os adultos bravos
- [ ] **Predador e presa**: Persian caça Rattata, Pidgeot caça Magikarp; presas fogem
- [ ] **Território**: Beedrill e Kakuna defendem a área das árvores onde ficam
- [ ] Pokémon selvagens atacam mobs hostis que chegam perto

---

## 7. Saqueadores e bruxas com Pokémon

- [ ] Saqueadores com Houndour, Houndoom ou Mightyena
- [ ] Vindicadores com Pawniard ou Bisharp (lâminas, combinam com o machado)
- [ ] Bruxas comuns (fora da cabana) com Murkrow, Honchkrow ou Mismagius
- [ ] Ilusionista com Zorua ou Zoroark
- [ ] Evocador com Shuppet ou Gastly
- [ ] **(decidir)** Devastador → **Rhyhorn / Rhydon**, montado pelos saqueadores nas invasões. Se casar bem, o devastador sai

---

## 8. Rotina e ambiente

- [ ] Zubat e Woobat dormem de cabeça para baixo no teto (usar a animação "roost" que o Cobblemon já tem)
- [ ] **Chuva**: tipo Fogo procuram abrigo; tipo Água e Lotad ficam felizes
- [ ] **Tempestade**: tipo Elétrico atraídos por para-raios e agitados com raios
- [ ] Pokémon vão beber água de vez em quando
- [ ] Selvagens comem frutas das árvores de fruta do Cobblemon
- [ ] **Atrair com comida**: selvagens seguem quem segura a comida favorita (como vacas seguem trigo)
- [ ] Bebês seguem os adultos da mesma linha evolutiva
- [ ] Bandos de Wingull e Pelipper voando sobre o mar
- [ ] Magikarp pulando na água e subindo cachoeiras
- [ ] Murkrow e Meowth pegam itens brilhantes do chão
- [ ] Sableye comem minérios expostos nas cavernas
- [ ] Rattata e Bunnelby comem plantações (praga); gatos espantam eles
- [ ] Snorlax dorme no meio do caminho e come plantações
- [ ] Ditto se transforma no Pokémon mais próximo

---

## 9. Mercador ambulante

- [ ] Montaria por bioma (Camerupt no deserto, Mamoswine na neve, Rapidash nas planícies), com manta própria
- [ ] Trocas com itens do Cobblemon (frutas, apricorns, Pokébolas)
- [ ] Pokémon do mercador com nível que acompanha o progresso do jogador

---

## 10. Ideia futura: couro sintético (analisada, não implementar agora)

Couro é pele, então só deveria sair derrotando um Pokémon (como já acontece no Cobblemon). Uma alternativa sem
violência precisa de um material **diferente**.

**Melhor candidato: Trubbish e Garbodor** (feitos de lixo e sacos plásticos). No pasto, Garbodor processa carne
podre e solta **Couro Sintético**, um item novo, aceito nas receitas de couro por receita alternativa ou tag.
Dá utilidade à carne podre e tem justificativa no Pokémon. Falta decidir: armadura igual à normal? tempo de
produção? textura do item.

Descartados: Grimer e Muk (já dão slimeball), receita na fogueira (sem Pokémon), couro de coelho (também sai
derrotando Pokémon).
