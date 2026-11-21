package com.abilitybooks.display;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Временные «картинки» (item_display) для копья и шипов. Рисуются ванильным клиентом по модели
 * предмета, поэтому отдельный рендерер не нужен.
 */
public final class Displays {
    private Displays() {}

    /** Если копьё летит остриём НАЗАД, поменяй на true. */
    public static final boolean FLIP = false;

    private static final String TAG = "abilitybooks_temp";
    private static final Set<UUID> LIVE = ConcurrentHashMap.newKeySet();

    public static void register() {
        // Если сервер упал и временные сущности остались в мире — удаляем при загрузке
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(TAG) && !LIVE.contains(entity.getUuid())) {
                entity.discard();
            }
        });
    }

    public static float[] noRotation() {
        return FLIP ? new float[]{0f, 1f, 0f, 0f} : new float[]{0f, 0f, 0f, 1f};
    }

    public static DisplayEntity.ItemDisplayEntity spawn(ServerWorld world, Item item, double x, double y, double z,
                                                        float yaw, float pitch, float scale, float[] rotation,
                                                        float tx, float ty, float tz) {
        DisplayEntity.ItemDisplayEntity e = EntityType.ITEM_DISPLAY.create(world);
        if (e == null) return null;
        e.refreshPositionAndAngles(x, y, z, yaw, pitch);
        e.addCommandTag(TAG);
        apply(e, item, scale, rotation, tx, ty, tz, 0);
        LIVE.add(e.getUuid());
        world.spawnEntity(e);
        return e;
    }

    /** Меняет размер/поворот/смещение; duration > 0 — плавная анимация (в тиках). */
    public static void apply(DisplayEntity.ItemDisplayEntity e, Item item, float scale, float[] rotation,
                             float tx, float ty, float tz, int duration) {
        NbtCompound nbt = new NbtCompound();
        e.writeNbt(nbt);
        nbt.put("item", new ItemStack(item).writeNbt(new NbtCompound()));
        nbt.putString("item_display", "fixed");

        NbtCompound tr = new NbtCompound();
        tr.put("left_rotation", floats(rotation[0], rotation[1], rotation[2], rotation[3]));
        tr.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tr.put("scale", floats(scale, scale, scale));
        tr.put("translation", floats(tx, ty, tz));
        nbt.put("transformation", tr);

        nbt.putInt("interpolation_duration", duration);
        nbt.putInt("start_interpolation", 0);
        nbt.putFloat("view_range", 3.0f);
        e.readNbt(nbt);
    }

    public static void remove(Entity e) {
        if (e == null) return;
        LIVE.remove(e.getUuid());
        e.discard();
    }

    private static NbtList floats(float... v) {
        NbtList list = new NbtList();
        for (float f : v) list.add(NbtFloat.of(f));
        return list;
    }
}
