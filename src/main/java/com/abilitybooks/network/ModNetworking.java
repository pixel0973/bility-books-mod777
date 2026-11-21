package com.abilitybooks.network;

import com.abilitybooks.AbilityBooksMod;
import com.abilitybooks.ability.AbilityLogic;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    private ModNetworking() {}

    // клиент -> сервер
    public static final Identifier USE_ABILITY = AbilityBooksMod.id("use_ability");
    public static final Identifier SELECT_ABILITY = AbilityBooksMod.id("select_ability");
    public static final Identifier REQUEST_MENU = AbilityBooksMod.id("request_menu");
    public static final Identifier UNLOCK_ABILITY = AbilityBooksMod.id("unlock_ability");
    // сервер -> клиент
    public static final Identifier MENU_DATA = AbilityBooksMod.id("menu_data");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(USE_ABILITY, (server, player, handler, buf, sender) ->
                server.execute(() -> AbilityLogic.useSelected(player)));

        ServerPlayNetworking.registerGlobalReceiver(SELECT_ABILITY, (server, player, handler, buf, sender) -> {
            int index = buf.readInt();
            server.execute(() -> AbilityLogic.select(player, index));
        });

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_MENU, (server, player, handler, buf, sender) ->
                server.execute(() -> AbilityLogic.sendMenu(player)));

        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_ABILITY, (server, player, handler, buf, sender) -> {
            int index = buf.readInt();
            server.execute(() -> AbilityLogic.unlock(player, index));
        });
    }
}
