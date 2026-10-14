package com.luiz.livingfaunaflora;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Alguns aldeões nascidos com a vila (não os de reprodução) adotam um Pokémon que combina com a profissão.
 * Pokémon pequenos andam em cima da cabeça do aldeão; os outros seguem ele por aí.
 *
 * Para testar: /summon minecraft:villager ~ ~ ~ {NeoForgeData:{"living_fauna_flora:adopt":1b,"living_fauna_flora:adopt_species":"pichu"}}
 */
@EventBusSubscriber(modid = LivingFaunaFlora.MOD_ID)
public class VillagerCompanions {
    /** Chance de um aldeão da vila ter um Pokémon. */
    private static final float ADOPT_CHANCE = 0.5f;
    /** Chance de o Pokémon ser um "qualquer um" (segue o aldeão sem ligação com a profissão). */
    private static final float GENERIC_CHANCE = 0.2f;
    /** Honedge é o mais diferente: o ferreiro de armas só fica com ele nessa fração das vezes. */
    private static final float HONEDGE_CHANCE = 0.35f;
    /** Sem profissão até este tempo (2 dias do jogo), adota mesmo assim. */
    private static final long MAX_WAIT_FOR_JOB = 48000;

    private static final List<String> GENERIC = List.of("pidgey", "pichu");
    private static final List<String> BABY = List.of("pichu", "togepi");
    private static final List<String> NO_JOB = List.of("slowpoke", "psyduck");
    private static final Map<VillagerProfession, List<String>> BY_PROFESSION = Map.ofEntries(
            Map.entry(VillagerProfession.ARMORER, List.of("aron", "aron", "lairon")),
            Map.entry(VillagerProfession.WEAPONSMITH, List.of("honedge")),
            Map.entry(VillagerProfession.TOOLSMITH, List.of("klefki", "roggenrola")),
            Map.entry(VillagerProfession.MASON, List.of("timburr")),
            Map.entry(VillagerProfession.FLETCHER, List.of("rowlet")),
            Map.entry(VillagerProfession.FARMER, List.of("bulbasaur", "oddish", "skiddo")),
            Map.entry(VillagerProfession.FISHERMAN, List.of("wingull")),
            Map.entry(VillagerProfession.SHEPHERD, List.of("wooloo", "mareep")),
            Map.entry(VillagerProfession.CLERIC, List.of("chansey", "audino")),
            Map.entry(VillagerProfession.LIBRARIAN, List.of("abra", "natu")),
            Map.entry(VillagerProfession.CARTOGRAPHER, List.of("hoothoot", "pidgey")),
            Map.entry(VillagerProfession.LEATHERWORKER, List.of("smeargle")),
            Map.entry(VillagerProfession.BUTCHER, List.of("farfetchd")),
            Map.entry(VillagerProfession.NITWIT, NO_JOB),
            Map.entry(VillagerProfession.NONE, NO_JOB)
    );
    /** Pequenos o bastante para andar na cabeça do aldeão. */
    private static final Set<String> HEAD_RIDERS = Set.of("pichu", "pidgey", "natu", "rowlet", "togepi");

    private static final String ADOPT_KEY = LivingFaunaFlora.MOD_ID + ":adopt";
    private static final String ADOPT_SPECIES_KEY = LivingFaunaFlora.MOD_ID + ":adopt_species";
    private static final String ADOPT_SINCE_KEY = LivingFaunaFlora.MOD_ID + ":adopt_since";
    private static final String COMPANION_KEY = LivingFaunaFlora.MOD_ID + ":companion";
    private static final String VILLAGER_KEY = LivingFaunaFlora.MOD_ID + ":villager";

    private static final Set<Villager> waiting = new HashSet<>();
    private static final Set<PokemonEntity> companions = new HashSet<>();
    private static int tick;

    /** Só aldeões criados junto com a vila podem adotar. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() == EntityType.VILLAGER && event.getSpawnType() == MobSpawnType.STRUCTURE
                && event.getLevel().getRandom().nextFloat() < ADOPT_CHANCE) {
            event.getEntity().getPersistentData().putBoolean(ADOPT_KEY, true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (event.getEntity() instanceof Villager villager && villager.getPersistentData().getBoolean(ADOPT_KEY)
                && !villager.getPersistentData().hasUUID(COMPANION_KEY)) {
            waiting.add(villager);
        } else if (event.getEntity() instanceof PokemonEntity pokemon && pokemon.getPersistentData().hasUUID(VILLAGER_KEY)) {
            PokemonAi.markAsVillagePokemon(pokemon); // também corrige nomes antigos
            companions.add(pokemon);
        }
    }

    /**
     * O aldeão passa por lugares com menos de 3 blocos de altura, e quem está na cabeça dele ficaria dentro do
     * bloco. Pokémon de aldeão não sufocam.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof PokemonEntity pokemon && event.getSource().is(DamageTypes.IN_WALL)
                && pokemon.getPersistentData().hasUUID(VILLAGER_KEY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tick % 40 != 0) return;

        waiting.removeIf(villager -> {
            if (villager.isRemoved() || villager.getPersistentData().hasUUID(COMPANION_KEY)) return true;
            ServerLevel level = (ServerLevel) villager.level();
            if (!villager.getPersistentData().contains(ADOPT_SINCE_KEY)) {
                villager.getPersistentData().putLong(ADOPT_SINCE_KEY, level.getGameTime());
            }
            // Espera o aldeão arrumar emprego, para o Pokémon combinar com a profissão.
            boolean forced = villager.getPersistentData().contains(ADOPT_SPECIES_KEY);
            boolean hasJob = villager.getVillagerData().getProfession() != VillagerProfession.NONE;
            boolean waitedEnough = level.getGameTime() - villager.getPersistentData().getLong(ADOPT_SINCE_KEY) > MAX_WAIT_FOR_JOB;
            if (!forced && !hasJob && !villager.isBaby() && !waitedEnough) return false;
            adopt(villager, level);
            return true;
        });

        companions.removeIf(pokemon -> {
            if (pokemon.isRemoved()) return true;
            ServerLevel level = (ServerLevel) pokemon.level();
            Entity villager = level.getEntity(pokemon.getPersistentData().getUUID(VILLAGER_KEY));
            // Aldeão morreu ou virou zumbi: o Pokémon vai embora junto.
            if (!(villager instanceof Villager) || !villager.isAlive()) {
                pokemon.discard();
                return true;
            }
            if (pokemon.isPassenger() || pokemon.isBusy()) return false;
            double distance = pokemon.distanceTo(villager);
            if (distance > 24) {
                pokemon.teleportTo(villager.getX(), villager.getY(), villager.getZ());
            } else if (distance > 5) {
                PokemonAi.walkTo(pokemon, villager, 0.5f, 3);
            }
            return false;
        });
    }

    private static void adopt(Villager villager, ServerLevel level) {
        String species = chooseSpecies(villager, level.random);
        PokemonEntity pokemon = PokemonProperties.Companion
                .parse(species + " level=" + (5 + level.random.nextInt(11)) + " uncatchable")
                .createEntity(level);
        pokemon.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), 0f);
        pokemon.getPersistentData().putUUID(VILLAGER_KEY, villager.getUUID());
        KeptPokemon.keep(pokemon);
        PokemonAi.markAsVillagePokemon(pokemon);
        if (!level.addFreshEntity(pokemon)) return;

        villager.getPersistentData().putUUID(COMPANION_KEY, pokemon.getUUID());
        companions.add(pokemon);
        if (HEAD_RIDERS.contains(species)) pokemon.startRiding(villager, true);
    }

    private static String chooseSpecies(Villager villager, RandomSource random) {
        String forced = villager.getPersistentData().getString(ADOPT_SPECIES_KEY);
        if (!forced.isEmpty()) return forced;
        if (villager.isBaby()) return pick(BABY, random);
        if (random.nextFloat() < GENERIC_CHANCE) return pick(GENERIC, random);

        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.WEAPONSMITH && random.nextFloat() >= HONEDGE_CHANCE) return pick(GENERIC, random);
        return pick(BY_PROFESSION.getOrDefault(profession, GENERIC), random);
    }

    private static String pick(List<String> options, RandomSource random) {
        return options.get(random.nextInt(options.size()));
    }
}
