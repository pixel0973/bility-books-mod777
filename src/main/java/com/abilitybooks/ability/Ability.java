package com.abilitybooks.ability;

import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Ability {
    @FunctionalInterface
    public interface Action {
        /** @return true, если способность сработала (и надо запустить перезарядку) */
        boolean use(ServerPlayerEntity player);
    }

    public final String id;
    public final String name;
    public final int cooldownTicks;
    /** Сколько уровней опыта нужно (и тратится), чтобы прокачать способность. 0 = открыта с самого начала. */
    public final int levelCost;
    /** Иконка узла в меню. */
    public final Item icon;
    public final Action action;

    public Ability(String id, String name, int cooldownTicks, int levelCost, Item icon, Action action) {
        this.id = id;
        this.name = name;
        this.cooldownTicks = cooldownTicks;
        this.levelCost = levelCost;
        this.icon = icon;
        this.action = action;
    }

    public boolean isFree() {
        return levelCost <= 0;
    }
}
