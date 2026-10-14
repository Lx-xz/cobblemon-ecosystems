# Cobblemon Living Fauna & Flora — Roadmap

Ideia central: os Pokémon substituem os animais do Minecraft. O mundo não pode perder nenhuma mecânica do
vanilla por causa disso, e os Pokémon devem parecer criaturas vivas, não só "mobs de batalha".

Legenda: `[x]` feito · `[ ]` a fazer · **(decidir)** precisa de decisão antes

---

## 0. Já feito

- [x] Remover animais vanilla do spawn natural de todos os biomas (biome modifier + tag editável)
- [x] Bloquear animais de outras fontes (ovos, reprodução, gatos de vila, mundos antigos, `/summon`)
- [x] Hoglin removido (javali)
- [x] Aranhas e aranhas de caverna removidas (olho de aranha e linha continuam vindo de Spinarak, Ariados, Joltik, Galvantula, Tarountula, Spidops)
- [x] Mercador ambulante chega com 2 Mudbray na guia, no lugar das lhamas
- [x] Manta do mercador no Mudbray e no Mudsdale (camada de textura por aspect)
- [x] Nome "Mudbray do Mercador"
- [x] Batalha contra o mercador (agachar + clique, ou lançar Pokémon nele ou nos Mudbray)
- [x] Mercador parado e Mudbray escondidos durante a batalha
- [x] Vencer o mercador libera a captura de um Mudbray (skin de manta mantida após captura e evolução)
- [x] Animais de estruturas (currais, estábulos, camelos do deserto, gatos de vila e da bruxa) viram Pokémon
- [x] Mesmo cercado = mesma espécie; 20% de chance de vir evoluído
- [x] **Golett e Golurk no lugar do golem de ferro** (nascem com a vila e quando os aldeões chamam um golem; atacam mobs hostis menos creepers; não se afastam de casa; incapturáveis)
- [x] Golem de ferro não pode mais ser montado com abóbora (o de neve continua)
- [x] **Aldeões com Pokémon** (20% dos aldeões nascidos com a vila; por profissão; Honedge mais raro; pequenos ficam na cabeça)
- [x] **Pokémon abelha nas colmeias** (Combee, Cutiefly, Ribombee e a rainha Vespiquen; visitam flores, polinizam e enchem a colmeia de mel)
- [x] **Cachorros caçam esqueletos** (do jogador e selvagens); selvagens às vezes caçam Wooloo, Dubwool e Mareep selvagens
- [x] Corrigido: cachorros e Golurk ficavam parados (trocar os comportamentos do cérebro do Cobblemon apagava andar,
      olhar etc.). Agora a caçada é feita pelo próprio mod (`Hunts`): anda até o alvo e golpeia
- [x] Golett e Golurk atacam mobs hostis como o golem de ferro (menos creepers), com golpe que joga para cima
- [x] **Ícone de casinha** antes do nome dos Pokémon da vila incapturáveis (Golett, Golurk e Pokémon dos aldeões)
- [x] Pokémon na cabeça do aldeão não sufocam quando ele passa por lugares baixos
- [x] Combee que travava em cima da árvore: agora vai até a frente da colmeia e, se não achar caminho em 30 s, entra mesmo assim
- [x] Gatos espantam creepers e phantoms, e esqueletos fogem de cachorros: **o Cobblemon já faz isso** (campo `entityInteract` das espécies)

---

## 1. Base do mod

- [x] Renomear para **Cobblemon: Living Fauna & Flora** (mod id `living_fauna_flora`, pacote `com.luiz.livingfaunaflora`)
- [ ] Arquivo de configuração simples para o jogador escolher (ver seção 1.1)

### 1.1 Configuração (`config/living_fauna_flora.toml`)

Rascunho das opções. Cada grupo é uma tag editável, então dá para ajustar mob por mob com datapack.

```toml
[remocao]
animais_passivos = true    # vacas, porcos, ovelhas, peixes, gatos, cavalos...
animais_hostis   = true    # aranhas, hoglin, traça, endermite, guardiões...
slimes           = false   # slime e magma cube juntos (são parecidos)
remover_dos_mundos_antigos = true   # apagar os que já existem ao carregar o chunk

[mercador]
pokemon_de_carga = true
batalha          = true

[estruturas]
trocar_animais_por_pokemon = true
chance_evoluido = 0.2

[comportamentos]
# um liga/desliga por mecânica: abelhas, gatos, sapos, pasto...
```

- [ ] Grupos de remoção: passivos, hostis e slimes (slime + magma cube)
- [ ] Liga/desliga para cada módulo (mercador, estruturas, pastos, cada comportamento)
- [ ] Recarregar sem reiniciar o servidor, se possível
- [ ] Explicar cada opção em comentários no próprio arquivo
- [ ] Traduções (pt_br e en_us) em vez de textos fixos no código
- [ ] Testar em servidor dedicado (não só no singleplayer)
- [ ] Página do mod (descrição, imagens, lista de dependências)

---

## 2. Remoção de mobs

**Regra:** só sai o mob que tem uma contraparte Pokémon clara. Mobs fantasiosos, ou que sustentam alguma
mecânica sem substituto, ficam.

| Mob | Decisão | Motivo |
|---|---|---|
| Guardião / Guardião Ancião | **Fica** | Peixe fantasioso, e a única fonte de prismarinho e esponja |
| Endermite | **Fica** | Sem contraparte Pokémon, e as farms de enderman dependem dele |
| Devastador (ravager) | **Fica, por enquanto** | Só sai se um Pokémon casar com os saqueadores (ver seção 8.1) |
| Phantom | **Fica** | Morto-vivo fantasioso |
| Cavalo esqueleto / zumbi, Zoglin | **Ficam** | Mortos-vivos |
| Slime / Magma Cube | **Opção na configuração** (`slimes`, desligada por padrão) | Monstros; Grimer, Goomy e Solosis dão slimeball; Slugma, Magcargo, Numel, Camerupt e Heatran dão creme de magma |
| Traça (silverfish) | **(decidir)** | Tem contraparte: Pokémon inseto que vivem na pedra (ver abaixo) |

- [ ] Opção `slimes` na configuração (slime e magma cube juntos)
- [ ] Traça → **Dwebble** nos blocos infestados (decidido: Nincada é raro demais para a quantidade de traças)
  - Dwebble é um inseto que mora dentro de uma pedra, combina com "bloco infestado"
  - Funcionaria assim: quebrar um bloco infestado faz sair um Nincada selvagem e bravo, e os outros
    escondidos nos blocos por perto também saem (como as traças chamam as outras)
- [ ] Spawners de aranha das minas abandonadas: trocar por spawn de Spinarak e Joltik (hoje ficam vazios)
- [ ] Jóquei de aranha: o esqueleto deveria montar num Ariados?

---

## 3. Devolver mecânicas que os animais faziam

### Abelhas e colmeias ⭐
- [x] Pokémon abelha moram nas colmeias e ninhos naturais (cada abelha guardada vira um morador, até 3)
- [x] **Uma colônia por colmeia**, como os cercados: Combee (com no máximo uma rainha Vespiquen) ou Cutiefly (com Ribombee)
- [x] Visitam flores, polinizam uma plantação perto e enchem a colmeia de mel
- [x] Com a rainha Vespiquen, a colmeia às vezes enche dois níveis de uma vez
- [x] **Entram na colmeia** à noite, na chuva e para descansar depois de levar mel (por isso não dormem do lado de fora)
- [x] **Quebrar ou explodir a colmeia solta** quem estiver dentro
- [x] **Ganhar e perder a colmeia**: Pokémon abelha selvagens sem casa adotam uma colmeia da mesma colônia com vaga
      (até 16 blocos); perdem a casa se ela for quebrada; morador sumido (capturado, derrotado) libera a vaga
- [ ] Bug corrigido a testar: abelhas do vanilla tentando sair sem parar (som infinito)
- [ ] Mexer na colmeia sem fogueira embaixo deixa os moradores bravos (com o revide da seção 6)
- [ ] **No pasto**: Pokémon abelha do jogador usam colmeias craftadas perto do pasto
- [ ] Toque Suave: levar a colmeia com os Pokémon dentro (hoje quebrar sempre solta todos)
- [ ] **(decidir)** Beedrill fora das colmeias (minha sugestão): é vespa, não faz mel
  - Kakuna penduradas nas árvores de florestas, e Beedrill defendendo a área (seção 6, território)
  - Ideia: Beedrill como predador que de vez em quando ataca colmeias de Combee, e a Vespiquen defende

### Gatos
- [x] Creepers fogem de 29 Pokémon gato (Meowth, Persian, Skitty, Litten, Shinx...) — **já existe no Cobblemon**
- [x] Phantoms evitam 33 Pokémon gato — **já existe no Cobblemon**
- [ ] Presente da manhã: o Pokémon gato do jogador traz um item ao acordar (Meowth traz pepita de ouro ou moeda)

### Cães e lobos
- [x] Esqueletos fogem de 27 Pokémon cachorro — **já existe no Cobblemon**
- [x] Pokémon cachorro (do jogador e selvagens) caçam esqueletos
- [x] Cachorros selvagens às vezes caçam Pokémon ovelha selvagens (nunca os de jogador, vila ou colmeia)

### Galinhas
- [ ] **Combusken** bota ovos de tempos em tempos (pasto e currais de vila)
  - Torchic não: é um pintinho
  - Chansey não: o ovo dela não é de galinha
  - Minecraft 1.21.1 só tem o ovo comum (os ovos azul e marrom de clima chegaram no 1.21.5)

### Sniffer
- [ ] Swinub, Piloswine e Lechonk farejam e desenterram sementes antigas (torchflower, pitcher) e cogumelos

### Sapos e anfibrilho (froglight)
O Cobblemon já dá anfibrilho ao derrotar Froakie, Frogadier, Greninja, Politoed, Tadbulb e Bellibolt.
Sapos: linha do Froakie, Poliwag (o girino!), Politoed, Croagunk, Toxicroak, linha do Tympole, Tadbulb e Bellibolt.

- [ ] **Na batalha** (como o sapo do vanilla, que mata o magma cube): Slugma derrotado por um Pokémon sapo
      solta anfibrilho com **100% de chance**
- [ ] **No pasto** (sem violência): o sapo lambe o Slugma (golpe Lick) e cospe um anfibrilho, mais devagar.
      O Slugma não se machuca e entra em tempo de espera
- [ ] Cor do anfibrilho pelo bioma, como no vanilla: temperado = ocre, frio = verdejante, quente = perolado
- [ ] Também vale com magma cube pequeno, se os slimes estiverem ligados na configuração

### Cabras
- [ ] Skiddo e Gogoat dão chifre de cabra ao dar cabeçada em pedra ou madeira

### Golfinhos
- [ ] Lapras, Wailmer, Dewgong e Primarina dão "graça do golfinho" a quem nada perto
- [ ] Wailmer e Lapras levam o jogador até naufrágios e tesouros enterrados (os golfinhos faziam isso)

### Axolotes
- [ ] Wooper e Mudkip ajudam o jogador a lutar contra afogados embaixo d'água

### Papagaios
- [ ] Chatot imita sons de mobs hostis por perto (como papagaios avisam)
- [ ] Oricorio e Chatot dançam perto de uma jukebox tocando

### Tartarugas
- [ ] Pokémon tartaruga voltam à praia onde nasceram (Squirtle, Tirtouga, Chewtle)

### Raposas
- [ ] Nickit e Vulpix roubam comida ou itens do chão e fogem; dormem de dia

### Pandas
- [ ] Pancham e Pangoro comem bambu e espirram (podem soltar slimeball, como o panda filhote)

---

## 4. Fazendas no pasto (Pasture Block do Cobblemon)

Regra geral: produção lenta, com tempo de espera. Talvez exija o Pokémon feliz ou alimentado, para não ficar forte demais.

- [ ] **Aranhas** (Spinarak, Ariados, Joltik, Galvantula): tecem teias (cobweb) nos cantos e soltam linha
- [ ] **Tipo Fogo** (linha do Charmander, Fennekin, Houndour): soltam pó de blaze
- [ ] **Gelatinosos** (Solosis, Grimer, Goomy): soltam slimeball
- [ ] **Abelhas**: Pokémon abelha do jogador no pasto usam colmeias craftadas perto dele (mel e favo)
- [ ] **Combusken**: ovos
- [ ] **Magcargo e Slugma**: transformam pedra em volta em bloco de magma
- [ ] **Snorunt e Vanillite**: neve e gelo em volta
- [ ] **Shuckle**: fermenta frutas guardadas e faz Suco de Fruta (como no jogo original)
- [ ] **Clamperl e Shellder**: pérolas, cristais de prismarinho
- [ ] **Tropius**: fruta no pescoço que dá para colher (maçã ou fruta Pokémon)
- [ ] **Gastly e Haunter**: membrana de phantom de vez em quando à noite
- [ ] **Sableye e Carbink**: acham minérios pequenos (cuidado com balanceamento)
- [ ] **Exeggcute**: ovos

---

## 5. Flora — comportamentos dos Pokémon planta

- [ ] Tipo Planta aceleram plantações em volta (Bulbasaur, Oddish, Bellsprout, Chikorita, Sunkern)
- [ ] Seedot e Sunkern plantam mudas e sementes perto de onde andam
- [ ] **Fotossíntese**: Pokémon planta se curam devagar sob o sol e ficam mais lentos à noite
- [ ] Sunflora vira o rosto para o sol durante o dia
- [ ] Bellossom dança quando faz sol
- [ ] Cherrim muda de forma com o tempo (sol ou nublado)
- [ ] Sudowoodo finge ser árvore: congela quando o jogador olha e foge da água
- [ ] Shroomish, Breloom, Paras e Parasect espalham cogumelos e micélio
- [ ] Foongus e Amoonguss se disfarçam de Pokébola no chão
- [ ] Lotad cria vitórias-régias na água
- [ ] Sewaddle e Swadloon comem folhas de árvores
- [ ] Phantump e Trevenant aparecem perto de árvores mortas ou florestas escuras
- [ ] Pokémon planta evitam fogo e Pokémon tipo Fogo
- [ ] Snover e Abomasnow deixam neve por onde passam em biomas frios

---

## 6. Combate e vida selvagem

O Cobblemon já marca 564 espécies como "se defendem" (`willDefendSelf`), mas só 3 têm a briga corpo a corpo
(`fightsMelee`) ligada. O mod já sabe ligar a briga por código numa entidade (`PokemonAi.addBehaviours`),
como nos cachorros e nos Golurk.

- [ ] **Revide**: Pokémon selvagens que apanham do jogador revidam
- [ ] **Temperamento por espécie**: agressivos (Tauros, Gyarados, Beedrill, Mankey), medrosos (Pidgey, Rattata, Wooloo) e neutros
- [ ] Natureza influencia o temperamento (Adamant e Brave mais bravos; Timid foge)
- [ ] Bando defende o filhote: atacar um bebê deixa os adultos do bando bravos
- [ ] **Predador e presa**: Persian caça Rattata, Pidgeot caça Magikarp; presas fogem de predadores
- [ ] **Território**: Beedrill e Kakuna defendem área perto das árvores onde ficam
- [ ] Pokémon selvagens atacam mobs hostis que chegam perto (já existe comportamento para Pokémon do jogador)

---

## 7. Rotina e ambiente

- [ ] **Dia e noite**: noturnos (Hoothoot, Zubat, Gastly, Murkrow) dormem de dia e saem à noite
- [ ] Zubat e Woobat dormem de cabeça para baixo no teto das cavernas
- [ ] **Chuva**: tipo Fogo procuram abrigo; tipo Água e Lotad ficam felizes na chuva
- [ ] **Tempestade**: tipo Elétrico são atraídos por para-raios e ficam agitados com raios
- [ ] Pokémon vão beber água de vez em quando
- [ ] Pokémon selvagens comem frutas das árvores de fruta do Cobblemon
- [ ] **Atrair com comida**: selvagens seguem quem segura a comida favorita (como vacas seguem trigo)
- [ ] Bebês seguem os adultos da mesma linha evolutiva
- [ ] Bandos de Wingull e Pelipper voando sobre o mar
- [ ] Magikarp pulando na água e subindo cachoeiras
- [ ] Murkrow e Meowth pegam itens brilhantes (ouro, diamante) do chão
- [ ] Sableye comem minérios expostos nas cavernas
- [ ] Rattata e Bunnelby comem plantações (praga). Gatos espantam eles
- [ ] Snorlax dorme no meio do caminho e come plantações
- [ ] Ditto se transforma no Pokémon mais próximo

---

## 8. Habitantes do Minecraft com Pokémon

O tema do mod: os Pokémon convivem com o mundo e com os habitantes do Minecraft. O mercador ter Pokémon
é lógico, então aldeões e saqueadores também podem ter.

### Guardiões das vilas
- [x] **Golett e Golurk** no lugar do golem de ferro (40% Golurk nível 43–50, 60% Golett nível 20–30)
- [x] Golem de ferro não é mais montado com abóbora
- [ ] Craft do Golett (ex.: abóbora sobre blocos de argila ou terra, como o golem de barro da lenda)
- [ ] Golurk "cansado" volta para o centro da vila (sino) em vez do ponto onde nasceu
- [ ] Growlithe guardiões ajudam o golem de ferro (como os cães policiais do anime)
- [ ] Pokémon da vila ficam calmos perto de aldeões e fogem de zumbis
- [ ] Aldeões fazendeiros "ordenham" Miltank e recolhem ovos dos currais

### Aldeões com Pokémon
Só aldeões **nascidos com a vila** (20% deles); aldeões de reprodução não adotam, por enquanto. O aldeão espera
arrumar profissão (até 2 dias do jogo) para o Pokémon combinar com ela. Honedge é mais raro (35% dos ferreiros
de armas; os outros ganham um "qualquer um").

Teste rápido no jogo:
`/summon minecraft:villager ~ ~ ~ {NeoForgeData:{"living_fauna_flora:adopt":1b,"living_fauna_flora:adopt_species":"pichu"}}`

| Profissão | Pokémon | Por quê |
|---|---|---|
| Armeiro (armorer) | Aron, Lairon | Comem ferro e têm couraça de aço |
| Ferreiro de armas (weaponsmith) | Honedge | É uma espada |
| Ferreiro de ferramentas (toolsmith) | Klefki, Roggenrola | Chaves e minério |
| Pedreiro (mason) | Timburr, Gurdurr | Carregam vigas e trabalham em obras |
| Flecheiro (fletcher) | Rowlet | As penas da evolução final viram flechas |
| Fazendeiro | Bulbasaur, Oddish, Skiddo | Ajudam nas plantações |
| Pescador | Wingull | Vive no mar |
| Pastor | Wooloo, Mareep | Lã |
| Clérigo | Chansey, Audino | Curandeiros |
| Bibliotecário | Abra, Natu | Psíquicos e estudiosos |
| Cartógrafo | Hoothoot, Pidgey | Navegação (os pequenos, para caberem na cabeça) |
| Curtumeiro (leatherworker) | Smeargle | Pinta e tinge |
| Açougueiro | Farfetch'd | Carrega um alho-poró (cozinha) |
| Desempregado ou bobo (nitwit) | Slowpoke, Psyduck | Sem pressa nenhuma |
| Criança aldeã | Pichu, Togepi | Brincam juntos |
| Qualquer um (20%) | Pidgey, Pichu | Só acompanham o aldeão |

- [x] Pokémon do aldeão segue ele e volta para perto se se afastar
- [x] Pokémon pequenos (Pidgey, Pichu, Natu, Rowlet, Togepi) **ficam em cima da cabeça** do aldeão (testar no jogo)
  - O "ombro" do Cobblemon é só para jogadores. Para aldeões daria para usar o sistema de **passageiro**
    do Minecraft (o Pokémon "monta" no aldeão), que é o mesmo das galinhas jóquei. Precisa testar a
    posição e como fica a IA do aldeão
- [ ] Pokémon do aldeão ajudam na profissão (Timburr carrega blocos, Bulbasaur acelera plantações)
- [ ] O jogador pode batalhar com o aldeão, como com o mercador
- [ ] Aldeões de reprodução também adotarem (mais tarde)

### 8.1 Saqueadores (illagers) com Pokémon
- [ ] Saqueadores com Houndour, Houndoom ou Mightyena (cães sombrios)
- [ ] Vindicadores com Pawniard ou Bisharp (lâminas, combinam com o machado)
- [ ] Bruxas com Murkrow, Honchkrow ou Mismagius
- [ ] Ilusionista com Zorua ou Zoroark (Pokémon da ilusão)
- [ ] Evocador com Shuppet ou Gastly
- [ ] **(decidir)** Devastador → **Rhyhorn / Rhydon**: rinoceronte de pedra, parecido com o devastador, que
      poderia ser montado pelos saqueadores nas invasões. Se casar bem, aí sim o devastador sai
- [ ] Postos de saqueadores com Pokémon presos em jaulas (como os golens de ferro e allays presos de hoje)

---

## 9. Mercador ambulante

- [ ] Variar a montaria por bioma (Camerupt no deserto, Mamoswine na neve, Rapidash nas planícies)
- [ ] Manta nas outras montarias, se houver variação
- [ ] Trocas com itens do Cobblemon (frutas, apricorns, Pokébolas)
- [ ] Pokémon do mercador com nível que acompanha o progresso do jogador

---

## 10. Ideia futura: couro sintético (analisada, não implementar agora)

**Lógica**: couro é pele, então só deveria sair derrotando um Pokémon (como já acontece no Cobblemon).
Uma alternativa sem violência precisaria de um material *diferente*, e não de couro de verdade.

**Melhor candidato: Trubbish e Garbodor.** Eles são feitos de lixo e sacos plásticos, então "reciclar lixo
em material sintético" combina com a espécie.

- No pasto, Garbodor "processa" carne podre (que zumbis dão em quantidade) e solta **Couro Sintético**
- O couro sintético seria um **item novo**, não o couro do Minecraft. Daria para usar nas receitas que
  pedem couro (livro, moldura, armadura de couro) por meio de receitas alternativas ou de uma tag de itens
- Vantagens: dá utilidade à carne podre, não "mata" ninguém e tem justificativa no próprio Pokémon
- Cuidados: decidir se a armadura de couro sintético é igual à normal; balancear o tempo de produção;
  dar uma textura própria ao item

Outras opções avaliadas:
- **Grimer e Muk** (lodo vira polímero): faz sentido, mas eles já são a fonte de slimeball
- **Receita na panela de fogueira** (linha + slimeball + cera de mel): não precisa de Pokémon e é menos interessante
- **Couro de coelho**: o Minecraft já transforma 4 peles de coelho em couro, mas essas peles também saem derrotando Pokémon
