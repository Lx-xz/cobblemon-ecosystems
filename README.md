# Cobblemon Ecosystems

**Pokémon ecosystems for Cobblemon · Ecossistemas Pokémon para o Cobblemon**

![Mudbray with the Wandering Trader's blanket](docs/images/mudbray-trader.png)

<details>
<summary><b>🇺🇸 English</b></summary>

Pokémon are the animals — and the plants, and the creatures of the wild. This add-on removes vanilla animals and lets Pokémon take their place as the living fauna of Minecraft, sharing the world with villagers, traders and everything else. Hostile monsters and villagers stay.

### ✅ Implemented
- **No vanilla animals**: passive animals, hoglins and spiders no longer spawn, in every biome
- **Every source blocked**: spawn eggs, breeding, village cats, `/summon` and animals saved in old worlds
- **Config file** (`config/cobblemon_ecosystems-common.toml`, commented in English and Portuguese): choose to remove passive animals, hostile animals (spiders, hoglins) and/or slimes, and turn each feature on or off
- **In-game config screen**: Mods → Cobblemon Ecosystems → Config, with translated names
- **Editable lists**: data tags (`removed_animals`, `removed_hostile_animals`, `removed_slimes`) you can change with a datapack, plus lists in the config for the removed mobs, the Pokémon of each village pen, villager partners, head riders, nests, infested blocks, outpost captives, the witch's team, egg-laying species and dog, wolf and sheep Pokémon. Weights set how common each one is
- **Villages with Pokémon, by biome**: pen and stable animals become Pokémon that fit the village
  - Horses: Mudbray (plains), Ponyta (desert), Blitzle (savanna), Stantler (snowy), Deerling (taiga)
  - Cows, pigs, sheep and cats too: Miltank, Tauros, Bouffalant, Lechonk, Spoink, Tepig, Swinub, Wooloo, Mareep, Skiddo, Meowth, Litleo, Shinx, Glameow, Sneasel, Purrloin, Skitty, Espurr
  - One species per village, with rarer species less likely; 20% chance of an evolved form
  - **Chickens become egg-laying birds**: Doduo and Psyduck are common, Delibird in the snow, Torchic is rare and Piplup very rare in the snow; the females lay an egg every 5 to 10 minutes, in pens and in pastures
- **Golurk and Golett guard villages** instead of iron golems; iron golems can no longer be built with a pumpkin (snow golems still can)
  - They are stronger, regenerate and come in numbers that match the village size
- **Villagers with Pokémon partners** matching their profession (and a Pichu riding on a villager's head!)
  - The partner follows its villager, gets petted, defends them from monsters and rests in its Poké Ball when defeated
  - **Battle villagers**: sneak + right-click, or send out a Pokémon at them. Each villager has a personality: peaceful ones refuse, challengers bring stronger teams
  - Win for trade discounts (or emeralds from unemployed villagers); after a battle they rest for 2 days, or 1 day if there is a Cobblemon Healing Machine nearby
  - A villager turned zombie keeps their partner, and gets it back when cured
- **Pasture habitats**: the space Pokémon can reach around the Pasture Block (up to 10 blocks, walls and fences close it) is their home
  - Each type likes and dislikes some surroundings (Fire likes lava and dislikes water, Water likes a pool, Steel likes iron blocks...), and some species have their own taste
  - That sets how comfortable each Pokémon is: Happy, Comfortable, Neutral or Uncomfortable; happy ones sparkle, uncomfortable or lost ones shed tears
  - **See the habitat** with the house button in the Pokémon interaction wheel, or sneak + right-click a pastured Pokémon holding the Pokédex
  - **Temperament by nature**: homebodies stay close, explorers roam far, and everyone comes home at their species' bedtime
  - Pasture Pokémon walk back when they leave the area, instead of being sent to the PC
  - **Passive farms**: comfortable Pokémon drop items on their own: Fire Pokémon on heat blocks give blaze powder, Bibarel gives the wood of the forest biome, Bidoof gives sticks
  - A **habitats book** on first join lists what each type likes and dislikes
- **Bees**: Combee, Vespiquen, Cutiefly and Ribombee live in beehives, visit flowers, pollinate crops and make honey, in the wild and in a pasture (with a crafted beehive)
- **The witch's black cat**: the swamp hut cat becomes a black Purrloin with orange eyes (white with purple eyes when shiny), and stays black as Liepard
  - The witch is neutral and keeps it as her pet; it can't be caught or hurt by players
  - **Battle the witch** to win it; she adopts a new one after 3 days
- **Caged Pokémon in pillager outposts**: Golett, Growlithe, Ponyta, Mudbray, Riolu, Eevee, Larvitar or Absol
- **Silverfish become Dwebble**: angry Dwebble come out of infested blocks and call the others hiding nearby
- **Spawners become nests**: spider spawners leave 3 to 5 Spinarak, Joltik or Tarountula; the stronghold silverfish spawner leaves Dwebble
- **Dog Pokémon hunt skeletons**, and wild wolf Pokémon (Poochyena, Mightyena, Lycanroc) sometimes hunt wild sheep Pokémon
- **Wandering Trader**: arrives with two Mudbray on leads instead of llamas, wearing an exclusive trader blanket, and no longer sells buckets of removed fish
  - **Shiny trader** (1 in 512): purple robe, purple blankets and both Mudbray shiny
- **Battle the trader**: sneak + right-click him, or send out a Pokémon at him or his Mudbray
  - While you battle, he stands still and hides his real Mudbray
  - Win and you can catch one of his Mudbray
- **Trader blanket as a unique look**: it stays after capture and on Mudsdale after evolving
- Works on dedicated servers as well as singleplayer

> Cat Pokémon scaring away creepers and phantoms, and skeletons fleeing from dog Pokémon, already come with Cobblemon itself.

### 🗺️ Planned
- Villagers herding the pen Pokémon, and quests to earn one instead of catching them
- Pillagers, vindicators, witches and illusioners with their own Pokémon
- More pasture farms: Spinarak webs, Solosis slime balls
- Frog Pokémon and Slugma giving froglights
- **Flora**: Grass Pokémon tending crops, spreading mushrooms and living by the sun
- Wild Pokémon fighting back, predators and prey, day and night routines

The full checklist is in [ROADMAP.md](ROADMAP.md) (Portuguese).

### Requirements
- Minecraft 1.21.1 · NeoForge
- Cobblemon 1.8+
- Kotlin for Forge

### Notes
- Back up existing worlds: tamed vanilla pets (wolves, cats, horses) are removed when loaded
- Village Pokémon only appear in villages generated after installing the mod
- Iron farms stop working, since villagers no longer summon iron golems

</details>

<details>
<summary><b>🇧🇷 Português</b></summary>

Os Pokémon são os animais, e também as plantas e as criaturas selvagens. Este add-on remove os animais vanilla e deixa os Pokémon ocuparem o lugar deles como a fauna viva do Minecraft, convivendo com aldeões, mercadores e todo o resto. Monstros hostis e aldeões continuam.

### ✅ Pronto
- **Sem animais vanilla**: animais passivos, hoglins e aranhas não nascem mais, em nenhum bioma
- **Todas as fontes bloqueadas**: ovos de spawn, reprodução, gatos de vila, `/summon` e animais salvos em mundos antigos
- **Arquivo de configuração** (`config/cobblemon_ecosystems-common.toml`, comentado em inglês e português): escolha remover animais passivos, animais hostis (aranhas, hoglins) e/ou slimes, e ligue ou desligue cada recurso
- **Tela de configuração no jogo**: Mods → Cobblemon Ecosystems → Config, com nomes traduzidos
- **Listas editáveis**: tags (`removed_animals`, `removed_hostile_animals`, `removed_slimes`) que podem ser mudadas por datapack, e listas na configuração para os mobs removidos, os Pokémon de cada curral, os parceiros dos aldeões, quem anda na cabeça, ninhos, blocos infestados, Pokémon presos nos postos, o time da bruxa, espécies que botam ovos e os Pokémon cachorro, lobo e ovelha. Os pesos dizem o quanto cada um é comum
- **Vilas com Pokémon, por bioma**: animais dos currais e estábulos viram Pokémon que combinam com a vila
  - Cavalos: Mudbray (planície), Ponyta (deserto), Blitzle (savana), Stantler (neve), Deerling (taiga)
  - Vacas, porcos, ovelhas e gatos também: Miltank, Tauros, Bouffalant, Lechonk, Spoink, Tepig, Swinub, Wooloo, Mareep, Skiddo, Meowth, Litleo, Shinx, Glameow, Sneasel, Purrloin, Skitty, Espurr
  - Uma espécie por vila, com as mais raras menos prováveis; 20% de chance de vir evoluído
  - **Galinhas viram aves que botam ovos**: Doduo e Psyduck são comuns, Delibird na neve, Torchic é raro e Piplup muito raro na neve; as fêmeas botam um ovo a cada 5 a 10 minutos, nos currais e no pasto
- **Golurk e Golett protegem as vilas** no lugar dos golens de ferro; o golem de ferro não é mais montado com abóbora (o de neve continua)
  - São mais fortes, se regeneram e aparecem em número de acordo com o tamanho da vila
- **Aldeões com Pokémon parceiros** de acordo com a profissão (e um Pichu na cabeça de um aldeão!)
  - O parceiro segue o aldeão, ganha carinho, defende ele dos monstros e descansa na Pokébola quando é derrotado
  - **Batalha com aldeões**: agache e clique com o botão direito, ou lance um Pokémon neles. Cada aldeão tem uma personalidade: os pacíficos recusam, os desafiadores têm times mais fortes
  - Vença para ganhar desconto nas trocas (ou esmeraldas dos aldeões sem profissão); depois da batalha eles descansam 2 dias, ou 1 dia se houver uma Máquina de Cura do Cobblemon por perto
  - Aldeão que vira zumbi guarda o parceiro, e recebe ele de volta quando é curado
- **Habitats do pasto**: o espaço que os Pokémon alcançam em volta do bloco do pasto (até 10 blocos, paredes e cercas fecham) é a casa deles
  - Cada tipo gosta e não gosta de certas coisas em volta (Fogo gosta de lava e não gosta de água, Água gosta de uma piscina, Metal gosta de blocos de ferro...), e algumas espécies têm gosto próprio
  - Isso decide o conforto de cada Pokémon: Feliz, Confortável, Neutro ou Desconfortável; os felizes brilham, os desconfortáveis ou perdidos choram
  - **Ver o habitat** pelo botão de casinha no menu radial do Pokémon, ou agachando e clicando com o botão direito num Pokémon do pasto segurando a Pokédex
  - **Temperamento pela natureza**: caseiros ficam perto, exploradores passeiam longe, e todos voltam para casa na hora de dormir da espécie
  - Pokémon do pasto voltam andando quando saem da área, em vez de irem para o PC
  - **Farms passivas**: Pokémon confortáveis soltam itens sozinhos: os de Fogo pisando em blocos de calor dão pó de blaze, o Bibarel dá a madeira do bioma de floresta, o Bidoof dá gravetos
  - Um **livro dos habitats** ao entrar no mundo lista do que cada tipo gosta e não gosta
- **Abelhas**: Combee, Vespiquen, Cutiefly e Ribombee moram nas colmeias, visitam flores, polinizam plantações e fazem mel, na natureza e no pasto (com uma colmeia craftada)
- **Gato preto da bruxa**: o gato da cabana do pântano vira um Purrloin preto de olhos laranja (branco de olhos roxos quando shiny), e continua preto como Liepard
  - A bruxa é neutra e fica com ele de estimação; ele é incapturável e não leva dano de jogadores
  - **Batalhe com a bruxa** para ganhá-lo; ela adota outro depois de 3 dias
- **Pokémon presos nos postos de saqueadores**: Golett, Growlithe, Ponyta, Mudbray, Riolu, Eevee, Larvitar ou Absol
- **Traças viram Dwebble**: Dwebble bravos saem dos blocos infestados e chamam os outros escondidos por perto
- **Spawners viram ninhos**: spawners de aranha deixam 3 a 5 Spinarak, Joltik ou Tarountula; o spawner de traça da fortaleza deixa Dwebble
- **Pokémon cachorro caçam esqueletos**, e os Pokémon lobo selvagens (Poochyena, Mightyena, Lycanroc) às vezes caçam Pokémon ovelha selvagens
- **Mercador ambulante**: chega com dois Mudbray na guia no lugar das lhamas, com uma manta exclusiva do mercador, e não vende mais balde dos peixes removidos
  - **Mercador shiny** (1 em 512): manto roxo, mantas roxas e os dois Mudbray shiny
- **Batalha com o mercador**: agache e clique com o botão direito nele, ou lance um Pokémon nele ou nos Mudbray
  - Durante a batalha, ele fica parado e esconde os Mudbray de verdade
  - Vença e você pode capturar um dos Mudbray
- **Manta como visual único**: continua depois da captura e no Mudsdale depois de evoluir
- Funciona em servidor dedicado, além do singleplayer

> Pokémon gato espantando creepers e phantoms, e esqueletos fugindo de Pokémon cachorro, já vêm no próprio Cobblemon.

### 🗺️ Planejado
- Aldeões pastoreando os Pokémon dos cercados, e missões para ganhar um em vez de capturar
- Saqueadores, vindicadores, bruxas e ilusionistas com seus próprios Pokémon
- Mais farms no pasto: teias do Spinarak, slimeball do Solosis
- Pokémon sapo e Slugma dando anfibrilho
- **Flora**: Pokémon planta cuidando das plantações, espalhando cogumelos e vivendo pelo sol
- Pokémon selvagens revidando, predadores e presas, rotinas de dia e de noite

A lista completa está no [ROADMAP.md](ROADMAP.md).

### Requisitos
- Minecraft 1.21.1 · NeoForge
- Cobblemon 1.8+
- Kotlin for Forge

### Observações
- Faça backup dos mundos: pets vanilla domados (lobos, gatos, cavalos) são removidos ao carregar
- Os Pokémon de vila só aparecem em vilas geradas depois de instalar o mod
- Farms de ferro param de funcionar, porque os aldeões não chamam mais golens de ferro

</details>

![Mudsdale with the trader blanket](docs/images/mudsdale-blanket.png)

## Development · Desenvolvimento

```bash
./gradlew build            # build/libs/cobblemon-ecosystems-<version>.jar
./gradlew runClient        # test client with Cobblemon and Kotlin for Forge
./gradlew runGameTestServer  # headless boot, checks that mods and mixins load
```

Blanket, shiny trader and witch cat textures are generated by the scripts in [`tools/`](tools/) (Python + Pillow).

## Credits

Code and texture scripts written with AI (Claude) under my design and direction; concept, decisions and testing are my own.

## License

[MPL-2.0](LICENSE)
