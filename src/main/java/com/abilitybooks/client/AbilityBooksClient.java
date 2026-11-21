package com.abilitybooks.client;

import com.abilitybooks.ability.AbilityBookItem;
import com.abilitybooks.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AbilityBooksClient implements ClientModInitializer {
    public static KeyBinding MENU_KEY;

    @Override
    public void onInitializeClient() {
        MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.abilitybooks.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.abilitybooks"));

        // ЛКМ с книгой в руке = способность. Нажатия «съедаем» до того, как их увидит ваниль.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null || client.currentScreen != null) return;
            if (client.player.getMainHandStack().getItem() instanceof AbilityBookItem) {
                while (client.options.attackKey.wasPressed()) {
                    client.player.swingHand(Hand.MAIN_HAND);
                    ClientPlayNetworking.send(ModNetworking.USE_ABILITY, PacketByteBufs.create());
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (MENU_KEY.wasPressed()) {
                if (client.player != null && client.currentScreen == null
                        && client.player.getMainHandStack().getItem() instanceof AbilityBookItem) {
                    ClientPlayNetworking.send(ModNetworking.REQUEST_MENU, PacketByteBufs.create());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.MENU_DATA, (client, handler, buf, sender) -> {
            int selected = buf.readInt();
            int n = buf.readInt();
            boolean[] unlocked = new boolean[n];
            int[] cooldown = new int[n];
            for (int i = 0; i < n; i++) {
                unlocked[i] = buf.readBoolean();
                cooldown[i] = buf.readInt();
            }
            client.execute(() -> {
                if (client.player == null) return;
                ItemStack stack = client.player.getMainHandStack();
                if (stack.getItem() instanceof AbilityBookItem book && book.getAbilities().size() == n) {
                    if (client.currentScreen instanceof AbilityMenuScreen menu) {
                        menu.update(selected, unlocked, cooldown);
                    } else {
                        client.setScreen(new AbilityMenuScreen(book, stack, selected, unlocked, cooldown));
                    }
                }
            });
        });
    }
}
