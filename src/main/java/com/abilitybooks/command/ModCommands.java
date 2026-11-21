package com.abilitybooks.command;

import com.abilitybooks.ability.Abilities;
import com.abilitybooks.ability.Ability;
import com.abilitybooks.item.ModItems;
import com.abilitybooks.state.AbilityState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * /abilitybooks give <игроки>      — выдать книгу «Костяной король»
 * /abilitybooks unlockall <игроки> — открыть все способности (для проверки)
 * /abilitybooks reset <игроки>     — сбросить прогресс и перезарядки
 */
public final class ModCommands {
    private ModCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("abilitybooks")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(ctx -> give(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                        .then(CommandManager.literal("unlockall")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(ctx -> unlockAll(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                        .then(CommandManager.literal("reset")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(ctx -> reset(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))));
    }

    private static int give(ServerCommandSource src, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity p : targets) {
            p.getInventory().offerOrDrop(new ItemStack(ModItems.BONE_KING_BOOK));
        }
        src.sendFeedback(() -> Text.literal("Книга выдана игрокам: " + targets.size()), true);
        return targets.size();
    }

    private static int unlockAll(ServerCommandSource src, Collection<ServerPlayerEntity> targets) {
        AbilityState state = AbilityState.get(src.getServer());
        for (ServerPlayerEntity p : targets) {
            for (Ability a : Abilities.BONE_KING) state.unlock(p.getUuid(), a);
        }
        src.sendFeedback(() -> Text.literal("Все способности открыты у игроков: " + targets.size()), true);
        return targets.size();
    }

    private static int reset(ServerCommandSource src, Collection<ServerPlayerEntity> targets) {
        AbilityState state = AbilityState.get(src.getServer());
        for (ServerPlayerEntity p : targets) state.resetPlayer(p.getUuid());
        src.sendFeedback(() -> Text.literal("Прогресс сброшен у игроков: " + targets.size()), true);
        return targets.size();
    }
}
