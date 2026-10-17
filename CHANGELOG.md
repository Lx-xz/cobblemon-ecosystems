# Changelog

## 1.1.1

<details>
<summary><b>🇺🇸 English</b></summary>

### New
- **Names with icons**: villagers with Pokémon get a name ("Ash the Armorer") and their Pokémon too ("Ash's Aron"); the wandering trader (with a star when shiny) and the swamp hut witch get one too. Names show up when you look at them
- **Battle hint and R key**: looking at someone you can battle shows "Sneak + right-click or R to battle"; pressing Cobblemon's send-out key (R) at them starts the challenge
- **Lost Pokémon**: a pastured Pokémon that can't find its way home gets lost instead of teleporting: it gets sad, you get a message with where it is, and once you get close it follows you home (`pasture_get_lost`)
- **`/habitats`** gives the habitats book again
- **Passive pasture farms**: comfortable Pokémon drop items on their own, faster when happy (they never break anything)
  - Fire Pokémon standing on heat blocks (magma, netherrack...): blaze powder every 10 min
  - Bibarel with water, earth and trees in the habitat: a log of the forest biome's wood every 15 min (birch in birch forests, spruce in taigas, dark oak in dark forests...)
  - Bidoof with water and trees: 2 sticks every 10 min
  - The habitat report shows what each Pokémon produces, when, and what is missing
- **Habitats book**: every player gets a book on first join with how habitats work and a page per type with what it likes and dislikes
- **Pasture habitats**: the space Pokémon can reach around the Pasture Block (up to 10 blocks, walls and fences close it) is their home
  - Each type likes and dislikes some surroundings: Fire likes lava and magma and dislikes water, Water likes a pool, Grass likes plants and open sky, Steel likes iron blocks... Some species have their own taste (Zubat and Woobat like darkness and a roof)
  - That sets how comfortable each Pokémon is: Happy, Comfortable, Neutral or Uncomfortable. Cramped pastures make them less comfortable
  - Happy Pokémon show green sparkles; uncomfortable or lost ones shed tears
  - **See the habitat**: a new house button in the Pokémon interaction wheel, or sneak + right-click a pastured Pokémon holding the Pokédex
  - **Temperament by nature**: homebodies barely leave the habitat, explorers roam far; everyone comes home at their species' bedtime (nocturnal ones in the morning). A Pokémon that can't find its way back gets sad for a while
  - Everything is editable in the config: radius, block groups, what each type likes, the two-type rule and the natures
- **In-game config screen**: Mods → Cobblemon Ecosystems → Config, with translated names
- **Pasture Pokémon walk back** to the pasture when they leave its area, instead of being sent to the PC (no more fences needed)
- **Pasture bees**: bee Pokémon in a pasture use a crafted beehive in the pasture area, visit the flowers there (getting covered in pollen), make honey and rest inside it at night and in the rain, still in the pasture
- **Egg-laying birds in a pasture**: female Doduo, Psyduck, Torchic, Delibird, Piplup and their evolutions lay eggs there too
- **The witch's Purrloin**: the swamp hut witch keeps the black Purrloin as her pet
  - She is neutral and only attacks who attacked her
  - The Purrloin follows her and can't be caught or hurt by players; if she dies, it runs away
  - **Battle the witch** (sneak + right-click, or send out a Pokémon at her or her cat) to win it; she adopts a new one after 3 days
- **Caged Pokémon in pillager outposts**: the iron golem cage holds a captive Pokémon (Golett, Growlithe, Ponyta, Mudbray, Riolu, Eevee, Larvitar or Absol)
- **Silverfish become Dwebble**: angry Dwebble come out of infested blocks, attack, and call the others hiding nearby
- **Spawners become nests**: spider spawners leave a nest of 3 to 5 spider Pokémon of one species (Spinarak, Joltik or Tarountula); the stronghold silverfish spawner leaves Dwebble
- New config options for each of these
- **Editable lists in the config**: which mobs are removed (ids, tags and exceptions), the Pokémon of each village pen by village type, villager partners by profession, head riders, spider nests, infested blocks, outpost captives, the witch's team, egg-laying species, and dog, wolf and sheep Pokémon. Weights set how common each one is

### Changed
- Only **female** birds lay eggs
- Only **wolf** Pokémon (Poochyena, Mightyena, Lycanroc) hunt wild sheep Pokémon; other dogs only hunt skeletons
- The wandering trader no longer sells pufferfish and tropical fish buckets while those fish are removed
- Slimes and magma cubes are now removed by default (`remove_slimes` is on); existing config files keep their current value

</details>

<details>
<summary><b>🇧🇷 Português</b></summary>

### Novidades
- **Nomes com ícones**: aldeões com Pokémon ganham nome ("Ash, o Armeiro") e o Pokémon também ("Aron do Ash"); o mercador ambulante (com estrela quando é shiny) e a bruxa da cabana também. O nome aparece ao olhar para eles
- **Dica de batalha e tecla R**: olhando para quem dá para desafiar aparece "Shift + clique direito ou R para batalhar"; apertar a tecla de soltar Pokémon do Cobblemon (R) mirando nele começa o desafio
- **Pokémon perdido**: o Pokémon do pasto que não acha o caminho de casa se perde em vez de ser teleportado: fica triste, você recebe um aviso com onde ele está, e chegando perto ele segue você até em casa (`pasture_get_lost`)
- **`/habitats`** dá o livro dos habitats de novo
- **Farms passivas no pasto**: Pokémon confortáveis soltam itens sozinhos, mais rápido quando felizes (sem quebrar nada)
  - Pokémon de Fogo pisando em blocos de calor (magma, netherrack...): pó de blaze a cada 10 min
  - Bibarel com água, terra e árvores no habitat: um tronco da madeira do bioma a cada 15 min (bétula nas florestas de bétula, abeto nas taigas, carvalho-escuro na floresta escura...)
  - Bidoof com água e árvores: 2 gravetos a cada 10 min
  - O relatório do habitat mostra o que cada Pokémon produz, quando, e o que está faltando
- **Livro dos habitats**: cada jogador ganha um livro ao entrar no mundo pela primeira vez, com como funcionam os habitats e uma página por tipo com do que ele gosta e não gosta
- **Habitats do pasto**: o espaço que os Pokémon alcançam em volta do bloco do pasto (até 10 blocos, paredes e cercas fecham) é a casa deles
  - Cada tipo gosta e não gosta de certas coisas em volta: Fogo gosta de lava e magma e não gosta de água, Água gosta de uma piscina, Planta gosta de plantas e céu aberto, Metal gosta de blocos de ferro... Algumas espécies têm gosto próprio (Zubat e Woobat gostam de escuro e de teto)
  - Isso decide o conforto de cada Pokémon: Feliz, Confortável, Neutro ou Desconfortável. Pasto apertado deixa menos confortável
  - Pokémon felizes soltam brilhos verdes; desconfortáveis ou perdidos choram
  - **Ver o habitat**: botão novo de casinha no menu radial do Pokémon, ou agache e clique com o botão direito num Pokémon do pasto segurando a Pokédex
  - **Temperamento pela natureza**: caseiros quase não saem do habitat, exploradores passeiam longe; todos voltam para casa na hora de dormir da espécie (os noturnos, de manhã). Quem não acha o caminho de volta fica triste por um tempo
  - Tudo editável na configuração: raio, grupos de blocos, do que cada tipo gosta, a regra dos dois tipos e as naturezas
- **Tela de configuração no jogo**: Mods → Cobblemon Ecosystems → Config, com nomes traduzidos
- **Pokémon do pasto voltam andando** quando saem da área, em vez de irem para o PC (não precisa mais cercar)
- **Abelhas no pasto**: Pokémon abelha no pasto usam uma colmeia craftada na área dele, visitam as flores de lá (ficando cobertos de pólen), fazem mel e descansam dentro dela à noite e na chuva, sem sair do pasto
- **Aves botam ovos no pasto**: fêmeas de Doduo, Psyduck, Torchic, Delibird, Piplup e evoluções botam ovos lá também
- **O Purrloin da bruxa**: a bruxa da cabana do pântano fica com o Purrloin preto
  - Ela é neutra e só ataca quem a atacou
  - O Purrloin segue a bruxa, é incapturável e não leva dano de jogadores; se ela morrer, ele foge
  - **Batalhe com a bruxa** (agache e clique com o botão direito, ou lance um Pokémon nela ou no gato) para ganhá-lo; ela adota outro depois de 3 dias
- **Pokémon presos nos postos de saqueadores**: a jaula do golem guarda um Pokémon (Golett, Growlithe, Ponyta, Mudbray, Riolu, Eevee, Larvitar ou Absol)
- **Traças viram Dwebble**: Dwebble bravos saem dos blocos infestados, atacam e chamam os outros escondidos por perto
- **Spawners viram ninhos**: spawners de aranha deixam um ninho com 3 a 5 Pokémon aranha de uma espécie (Spinarak, Joltik ou Tarountula); o spawner de traça da fortaleza deixa Dwebble
- Opções novas na configuração para cada uma dessas
- **Listas editáveis na configuração**: quais mobs saem (ids, tags e exceções), os Pokémon de cada curral por tipo de vila, os Pokémon dos aldeões por profissão, quem anda na cabeça, ninhos de aranha, blocos infestados, Pokémon presos nos postos, o time da bruxa, espécies que botam ovos e os Pokémon cachorro, lobo e ovelha. Os pesos dizem o quanto cada um é comum

### Mudanças
- Só as **fêmeas** das aves botam ovos
- Só os Pokémon **lobo** (Poochyena, Mightyena, Lycanroc) caçam Pokémon ovelha selvagens; os outros cachorros só caçam esqueletos
- O mercador ambulante não vende mais balde de baiacu nem de peixe tropical enquanto esses peixes estiverem removidos
- Slimes e cubos de magma agora saem por padrão (`remove_slimes` ligado); arquivos de configuração que já existem mantêm o valor atual

</details>

## 1.1.0

<details>
<summary><b>🇺🇸 English</b></summary>

### New
- **Config file** `config/cobblemon_ecosystems-common.toml`, commented in English and Portuguese
  - Remove passive animals, hostile animals (spiders, cave spiders, hoglins) and/or slimes (slime and magma cube, off by default)
  - Choose whether animals already saved in old worlds are removed
  - Turn each feature on or off: village Pokémon, guardians, villager partners, villager battles, trader Pokémon, trader battle, hive Pokémon, dog hunters
  - Evolved chance for village Pokémon and the adoption chance for villagers
- **Village Pokémon by biome**: each village type has its own species
  - Horses: Mudbray (plains), Ponyta (desert), Blitzle (savanna), Stantler (snowy), Deerling (taiga)
  - New species: Bouffalant, Spoink, Tepig, Swinub, Skiddo, Litleo, Shinx, Glameow, Sneasel
  - One species per village (instead of per pen), with rarer species less likely
- **Chickens become egg-laying birds**: Doduo and Psyduck (common), Delibird in snowy villages, Torchic (rare) and Piplup (very rare, snow). They lay an egg every 5 to 10 minutes
- **Witch's black cat**: the swamp hut Purrloin and Liepard are black with orange eyes; shiny ones are white with purple eyes
- **Shiny Wandering Trader** (1 in 512): purple robe, purple blankets with black trim, and both Mudbray come shiny
- **Battle villagers**: sneak + right-click a villager, or send out a Pokémon at them or their partner
  - Personalities: peaceful (refuse), friendly, competitive and challenger (stronger teams, up to 2 extra Pokémon)
  - Win for trade discounts; unemployed villagers and nitwits pay in emeralds
  - They rest for 2 days after a battle, or heal at a nearby Cobblemon Healing Machine and battle again after 1 day
- **Villager partners**: teleport back to their villager, get petted (hearts), defend their villager from monsters and rest in the Poké Ball instead of dying
  - A villager turned zombie keeps their partner and gets it back when cured
  - If the partner is really gone, the villager adopts a new one after 3 days

### Changed
- **Golurk and Golett** hit harder, take less damage from monsters, regenerate out of combat, and their number follows the village size (2 to 4)
- Hunt particles use the official colour of the move's type, with dirt and stone bursts for Ground and Rock moves

### Fixed
- Villager timers count sleeping and `/time` changes

</details>

<details>
<summary><b>🇧🇷 Português</b></summary>

### Novidades
- **Arquivo de configuração** `config/cobblemon_ecosystems-common.toml`, comentado em inglês e português
  - Remover animais passivos, animais hostis (aranhas, aranhas de caverna, hoglins) e/ou slimes (slime e cubo de magma, desligado por padrão)
  - Escolher se os animais já salvos em mundos antigos são removidos
  - Ligar ou desligar cada recurso: Pokémon das vilas, guardiões, parceiros dos aldeões, batalha com aldeões, Pokémon do mercador, batalha com o mercador, Pokémon das colmeias, cachorros caçadores
  - Chance de evoluído dos Pokémon das vilas e chance de adoção dos aldeões
- **Pokémon das vilas por bioma**: cada tipo de vila tem suas espécies
  - Cavalos: Mudbray (planície), Ponyta (deserto), Blitzle (savana), Stantler (neve), Deerling (taiga)
  - Espécies novas: Bouffalant, Spoink, Tepig, Swinub, Skiddo, Litleo, Shinx, Glameow, Sneasel
  - Uma espécie por vila (e não por cercado), com as mais raras menos prováveis
- **Galinhas viram aves que botam ovos**: Doduo e Psyduck (comuns), Delibird nas vilas de neve, Torchic (raro) e Piplup (muito raro, na neve). Botam um ovo a cada 5 a 10 minutos
- **Gato preto da bruxa**: o Purrloin e o Liepard da cabana do pântano são pretos de olhos laranja; os shiny são brancos de olhos roxos
- **Mercador ambulante shiny** (1 em 512): manto roxo, mantas roxas com detalhes pretos e os dois Mudbray shiny
- **Batalha com aldeões**: agache e clique com o botão direito no aldeão, ou lance um Pokémon nele ou no parceiro
  - Personalidades: pacífico (recusa), amigável, competitivo e desafiador (times mais fortes, até 2 Pokémon a mais)
  - Vença para ganhar desconto nas trocas; aldeões sem profissão e bobos pagam em esmeraldas
  - Eles descansam 2 dias depois da batalha, ou se curam numa Máquina de Cura do Cobblemon por perto e batalham de novo depois de 1 dia
- **Parceiros dos aldeões**: teleportam de volta para o aldeão, ganham carinho (corações), defendem o aldeão dos monstros e descansam na Pokébola em vez de morrer
  - Aldeão que vira zumbi guarda o parceiro e recebe ele de volta quando é curado
  - Se o parceiro sumir de verdade, o aldeão adota outro depois de 3 dias

### Mudanças
- **Golurk e Golett** batem mais forte, levam menos dano de monstros, se regeneram fora de combate e a quantidade acompanha o tamanho da vila (2 a 4)
- As partículas das caçadas usam a cor oficial do tipo do golpe, com terra e pedra nos golpes de Terra e Pedra

### Correções
- Os tempos dos aldeões contam o sono e as mudanças com `/time`

</details>
