package com.abilitybooks.state;

import com.abilitybooks.ability.Ability;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Прогресс игроков: открытые способности, перезарядки, точка возврата из мира испытаний. */
public class AbilityState extends PersistentState {
    private static final String KEY = "abilitybooks_state";

    public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch) {}

    private final Map<UUID, Set<String>> unlocked = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, ReturnPoint> returns = new HashMap<>();

    public static AbilityState get(MinecraftServer server) {
        PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
        return mgr.getOrCreate(AbilityState::fromNbt, AbilityState::new, KEY);
    }

    public static long now(MinecraftServer server) {
        return server.getOverworld().getTime();
    }

    // ---- способности ----
    public boolean isUnlocked(UUID player, Ability a) {
        return a.isFree() || unlocked.getOrDefault(player, Set.of()).contains(a.id);
    }

    public void unlock(UUID player, Ability a) {
        unlocked.computeIfAbsent(player, k -> new HashSet<>()).add(a.id);
        markDirty();
    }

    public void resetPlayer(UUID player) {
        unlocked.remove(player);
        cooldowns.remove(player);
        markDirty();
    }

    // ---- перезарядки ----
    public int cooldownLeft(UUID player, Ability a, long now) {
        Long end = cooldowns.getOrDefault(player, Map.of()).get(a.id);
        if (end == null) return 0;
        return (int) Math.max(0, end - now);
    }

    public void setCooldown(UUID player, Ability a, long endTick) {
        cooldowns.computeIfAbsent(player, k -> new HashMap<>()).put(a.id, endTick);
        markDirty();
    }

    // ---- возврат из мира испытаний ----
    public void setReturn(UUID player, ReturnPoint point) {
        returns.put(player, point);
        markDirty();
    }

    public ReturnPoint getReturn(UUID player) {
        return returns.get(player);
    }

    // ---- NBT ----
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        Set<UUID> all = new HashSet<>();
        all.addAll(unlocked.keySet());
        all.addAll(cooldowns.keySet());
        all.addAll(returns.keySet());

        NbtList list = new NbtList();
        for (UUID id : all) {
            NbtCompound p = new NbtCompound();
            p.putUuid("Id", id);

            NbtList u = new NbtList();
            for (String s : unlocked.getOrDefault(id, Set.of())) u.add(NbtString.of(s));
            p.put("Unlocked", u);

            NbtCompound c = new NbtCompound();
            for (Map.Entry<String, Long> e : cooldowns.getOrDefault(id, Map.of()).entrySet()) {
                c.putLong(e.getKey(), e.getValue());
            }
            p.put("Cooldowns", c);

            ReturnPoint r = returns.get(id);
            if (r != null) {
                NbtCompound rc = new NbtCompound();
                rc.putString("Dim", r.dimension());
                rc.putDouble("X", r.x());
                rc.putDouble("Y", r.y());
                rc.putDouble("Z", r.z());
                rc.putFloat("Yaw", r.yaw());
                rc.putFloat("Pitch", r.pitch());
                p.put("Return", rc);
            }
            list.add(p);
        }
        nbt.put("Players", list);
        return nbt;
    }

    public static AbilityState fromNbt(NbtCompound nbt) {
        AbilityState s = new AbilityState();
        NbtList list = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound p = list.getCompound(i);
            UUID id = p.getUuid("Id");

            Set<String> set = new HashSet<>();
            NbtList u = p.getList("Unlocked", NbtElement.STRING_TYPE);
            for (int j = 0; j < u.size(); j++) set.add(u.getString(j));
            if (!set.isEmpty()) s.unlocked.put(id, set);

            NbtCompound c = p.getCompound("Cooldowns");
            Map<String, Long> map = new HashMap<>();
            for (String key : c.getKeys()) map.put(key, c.getLong(key));
            if (!map.isEmpty()) s.cooldowns.put(id, map);

            if (p.contains("Return", NbtElement.COMPOUND_TYPE)) {
                NbtCompound rc = p.getCompound("Return");
                s.returns.put(id, new ReturnPoint(rc.getString("Dim"), rc.getDouble("X"), rc.getDouble("Y"),
                        rc.getDouble("Z"), rc.getFloat("Yaw"), rc.getFloat("Pitch")));
            }
        }
        return s;
    }
}
