package com.abilitybooks;

import com.abilitybooks.ability.AbilityBookItem;
import com.abilitybooks.ability.SpearProjectiles;
import com.abilitybooks.display.Displays;
import com.abilitybooks.command.ModCommands;
import com.abilitybooks.item.ModItems;
import com.abilitybooks.network.ModNetworking;
import com.abilitybooks.spikes.SpikeZones;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class AbilityBooksMod implements ModInitializer {
    public static final String MOD_ID = "abilitybooks";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.register();
        ModNetworking.registerServer();
        ModCommands.register();
        Displays.register();
        SpearProjectiles.register();
        SpikeZones.register();

        // С книгой в руке ЛКМ не ломает блоки (ЛКМ = способность)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                player.getMainHandStack().getItem() instanceof AbilityBookItem ? ActionResult.FAIL : ActionResult.PASS);
    }
}
