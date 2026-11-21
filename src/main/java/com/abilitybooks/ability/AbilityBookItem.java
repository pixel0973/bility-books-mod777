package com.abilitybooks.ability;

import com.abilitybooks.state.AbilityState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AbilityBookItem extends Item {
    private final List<Ability> abilities;

    public AbilityBookItem(Settings settings, List<Ability> abilities) {
        super(settings);
        this.abilities = abilities;
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public static int getSelected(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt("SelectedAbility");
    }

    public static void setSelected(ItemStack stack, int index) {
        stack.getOrCreateNbt().putInt("SelectedAbility", index);
    }

    /** Shift + ПКМ: переключить на следующую открытую способность. */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.isSneaking()) {
            if (!world.isClient && user instanceof ServerPlayerEntity sp) {
                cycle(sp, stack);
            }
            return TypedActionResult.success(stack, world.isClient);
        }
        return TypedActionResult.pass(stack);
    }

    private void cycle(ServerPlayerEntity player, ItemStack stack) {
        AbilityState state = AbilityState.get(player.getServer());
        int n = abilities.size();
        int cur = Math.max(0, Math.min(getSelected(stack), n - 1));
        for (int i = 1; i <= n; i++) {
            int idx = (cur + i) % n;
            Ability a = abilities.get(idx);
            if (state.isUnlocked(player.getUuid(), a)) {
                setSelected(stack, idx);
                player.sendMessage(Text.literal("Способность: " + a.name).formatted(Formatting.GOLD), true);
                return;
            }
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int sel = Math.max(0, Math.min(getSelected(stack), abilities.size() - 1));
        tooltip.add(Text.literal("Выбрано: " + abilities.get(sel).name).formatted(Formatting.GOLD));
        tooltip.add(Text.literal("ЛКМ — использовать способность").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Shift + ПКМ — сменить способность").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("R — меню книги").formatted(Formatting.GRAY));
    }
}
