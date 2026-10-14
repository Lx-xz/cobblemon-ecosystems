# Changelog

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
