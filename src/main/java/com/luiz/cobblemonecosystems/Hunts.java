package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.joml.Vector3f;

/**
 * Caçada controlada pelo mod, usando os golpes do próprio Pokémon:
 * - a cada ataque escolhe um golpe de dano do moveset (Ember, Shadow Punch, Bite...);
 * - golpe físico precisa chegar perto; golpe especial ataca de longe, com um rastro de partículas;
 * - o dano vem da força do golpe e do nível (com bônus quando o golpe é do mesmo tipo do Pokémon);
 * - partículas, som e efeito extra pelo tipo do golpe (fogo queima, gelo deixa lento, veneno envenena).
 * Não mexe no cérebro do Cobblemon (trocar os comportamentos dele deixava o Pokémon parado).
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public final class Hunts {
    /** Desiste se não alcançar o alvo nesse tempo. */
    private static final int GIVE_UP_TICKS = 400;
    private static final double MAX_CHASE_DISTANCE = 24;
    /** Golpes especiais acertam a partir dessa distância. */
    private static final double SPECIAL_RANGE = 7;
    /** Bônus de golpe do mesmo tipo do Pokémon (o STAB das batalhas). */
    private static final float SAME_TYPE_BONUS = 1.25f;

    private record Hunt(LivingEntity target, float minDamage, double knockUp, int cooldown, long startedAt) {}
    private static final Map<PokemonEntity, Hunt> hunts = new HashMap<>();
    private static final Map<PokemonEntity, Long> nextHit = new HashMap<>();
    private static int tick;

    private Hunts() {}

    public static boolean isHunting(PokemonEntity hunter) {
        return hunts.containsKey(hunter);
    }

    /**
     * Começa (ou troca) a caçada. minDamage é o dano mínimo de cada golpe, para quem tem golpes fracos;
     * knockUp joga o alvo para cima, como o golem de ferro.
     */
    public static void start(PokemonEntity hunter, LivingEntity target, float minDamage, double knockUp, int cooldown) {
        hunts.put(hunter, new Hunt(target, minDamage, knockUp, cooldown, hunter.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tick % 5 != 0) return;
        hunts.entrySet().removeIf(entry -> !step(entry.getKey(), entry.getValue()));
        nextHit.keySet().removeIf(hunter -> hunter.isRemoved() && !hunts.containsKey(hunter));
    }

    /** Um passo da caçada. Devolve false quando ela acabou. */
    private static boolean step(PokemonEntity hunter, Hunt hunt) {
        LivingEntity target = hunt.target();
        if (hunter.isRemoved() || hunter.isBusy() || !target.isAlive() || target.level() != hunter.level()) return false;
        long now = hunter.level().getGameTime();
        double distance = hunter.distanceTo(target);
        if (now - hunt.startedAt() > GIVE_UP_TICKS || distance > MAX_CHASE_DISTANCE) return false;

        boolean ready = now >= nextHit.getOrDefault(hunter, 0L);
        Move move = ready ? pickMove(hunter) : null;
        boolean special = move != null && move.getDamageCategory().getName().equals("special");
        double reach = special ? SPECIAL_RANGE : 1.2 + (hunter.getBbWidth() + target.getBbWidth()) / 2;

        if (distance > reach || !hunter.hasLineOfSight(target)) {
            PokemonAi.walkTo(hunter, target, 0.7f, 0);
            return true;
        }
        hunter.getLookControl().setLookAt(target);
        if (!ready) return true;

        hit(hunter, target, hunt, move, special);
        nextHit.put(hunter, now + hunt.cooldown());
        return true;
    }

    private static void hit(PokemonEntity hunter, LivingEntity target, Hunt hunt, Move move, boolean special) {
        ServerLevel level = (ServerLevel) hunter.level();
        ElementalType type = move != null ? move.getType() : hunter.getPokemon().getPrimaryType();
        String typeName = type.getName();

        float damage = hunt.minDamage();
        if (move != null) {
            float levelFactor = 0.6f + hunter.getPokemon().getLevel() / 100f;
            float bonus = isSameType(hunter, type) ? SAME_TYPE_BONUS : 1f;
            damage = Math.max(damage, (float) move.getPower() / 8f * levelFactor * bonus);
        }

        if (special) trail(level, hunter, target, typeColor(typeName));
        if (target.hurt(level.damageSources().mobAttack(hunter), damage)) {
            Vec3 push = target.position().subtract(hunter.position()).normalize().scale(0.4);
            target.push(push.x, hunt.knockUp(), push.z);
            target.hurtMarked = true;
            typeEffect(target, typeName);
        }
        // Impacto: a partícula do tipo e uma explosão de pó na cor do tipo.
        level.sendParticles(typeColor(typeName), target.getX(), target.getY(0.5), target.getZ(), 10,
                target.getBbWidth() / 2, target.getBbHeight() / 3, target.getBbWidth() / 2, 0.05);
        level.sendParticles(particle(typeName), target.getX(), target.getY(0.5), target.getZ(), 12,
                target.getBbWidth() / 2, target.getBbHeight() / 3, target.getBbWidth() / 2, 0.05);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), sound(typeName, special), SoundSource.NEUTRAL, 0.8f,
                0.9f + level.random.nextFloat() * 0.2f);
    }

    /** Um golpe de dano sorteado do moveset, ou null se não tiver nenhum. */
    private static Move pickMove(PokemonEntity hunter) {
        List<Move> damaging = new ArrayList<>();
        for (Move move : hunter.getPokemon().getMoveSet().getMoves()) {
            if (move.getPower() > 0 && !move.getDamageCategory().getName().equals("status")) damaging.add(move);
        }
        return damaging.isEmpty() ? null : damaging.get(hunter.getRandom().nextInt(damaging.size()));
    }

    private static boolean isSameType(PokemonEntity hunter, ElementalType type) {
        for (ElementalType own : hunter.getPokemon().getTypes()) {
            if (own == type) return true;
        }
        return false;
    }

    /** Rastro de partículas do Pokémon até o alvo, para golpes especiais, na cor do tipo. */
    private static void trail(ServerLevel level, PokemonEntity hunter, LivingEntity target, ParticleOptions particle) {
        Vec3 from = hunter.getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        int steps = (int) (from.distanceTo(to) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static void typeEffect(LivingEntity target, String type) {
        switch (type) {
            case "fire" -> { if (!target.fireImmune()) target.igniteForSeconds(3); }
            case "ice" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case "poison" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            default -> {}
        }
    }

    /** Pó na cor oficial do tipo (as mesmas cores dos jogos). */
    private static ParticleOptions typeColor(String type) {
        int rgb = switch (type) {
            case "fire" -> 0xEE8130;
            case "water" -> 0x6390F0;
            case "electric" -> 0xF7D02C;
            case "grass" -> 0x7AC74C;
            case "ice" -> 0x96D9D6;
            case "fighting" -> 0xC22E28;
            case "poison" -> 0xA33EA1;
            case "ground" -> 0xE2BF65;
            case "flying" -> 0xA98FF3;
            case "psychic" -> 0xF95587;
            case "bug" -> 0xA6B91A;
            case "rock" -> 0xB6A136;
            case "ghost" -> 0x735797;
            case "dragon" -> 0x6F35FC;
            case "dark" -> 0x705746;
            case "steel" -> 0xB7B7CE;
            case "fairy" -> 0xD685AD;
            default -> 0xA8A77A; // normal
        };
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), 1.3f);
    }

    private static ParticleOptions particle(String type) {
        return switch (type) {
            case "fire" -> ParticleTypes.FLAME;
            case "water" -> ParticleTypes.SPLASH;
            case "grass" -> ParticleTypes.COMPOSTER;
            case "electric" -> ParticleTypes.ELECTRIC_SPARK;
            case "ice" -> ParticleTypes.SNOWFLAKE;
            case "poison" -> ParticleTypes.WITCH;
            case "ground" -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState());
            case "rock" -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
            case "flying" -> ParticleTypes.CLOUD;
            case "psychic" -> ParticleTypes.PORTAL;
            case "ghost" -> ParticleTypes.SOUL;
            case "dark" -> ParticleTypes.SQUID_INK;
            case "dragon" -> ParticleTypes.DRAGON_BREATH;
            case "fairy" -> ParticleTypes.END_ROD;
            case "steel" -> ParticleTypes.WAX_OFF;
            default -> ParticleTypes.CRIT; // normal, fighting, bug
        };
    }

    private static SoundEvent sound(String type, boolean special) {
        return switch (type) {
            case "fire" -> SoundEvents.FIRECHARGE_USE;
            case "water" -> SoundEvents.GENERIC_SPLASH;
            case "ice" -> SoundEvents.GLASS_BREAK;
            case "electric" -> SoundEvents.FIREWORK_ROCKET_BLAST;
            default -> special ? SoundEvents.ILLUSIONER_CAST_SPELL : SoundEvents.PLAYER_ATTACK_STRONG;
        };
    }
}
