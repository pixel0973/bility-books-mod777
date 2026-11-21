package com.abilitybooks.item;

import com.abilitybooks.AbilityBooksMod;
import com.abilitybooks.ability.Abilities;
import com.abilitybooks.ability.AbilityBookItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public final class ModItems {
    private ModItems() {}

    public static final AbilityBookItem BONE_KING_BOOK =
            new AbilityBookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), Abilities.BONE_KING);

    public static final Item UNDERWEAR = new Item(new FabricItemSettings().maxCount(16));

    /** Служебные предметы: только модель для копья и шипов (в креативе их нет). */
    public static final Item BONE_SPEAR = new Item(new FabricItemSettings().maxCount(1));
    public static final Item BONE_SPIKE = new Item(new FabricItemSettings().maxCount(1));

    public static final ItemGroup GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(BONE_KING_BOOK))
            .displayName(Text.literal("Книги способностей"))
            .entries((ctx, entries) -> {
                entries.add(BONE_KING_BOOK);
                entries.add(UNDERWEAR);
            })
            .build();

    public static void register() {
        Registry.register(Registries.ITEM, AbilityBooksMod.id("bone_king_book"), BONE_KING_BOOK);
        Registry.register(Registries.ITEM, AbilityBooksMod.id("underwear"), UNDERWEAR);
        Registry.register(Registries.ITEM, AbilityBooksMod.id("bone_spear_model"), BONE_SPEAR);
        Registry.register(Registries.ITEM, AbilityBooksMod.id("bone_spike_model"), BONE_SPIKE);
        Registry.register(Registries.ITEM_GROUP, AbilityBooksMod.id("main"), GROUP);
    }
}
