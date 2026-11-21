package com.abilitybooks.ability;

import net.minecraft.item.Items;

import java.util.List;

/** Здесь описываются все книги и их способности. Чтобы добавить книгу — добавьте новый список. */
public final class Abilities {
    private Abilities() {}

    /** Порядок = порядок в дереве: каждая следующая способность открывается только после предыдущей. */
    public static final List<Ability> BONE_KING = List.of(
            new Ability("bone_spear", "Костяное копьё", 10 * 20,
                    0, Items.BONE,
                    BoneKingActions::boneSpear),
            new Ability("bone_spikes", "Костяные шипы", 20 * 20,
                    40, Items.BONE_BLOCK,
                    BoneKingActions::boneSpikes),
            new Ability("pants_thief", "Похититель трусов", 30 * 60 * 20,
                    50, Items.LEATHER_LEGGINGS,
                    BoneKingActions::pantsThief)
    );
}
