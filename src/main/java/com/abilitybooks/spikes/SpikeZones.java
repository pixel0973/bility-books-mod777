package com.abilitybooks.spikes;

import com.abilitybooks.display.Displays;
import com.abilitybooks.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Активные зоны костяных шипов: из земли вылезает много острий копья. */
public final class SpikeZones {
    private SpikeZones() {}

    /** Сколько тиков живут шипы (100 = 5 секунд). */
    public static final int DURATION_TICKS = 100;
    public static final float DAMAGE = 0.5f;

    /** На сколько блоков шип спрятан под землёй (должно быть больше его высоты). */
    private static final float BURIED = -2.8f;
    private static final int RISE_TICKS = 4;
    private static final int SINK_TICKS = 8;

    private record Spike(DisplayEntity.ItemDisplayEntity entity, float scale, float[] rot, int emergeAt) {}

    private record Zone(ServerWorld world, Box box, UUID owner, long start, long end, List<Spike> spikes) {}

    private static final List<Zone> ZONES = new ArrayList<>();

    public static void add(ServerWorld world, Box box, UUID owner) {
        long t = world.getTime();
        List<Spike> spikes = new ArrayList<>();
        Random r = world.random;
        int count = Math.max(14, (int) (box.getXLength() * box.getZLength() * 1.3));
        for (int i = 0; i < count; i++) {
            double x = box.minX + r.nextDouble() * box.getXLength();
            double z = box.minZ + r.nextDouble() * box.getZLength();
            double y = groundY(world, x, box.minY + 1.0, z);

            // лёгкий наклон в случайную сторону
            double dir = r.nextDouble() * Math.PI * 2;
            double half = (r.nextDouble() - 0.5) * 0.5 / 2.0;
            float[] rot = {
                    (float) (Math.cos(dir) * Math.sin(half)), 0f,
                    (float) (Math.sin(dir) * Math.sin(half)), (float) Math.cos(half)};
            float scale = 0.9f + r.nextFloat() * 0.7f;

            DisplayEntity.ItemDisplayEntity e = Displays.spawn(world, ModItems.BONE_SPIKE, x, y, z,
                    r.nextFloat() * 360f, 0f, scale, rot, 0f, BURIED, 0f);
            if (e != null) spikes.add(new Spike(e, scale, rot, 1 + r.nextInt(10)));
        }
        ZONES.add(new Zone(world, box, owner, t, t + DURATION_TICKS, spikes));
    }

    /** Верх поверхности под точкой (ищем вниз до 6 блоков от уровня игрока). */
    private static double groundY(ServerWorld world, double x, double startY, double z) {
        BlockPos.Mutable pos = new BlockPos.Mutable(Math.floor(x), Math.floor(startY) + 1, Math.floor(z));
        for (int i = 0; i < 8; i++) {
            BlockPos below = pos.down();
            if (!world.getBlockState(below).getCollisionShape(world, below).isEmpty()) {
                return pos.getY();
            }
            pos.move(0, -1, 0);
        }
        return startY;
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Zone z : ZONES) removeSpikes(z);
            ZONES.clear();
        });
    }

    private static void removeSpikes(Zone z) {
        for (Spike s : z.spikes()) Displays.remove(s.entity());
    }

    private static void tick() {
        if (ZONES.isEmpty()) return;
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            long now = z.world().getTime();
            if (now >= z.end()) {
                removeSpikes(z);
                it.remove();
                continue;
            }
            int age = (int) (now - z.start());

            for (Spike s : z.spikes()) {
                if (s.entity().isRemoved()) continue;
                if (age == s.emergeAt()) {
                    Displays.apply(s.entity(), ModItems.BONE_SPIKE, s.scale(), s.rot(), 0f, 0f, 0f, RISE_TICKS);
                    z.world().playSound(null, s.entity().getX(), s.entity().getY(), s.entity().getZ(),
                            SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 0.7f + z.world().random.nextFloat() * 0.5f);
                } else if (age == DURATION_TICKS - SINK_TICKS - 2) {
                    Displays.apply(s.entity(), ModItems.BONE_SPIKE, s.scale(), s.rot(), 0f, BURIED, 0f, SINK_TICKS);
                }
            }

            if (age % 4 == 0) particles(z);
            if (age % 20 == 0 && age > 0) damage(z);
        }
    }

    private static void particles(Zone z) {
        ServerWorld w = z.world();
        Box b = z.box();
        Random r = w.random;
        ItemStackParticleEffect bone = new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.BONE));
        for (int i = 0; i < 4; i++) {
            double x = b.minX + r.nextDouble() * (b.maxX - b.minX);
            double zz = b.minZ + r.nextDouble() * (b.maxZ - b.minZ);
            double y = b.minY + 1.0;
            w.spawnParticles(bone, x, y, zz, 0, 0.0, 1.0, 0.0, 0.25);
        }
    }

    private static void damage(Zone z) {
        List<LivingEntity> targets = z.world().getEntitiesByClass(LivingEntity.class, z.box(),
                e -> e.isAlive() && !e.isSpectator() && !e.getUuid().equals(z.owner()));
        for (LivingEntity e : targets) {
            e.damage(z.world().getDamageSources().magic(), DAMAGE);
        }
    }
}
