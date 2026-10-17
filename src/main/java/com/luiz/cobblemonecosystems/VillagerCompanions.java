package com.luiz.cobblemonecosystems;

import com.luiz.cobblemonecosystems.ConfigLists.Family;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Alguns aldeões nascidos com a vila (não os de reprodução) adotam um Pokémon que combina com a profissão.
 * - Pokémon pequenos andam em cima da cabeça do aldeão; os outros seguem ele e são teleportados se ficarem longe.
 * - Cada aldeão tem uma personalidade: os mais desafiadores têm Pokémon mais fortes e um time maior (só um aparece).
 * - De vez em quando o aldeão faz carinho no Pokémon.
 * - O Pokémon é protegido: jogadores não machucam ele (para batalhar, desafie o aldeão: {@link VillagerBattles}).
 * - O Pokémon defende o aldeão de monstros. Derrotado, vai para a Pokébola e descansa; não morre.
 * - Aldeão que vira zumbi guarda o Pokémon; curado, solta ele de novo.
 * - Se o Pokémon sumir de outro jeito (/kill), o aldeão adota outro depois de alguns dias.
 *
 * Para testar: /summon minecraft:villager ~ ~ ~ {NeoForgeData:{"cobblemon_ecosystems:adopt":1b,"cobblemon_ecosystems:adopt_species":"pichu"}}
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class VillagerCompanions {
    /** Chance de o Pokémon ser um "qualquer um" (segue o aldeão sem ligação com a profissão). */
    private static final float GENERIC_CHANCE = 0.2f;
    // Os tempos usam o horário do dia (getDayTime): dormir e o /time contam, como o jogador espera de "dias".
    /** Sem profissão até este tempo (2 dias do jogo), adota mesmo assim. */
    private static final long MAX_WAIT_FOR_JOB = 48000;
    /** Longe assim do aldeão, o Pokémon é teleportado para perto dele. */
    private static final double TELEPORT_DISTANCE = 16;
    /** Pokémon sumido (e não guardado na Pokébola) por esse tempo (3 dias): o aldeão adota outro. */
    private static final long READOPT_AFTER = 72000;
    /** Monstros a essa distância do aldeão são atacados pelo pet. */
    private static final double DEFEND_RANGE = 10;
    /** Chance, a cada 2 segundos com os dois perto, de o aldeão fazer carinho no Pokémon. */
    private static final float PET_CHANCE = 1f / 12;

    /** Na lista villager_partners: "baby" é para criança, "any" é o grupo de qualquer aldeão e "*" sorteia do "any". */
    private static final String BABY = "baby", ANY = "any", FROM_ANY = "*";

    /** As opções da configuração para essa chave (profissão, "baby" ou "any"). */
    private static List<Family> partners(String key) {
        return ConfigLists.keyed(Config.VILLAGER_PARTNERS).getOrDefault(key, List.of());
    }

    private static List<Family> partners(VillagerProfession profession) {
        List<Family> options = partners(profession.name());
        return options.isEmpty() ? partners(ANY) : options;
    }

    static boolean ridesOnHead(String species) {
        return ConfigLists.species(Config.HEAD_RIDERS).contains(species);
    }

    /** Quão desafiador o aldeão é: decide se aceita batalhas, a força e o tamanho do time. */
    enum Personality {
        PEACEFUL(5, 15, 0, 0), FRIENDLY(10, 20, 0, 2), COMPETITIVE(20, 35, 1, 3), CHALLENGER(35, 50, 2, 4);

        final int minLevel, maxLevel, extraPokemon, battleSkill;

        Personality(int minLevel, int maxLevel, int extraPokemon, int battleSkill) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.extraPokemon = extraPokemon;
            this.battleSkill = battleSkill;
        }

        boolean battles() {
            return this != PEACEFUL;
        }

        int rollLevel(RandomSource random) {
            return minLevel + random.nextInt(maxLevel - minLevel + 1);
        }
    }

    /** Pesos de PEACEFUL, FRIENDLY, COMPETITIVE e CHALLENGER: quem trabalha com armas e obras gosta mais de batalhar. */
    private static int[] personalityWeights(VillagerProfession profession) {
        if (profession == VillagerProfession.WEAPONSMITH || profession == VillagerProfession.ARMORER
                || profession == VillagerProfession.FLETCHER || profession == VillagerProfession.MASON
                || profession == VillagerProfession.TOOLSMITH) return new int[] {10, 30, 40, 20};
        if (profession == VillagerProfession.CLERIC || profession == VillagerProfession.LIBRARIAN
                || profession == VillagerProfession.CARTOGRAPHER) return new int[] {70, 25, 5, 0};
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) return new int[] {40, 40, 15, 5};
        return new int[] {30, 40, 25, 5}; // fazendeiro, pastor, pescador, açougueiro, curtumeiro
    }

    static final String ADOPT_KEY = CobblemonEcosystems.MOD_ID + ":adopt";
    private static final String ADOPT_SPECIES_KEY = CobblemonEcosystems.MOD_ID + ":adopt_species";
    private static final String ADOPT_SINCE_KEY = CobblemonEcosystems.MOD_ID + ":adopt_since";
    /** No aldeão: o Pokémon que aparece, a personalidade, o resto do time e quando viu o Pokémon pela última vez. */
    static final String COMPANION_KEY = CobblemonEcosystems.MOD_ID + ":companion";
    static final String PERSONALITY_KEY = CobblemonEcosystems.MOD_ID + ":personality";
    static final String TEAM_KEY = CobblemonEcosystems.MOD_ID + ":team";
    private static final String PET_SEEN_KEY = CobblemonEcosystems.MOD_ID + ":pet_seen";
    /** No aldeão: o Pokémon guardado na Pokébola (descansando depois de uma batalha). */
    static final String STORED_PET_KEY = CobblemonEcosystems.MOD_ID + ":stored_pet";
    /** No Pokémon: o aldeão dono dele. */
    static final String VILLAGER_KEY = CobblemonEcosystems.MOD_ID + ":villager";

    private static final Set<Villager> waiting = new HashSet<>();
    private static final Set<Villager> owners = new HashSet<>();
    private static final Set<PokemonEntity> companions = new HashSet<>();
    /** Aldeões curados (eram zumbis) que soltam o pet no próximo tick. */
    private static final List<Villager> toRelease = new ArrayList<>();
    private static int tick;

    /** Só aldeões criados junto com a vila podem adotar. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() == EntityType.VILLAGER && event.getSpawnType() == MobSpawnType.STRUCTURE
                && Config.VILLAGER_POKEMON.get() && event.getLevel().getRandom().nextFloat() < Config.VILLAGER_ADOPT_CHANCE.get()) {
            event.getEntity().getPersistentData().putBoolean(ADOPT_KEY, true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (event.getEntity() instanceof Villager villager) {
            if (villager.getPersistentData().hasUUID(COMPANION_KEY)) owners.add(villager);
            else if (villager.getPersistentData().getBoolean(ADOPT_KEY)) waiting.add(villager);
        } else if (event.getEntity() instanceof PokemonEntity pokemon && pokemon.getPersistentData().hasUUID(VILLAGER_KEY)) {
            PokemonAi.markAsVillagePokemon(pokemon); // também corrige nomes antigos
            companions.add(pokemon);
        }
    }

    /**
     * Pokémon de aldeão não sufocam (quem está na cabeça entra no bloco quando o aldeão passa por lugares baixos)
     * e não levam dano de jogadores: para batalhar com eles, desafie o aldeão.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof PokemonEntity pokemon && pokemon.getPersistentData().hasUUID(VILLAGER_KEY)
                && (event.getSource().is(DamageTypes.IN_WALL) || event.getSource().getEntity() instanceof Player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Villager villager : List.copyOf(toRelease)) releasePet(villager);
        toRelease.clear();
        if (++tick % 40 != 0) return;

        waiting.removeIf(villager -> {
            if (villager.isRemoved() || villager.getPersistentData().hasUUID(COMPANION_KEY)) return true;
            ServerLevel level = (ServerLevel) villager.level();
            if (!villager.getPersistentData().contains(ADOPT_SINCE_KEY)
                    || villager.getPersistentData().getLong(ADOPT_SINCE_KEY) > level.getDayTime()) {
                villager.getPersistentData().putLong(ADOPT_SINCE_KEY, level.getDayTime());
            }
            // Espera o aldeão arrumar emprego, para o Pokémon combinar com a profissão.
            boolean forced = villager.getPersistentData().contains(ADOPT_SPECIES_KEY);
            boolean hasJob = villager.getVillagerData().getProfession() != VillagerProfession.NONE;
            boolean waitedEnough = level.getDayTime() - villager.getPersistentData().getLong(ADOPT_SINCE_KEY) > MAX_WAIT_FOR_JOB;
            if (!forced && !hasJob && !villager.isBaby() && !waitedEnough) return false;
            adopt(villager, level);
            return true;
        });

        companions.removeIf(pokemon -> {
            if (pokemon.isRemoved()) return true;
            ServerLevel level = (ServerLevel) pokemon.level();
            Entity entity = level.getEntity(pokemon.getPersistentData().getUUID(VILLAGER_KEY));
            // Aldeão morreu ou virou zumbi: o Pokémon vai embora junto.
            if (!(entity instanceof Villager villager) || !villager.isAlive()) {
                pokemon.discard();
                return true;
            }
            villager.getPersistentData().putLong(PET_SEEN_KEY, level.getDayTime());
            if (pokemon.isBusy() || Hunts.isHunting(pokemon)) return false;
            if (defend(villager, pokemon)) return false;
            // Quem anda na cabeça desceu para lutar: sobe de novo quando estiver perto.
            if (!pokemon.isPassenger() && ridesOnHead(PokemonAi.species(pokemon)) && pokemon.distanceTo(villager) < 3) {
                pokemon.startRiding(villager, true);
            }
            if (!pokemon.isPassenger()) {
                double distance = pokemon.distanceTo(villager);
                if (distance > TELEPORT_DISTANCE) {
                    pokemon.teleportTo(villager.getX(), villager.getY(), villager.getZ());
                } else if (distance > 5) {
                    PokemonAi.walkTo(pokemon, villager, 0.5f, 3);
                }
            }
            maybePet(level, villager, pokemon);
            return false;
        });

        // Pokémon sumido há dias (e não guardado descansando): o aldeão adota outro.
        owners.removeIf(villager -> {
            if (villager.isRemoved()) return true;
            CompoundTag data = villager.getPersistentData();
            if (!data.hasUUID(COMPANION_KEY)) return true;
            long now = villager.level().getDayTime();
            if (!data.contains(PET_SEEN_KEY) || data.getLong(PET_SEEN_KEY) > now) data.putLong(PET_SEEN_KEY, now);
            if (data.contains(STORED_PET_KEY) || now - data.getLong(PET_SEEN_KEY) < READOPT_AFTER) return false;
            for (String key : List.of(COMPANION_KEY, PET_SEEN_KEY, TEAM_KEY, ADOPT_SINCE_KEY)) data.remove(key);
            data.putBoolean(ADOPT_KEY, true);
            waiting.add(villager);
            return true;
        });
    }

    /** O Pokémon ataca monstros (menos creepers) que miram o aldeão ou chegam perto dele. */
    private static boolean defend(Villager villager, PokemonEntity pokemon) {
        Mob enemy = villager.level().getEntitiesOfClass(Mob.class, villager.getBoundingBox().inflate(DEFEND_RANGE),
                        mob -> mob instanceof Enemy && !(mob instanceof Creeper) && mob.isAlive()
                                && (mob.getTarget() == villager || mob.distanceTo(villager) < 6))
                .stream().min(Comparator.comparingDouble(villager::distanceToSqr)).orElse(null);
        if (enemy == null) return false;
        pokemon.stopRiding();
        Hunts.start(pokemon, enemy, 3f + pokemon.getPokemon().getLevel() / 5f, 0.2, 20);
        return true;
    }

    /**
     * Pet derrotado não morre: vai para a Pokébola e descansa, como depois de uma batalha (o aldeão pode levar
     * até a máquina de cura). Só o /kill mata de verdade (útil para testar a adoção de outro pet).
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity pokemon) || !pokemon.getPersistentData().hasUUID(VILLAGER_KEY)
                || event.getSource().is(DamageTypes.GENERIC_KILL) || !(pokemon.level() instanceof ServerLevel level)) return;
        if (!(level.getEntity(pokemon.getPersistentData().getUUID(VILLAGER_KEY)) instanceof Villager villager)) return;
        event.setCanceled(true);
        pokemon.setHealth(1f);
        storePet(villager, pokemon);
        VillagerBattles.startRest(villager);
    }

    /**
     * Aldeão que vira zumbi guarda o pet na Pokébola (fica no zumbi); curado, o aldeão solta o pet na hora.
     * Nossos dados vão junto para a entidade nova.
     */
    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel)) return;
        if (event.getEntity() instanceof Villager villager) {
            PokemonEntity pet = companionOf(villager);
            if (pet != null) storePet(villager, pet);
        }
        CompoundTag from = event.getEntity().getPersistentData();
        CompoundTag to = event.getOutcome().getPersistentData();
        for (String key : from.getAllKeys()) {
            if (key.startsWith(CobblemonEcosystems.MOD_ID + ":")) to.put(key, from.get(key).copy());
        }
        if (event.getOutcome() instanceof Villager cured && to.contains(STORED_PET_KEY)) toRelease.add(cured);
    }

    /** O aldeão para, olha para o Pokémon e faz carinho: corações sobem do Pokémon. */
    private static void maybePet(ServerLevel level, Villager villager, PokemonEntity pokemon) {
        if (villager.isSleeping() || villager.isTrading() || pokemon.distanceTo(villager) > 5
                || level.random.nextFloat() >= PET_CHANCE) return;
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(pokemon, true));
        pokemon.getLookControl().setLookAt(villager);
        level.sendParticles(ParticleTypes.HEART, pokemon.getX(), pokemon.getY() + pokemon.getBbHeight() + 0.3, pokemon.getZ(),
                3, 0.3, 0.2, 0.3, 0);
        level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.5f, 1.1f);
    }

    private static void adopt(Villager villager, ServerLevel level) {
        RandomSource random = level.random;
        Personality personality = personalityOf(villager, random);
        String species = chooseSpecies(villager, random);
        if (species == null) return;
        PokemonEntity pokemon = PokemonProperties.Companion
                .parse(species + " level=" + personality.rollLevel(random) + " uncatchable")
                .createEntity(level);
        pokemon.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), 0f);
        pokemon.getPersistentData().putUUID(VILLAGER_KEY, villager.getUUID());
        KeptPokemon.keep(pokemon);
        PokemonAi.markAsVillagePokemon(pokemon);
        if (!level.addFreshEntity(pokemon)) return;

        villager.getPersistentData().putUUID(COMPANION_KEY, pokemon.getUUID());
        NameTags.onAdopted(villager, pokemon);
        villager.getPersistentData().putLong(PET_SEEN_KEY, level.getDayTime());
        rollTeam(villager, personality, species, random);
        companions.add(pokemon);
        owners.add(villager);
        if (ridesOnHead(species)) pokemon.startRiding(villager, true);
    }

    /** A personalidade do aldeão; sorteada pela profissão na primeira vez (inclusive para pets de versões antigas). */
    static Personality personalityOf(Villager villager, RandomSource random) {
        CompoundTag data = villager.getPersistentData();
        if (data.contains(PERSONALITY_KEY)) {
            try {
                return Personality.valueOf(data.getString(PERSONALITY_KEY));
            } catch (IllegalArgumentException ignored) {
                // valor inválido: sorteia de novo
            }
        }
        Personality personality = Personality.PEACEFUL;
        if (!villager.isBaby()) {
            int[] weights = personalityWeights(villager.getVillagerData().getProfession());
            int roll = random.nextInt(weights[0] + weights[1] + weights[2] + weights[3]);
            for (Personality p : Personality.values()) {
                roll -= weights[p.ordinal()];
                if (roll < 0) {
                    personality = p;
                    break;
                }
            }
        }
        data.putString(PERSONALITY_KEY, personality.name());
        return personality;
    }

    /** O resto do time (fica na Pokébola, só aparece na batalha), salvo como propriedades do Cobblemon. */
    static void rollTeam(Villager villager, Personality personality, String shownSpecies, RandomSource random) {
        List<Family> pool = new ArrayList<>(partners(villager.getVillagerData().getProfession()));
        pool.addAll(partners(ANY));
        pool.removeIf(family -> family.base().equals(FROM_ANY));
        if (pool.size() > 1) pool.removeIf(family -> family.base().equals(shownSpecies));
        ListTag team = new ListTag();
        for (int i = 0; i < personality.extraPokemon && !pool.isEmpty(); i++) {
            team.add(StringTag.valueOf(ConfigLists.pick(pool, random).base() + " level=" + personality.rollLevel(random)));
        }
        villager.getPersistentData().put(TEAM_KEY, team);
    }

    static List<String> teamOf(Villager villager) {
        List<String> team = new ArrayList<>();
        for (Tag entry : villager.getPersistentData().getList(TEAM_KEY, Tag.TAG_STRING)) team.add(entry.getAsString());
        return team;
    }

    /** O Pokémon que segue o aldeão, se estiver carregado no mundo. */
    static PokemonEntity companionOf(Villager villager) {
        if (!villager.getPersistentData().hasUUID(COMPANION_KEY)) return null;
        Entity entity = ((ServerLevel) villager.level()).getEntity(villager.getPersistentData().getUUID(COMPANION_KEY));
        return entity instanceof PokemonEntity pokemon && entity.isAlive() ? pokemon : null;
    }

    /** Guarda o Pokémon "na Pokébola": a entidade fica salva no aldeão e sai do mundo. */
    static void storePet(Villager villager, PokemonEntity pokemon) {
        CompoundTag entityData = new CompoundTag();
        pokemon.stopRiding();
        if (!pokemon.save(entityData)) return;
        villager.getPersistentData().put(STORED_PET_KEY, entityData);
        companions.remove(pokemon);
        pokemon.discard();
    }

    /** Solta o Pokémon guardado ao lado do aldeão (ou na cabeça, se for pequeno). */
    static void releasePet(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(STORED_PET_KEY)) return;
        ServerLevel level = (ServerLevel) villager.level();
        CompoundTag entityData = data.getCompound(STORED_PET_KEY);
        data.remove(STORED_PET_KEY);
        data.putLong(PET_SEEN_KEY, level.getDayTime());
        EntityType.create(entityData, level).ifPresent(entity -> {
            entity.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), 0f);
            entity.getPersistentData().putUUID(VILLAGER_KEY, villager.getUUID());
            data.putUUID(COMPANION_KEY, entity.getUUID());
            if (entity instanceof PokemonEntity healed) {
                healed.getPokemon().heal();
                healed.setHealth(healed.getMaxHealth());
            }
            if (!level.addFreshEntity(entity) || !(entity instanceof PokemonEntity pokemon)) return;
            level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.6f, 1.4f);
            if (ridesOnHead(PokemonAi.species(pokemon))) pokemon.startRiding(villager, true);
        });
    }

    /** A espécie do pet, ou null se a configuração não tiver nenhuma opção para esse aldeão. */
    private static String chooseSpecies(Villager villager, RandomSource random) {
        String forced = villager.getPersistentData().getString(ADOPT_SPECIES_KEY);
        if (!forced.isEmpty()) return forced;
        if (villager.isBaby()) return pickSpecies(partners(BABY), random);
        if (random.nextFloat() < GENERIC_CHANCE) return pickSpecies(partners(ANY), random);
        String species = pickSpecies(partners(villager.getVillagerData().getProfession()), random);
        return FROM_ANY.equals(species) ? pickSpecies(partners(ANY), random) : species;
    }

    private static String pickSpecies(List<Family> options, RandomSource random) {
        Family family = ConfigLists.pick(options, random);
        return family == null ? null : family.base();
    }
}
