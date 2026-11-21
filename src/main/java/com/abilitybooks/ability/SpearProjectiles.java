package com.abilitybooks.ability;

import com.abilitybooks.display.Displays;
import com.abilitybooks.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Большое костяное копьё: появляется перед взглядом игрока, зависает и вылетает вперёд. */
public final class SpearProjectiles {
    private SpearProjectiles() {}

    private static final int WINDUP_TICKS = 10;   // сколько копьё висит перед игроком
    private static final double SPEED = 2.0;      // блоков за тик
    private static final int MAX_FLIGHT_TICKS = 30;
    private static final double TIP = 2.2;        // расстояние от центра копья до острия
    private static final float DAMAGE = 2.5f;
    private static final float SCALE = 1.6f;

    private static final class Spear {
        final ServerWorld world;
        final UUID owner;
        final DisplayEntity.ItemDisplayEntity entity;
        int age = 0;
        Vec3d dir;
        float yaw;
        float pitch;

        Spear(ServerWorld world, UUID owner, DisplayEntity.ItemDisplayEntity entity, Vec3d dir, float yaw, float pitch) {
            this.world = world;
            this.owner = owner;
            this.entity = entity;
            this.dir = dir;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final List<Spear> SPEARS = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Spear s : SPEARS) Displays.remove(s.entity);
            SPEARS.clear();
        });
    }

    private static Vec3d holdPos(ServerPlayerEntity p) {
        return p.getEyePos().add(p.getRotationVec(1.0f).multiply(3.2)).add(0, -0.3, 0);
    }

    public static boolean launch(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d c = holdPos(player);
        DisplayEntity.ItemDisplayEntity e = Displays.spawn(world, ModItems.BONE_SPEAR, c.x, c.y, c.z,
                player.getYaw(), player.getPitch(), SCALE, Displays.noRotation(), 0f, 0f, 0f);
        if (e == null) return false;
        SPEARS.add(new Spear(world, player.getUuid(), e, player.getRotationVec(1.0f), player.getYaw(), player.getPitch()));
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BONE_BLOCK_PLACE, SoundCategory.PLAYERS, 1.5f, 0.6f);
        return true;
    }

    private static ItemStackParticleEffect bone() {
        return new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.BONE));
    }

    private static void tick() {
        if (SPEARS.isEmpty()) return;
        Iterator<Spear> it = SPEARS.iterator();
        while (it.hasNext()) {
            Spear s = it.next();
            if (s.entity.isRemoved()) {
                it.remove();
                continue;
            }
            s.age++;
            ServerPlayerEntity owner = s.world.getServer().getPlayerManager().getPlayer(s.owner);

            // 1) копьё висит перед взглядом игрока и следует за ним
            if (s.age <= WINDUP_TICKS) {
                if (owner == null || !owner.isAlive()) {
                    Displays.remove(s.entity);
                    it.remove();
                    continue;
                }
                Vec3d c = holdPos(owner);
                s.dir = owner.getRotationVec(1.0f);
                s.yaw = owner.getYaw();
                s.pitch = owner.getPitch();
                s.entity.refreshPositionAndAngles(c.x, c.y, c.z, s.yaw, s.pitch);
                if (s.age % 2 == 0) {
                    s.world.spawnParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 2, 0.4, 0.4, 0.4, 0.02);
                }
                if (s.age == WINDUP_TICKS) {
                    s.world.playSound(null, c.x, c.y, c.z,
                            SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.6f);
                }
                continue;
            }

            // 2) полёт вперёд
            Vec3d pos = s.entity.getPos();
            Vec3d from = pos.add(s.dir.multiply(TIP));
            Vec3d to = from.add(s.dir.multiply(SPEED));

            BlockHitResult blockHit = s.world.raycast(new RaycastContext(from, to,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, s.entity));
            boolean blocked = blockHit.getType() != HitResult.Type.MISS;
            Vec3d end = blocked ? blockHit.getPos() : to;

            Box box = new Box(from, end).expand(0.6);
            EntityHitResult hit = ProjectileUtil.getEntityCollision(s.world, s.entity, from, end, box,
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && !e.getUuid().equals(s.owner));

            if (hit != null) {
                if (owner != null) {
                    hit.getEntity().damage(s.world.getDamageSources().playerAttack(owner), DAMAGE);
                } else {
                    hit.getEntity().damage(s.world.getDamageSources().magic(), DAMAGE);
                }
                impact(s, hit.getPos());
                Displays.remove(s.entity);
                it.remove();
                continue;
            }
            if (blocked) {
                impact(s, blockHit.getPos());
                Displays.remove(s.entity);
                it.remove();
                continue;
            }

            Vec3d next = pos.add(s.dir.multiply(SPEED));
            s.entity.refreshPositionAndAngles(next.x, next.y, next.z, s.yaw, s.pitch);
            s.world.spawnParticles(bone(), next.x, next.y, next.z, 1, 0.1, 0.1, 0.1, 0.0);

            if (s.age > WINDUP_TICKS + MAX_FLIGHT_TICKS) {
                Displays.remove(s.entity);
                it.remove();
            }
        }
    }

    private static void impact(Spear s, Vec3d p) {
        s.world.spawnParticles(bone(), p.x, p.y, p.z, 12, 0.3, 0.3, 0.3, 0.1);
        s.world.playSound(null, p.x, p.y, p.z, SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 0.8f);
    }
}
