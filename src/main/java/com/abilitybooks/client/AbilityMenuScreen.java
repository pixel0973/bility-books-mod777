package com.abilitybooks.client;

import com.abilitybooks.AbilityBooksMod;
import com.abilitybooks.ability.Ability;
import com.abilitybooks.ability.AbilityBookItem;
import com.abilitybooks.ability.AbilityLogic;
import com.abilitybooks.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Меню книги в стиле дерева достижений: тёмное окно с картинкой пустоты, узлы-способности
 * соединены линиями. Фон можно двигать мышью. ЛКМ по открытому узлу — выбрать способность,
 * по закрытому — прокачать за уровни опыта.
 */
public class AbilityMenuScreen extends Screen {
    private static final Identifier VOID_BG = AbilityBooksMod.id("textures/gui/void.png");

    private static final int WIN_W = 290;
    private static final int WIN_H = 190;
    private static final int NODE = 26;
    private static final int SPACING = 90;

    private final AbilityBookItem book;
    private final ItemStack stack;
    private int selected;
    private boolean[] unlocked;
    private int[] cooldown;

    private double panX;
    private double panY;
    private boolean panInit = false;
    private double dragDistance = 0;

    public AbilityMenuScreen(AbilityBookItem book, ItemStack stack, int selected, boolean[] unlocked, int[] cooldown) {
        super(Text.literal("Книга способностей"));
        this.book = book;
        this.stack = stack;
        this.selected = selected;
        this.unlocked = unlocked;
        this.cooldown = cooldown;
    }

    /** Обновить данные, не закрывая меню (после прокачки). */
    public void update(int selected, boolean[] unlocked, int[] cooldown) {
        this.selected = selected;
        this.unlocked = unlocked;
        this.cooldown = cooldown;
    }

    // ---- геометрия ----
    private int winX() { return (this.width - WIN_W) / 2; }
    private int winY() { return (this.height - WIN_H) / 2; }
    private int innerX() { return winX() + 9; }
    private int innerY() { return winY() + 20; }
    private int innerW() { return WIN_W - 18; }
    private int innerH() { return WIN_H - 29; }

    private int nodeX(int i) { return innerX() + (int) panX + i * SPACING; }
    private int nodeY() { return innerY() + (int) panY; }

    private int level() {
        return this.client != null && this.client.player != null ? this.client.player.experienceLevel : 0;
    }

    private boolean inInner(double mx, double my) {
        return mx >= innerX() && mx < innerX() + innerW() && my >= innerY() && my < innerY() + innerH();
    }

    private int nodeAt(double mx, double my) {
        if (!inInner(mx, my)) return -1;
        int n = book.getAbilities().size();
        for (int i = 0; i < n; i++) {
            int x = nodeX(i);
            int y = nodeY();
            if (mx >= x && mx < x + NODE && my >= y && my < y + NODE) return i;
        }
        return -1;
    }

    // ---- отрисовка ----
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        List<Ability> list = book.getAbilities();
        int n = list.size();

        if (!panInit) {
            panX = (innerW() - ((n - 1) * SPACING + NODE)) / 2.0;
            panY = (innerH() - NODE) / 2.0 - 8;
            panInit = true;
        }

        this.renderBackground(ctx);

        int wx = winX();
        int wy = winY();
        int ix = innerX();
        int iy = innerY();
        int iw = innerW();
        int ih = innerH();

        // рамка окна
        ctx.fill(wx - 2, wy - 2, wx + WIN_W + 2, wy + WIN_H + 2, 0xFF17120E);
        ctx.fill(wx, wy, wx + WIN_W, wy + WIN_H, 0xFF4A3B2E);
        ctx.fill(wx + 1, wy + 1, wx + WIN_W - 1, wy + WIN_H - 1, 0xFF6B5846);
        ctx.fill(wx + 2, wy + 2, wx + WIN_W - 2, wy + WIN_H - 2, 0xFF3A2E24);
        ctx.fill(ix - 1, iy - 1, ix + iw + 1, iy + ih + 1, 0xFF0B0A0E);

        // заголовок
        ctx.drawItem(stack, wx + 6, wy + 3);
        ctx.drawTextWithShadow(this.textRenderer, this.title, wx + 26, wy + 7, 0xFFE9E4D6);
        Text lvl = Text.literal("Уровень: " + level());
        ctx.drawTextWithShadow(this.textRenderer, lvl,
                wx + WIN_W - 8 - this.textRenderer.getWidth(lvl), wy + 7, 0xFF8CFF6B);

        // картинка пустоты
        ctx.drawTexture(VOID_BG, ix, iy, iw, ih, 0f, 0f, 256, 256, 256, 256);

        ctx.enableScissor(ix, iy, ix + iw, iy + ih);

        // линии между узлами
        int cy = nodeY() + NODE / 2;
        for (int i = 0; i < n - 1; i++) {
            int x1 = nodeX(i) + NODE;
            int x2 = nodeX(i + 1);
            int color = unlocked[i + 1] ? 0xFFFFD27A : 0xFF55556E;
            ctx.fill(x1, cy - 3, x2, cy + 3, 0xFF000000);
            ctx.fill(x1, cy - 1, x2, cy + 1, color);
        }

        // узлы
        int lvlNow = level();
        for (int i = 0; i < n; i++) {
            Ability a = list.get(i);
            int x = nodeX(i);
            int y = nodeY();
            boolean open = unlocked[i];
            boolean canBuy = !open && (i == 0 || unlocked[i - 1]) && lvlNow >= a.levelCost;

            int border;
            if (open && i == selected) border = 0xFFFFD700;
            else if (open) border = 0xFF9FD8A0;
            else if (canBuy) border = 0xFFB070FF;
            else border = 0xFF555560;
            int fill = open ? 0xFF2B2B3D : 0xFF14141C;

            ctx.fill(x, y, x + NODE, y + NODE, border);
            ctx.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, fill);
            ctx.drawItem(new ItemStack(a.icon), x + 5, y + 5);
            if (!open) {
                ctx.fill(x + 3, y + 3, x + NODE - 3, y + NODE - 3, 0xB0101018);
            } else if (cooldown[i] > 0) {
                ctx.fill(x + 3, y + 3, x + NODE - 3, y + NODE - 3, 0x80000000);
            }

            int cx = x + NODE / 2;
            if (open) {
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(a.name), cx, y + NODE + 5,
                        i == selected ? 0xFFFFD700 : 0xFFFFFFFF);
            } else {
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(a.levelCost + " ур."), cx, y + NODE + 5,
                        lvlNow >= a.levelCost ? 0xFF8CFF6B : 0xFFFF6B6B);
            }
        }

        ctx.disableScissor();

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("ЛКМ — выбрать / прокачать   •   тяни, чтобы двигать"),
                this.width / 2, iy + ih - 11, 0xFF8888AA);

        // подсказка
        int hover = nodeAt(mouseX, mouseY);
        if (hover >= 0) {
            ctx.drawTooltip(this.textRenderer, tooltip(hover), mouseX, mouseY);
        }
    }

    private List<Text> tooltip(int i) {
        List<Ability> list = book.getAbilities();
        Ability a = list.get(i);
        List<Text> t = new ArrayList<>();
        t.add(Text.literal(a.name).formatted(Formatting.GOLD));
        if (unlocked[i]) {
            t.add(Text.literal(i == selected ? "Выбрано" : "Открыто").formatted(Formatting.GREEN));
            t.add(Text.literal("Перезарядка: " + AbilityLogic.formatTicks(a.cooldownTicks)).formatted(Formatting.GRAY));
            if (cooldown[i] > 0) {
                t.add(Text.literal("Осталось: " + AbilityLogic.formatTicks(cooldown[i])).formatted(Formatting.RED));
            }
            if (i != selected) {
                t.add(Text.literal("ЛКМ — выбрать").formatted(Formatting.DARK_GRAY));
            }
        } else {
            t.add(Text.literal("Закрыто").formatted(Formatting.RED));
            boolean enough = level() >= a.levelCost;
            t.add(Text.literal("Нужно уровней: " + a.levelCost).formatted(enough ? Formatting.GREEN : Formatting.RED));
            t.add(Text.literal("У тебя: " + level()).formatted(Formatting.GRAY));
            if (i > 0 && !unlocked[i - 1]) {
                t.add(Text.literal("Сначала открой «" + list.get(i - 1).name + "»").formatted(Formatting.DARK_RED));
            } else if (enough) {
                t.add(Text.literal("ЛКМ — прокачать (уровни будут потрачены)").formatted(Formatting.LIGHT_PURPLE));
            }
        }
        return t;
    }

    // ---- мышь ----
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inInner(mouseX, mouseY)) {
            dragDistance = 0;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && inInner(mouseX, mouseY)) {
            dragDistance += Math.abs(deltaX) + Math.abs(deltaY);
            int n = book.getAbilities().size();
            double minX = -((n - 1) * SPACING + NODE) + 40;
            double maxX = innerW() - 40;
            double minY = -NODE + 20;
            double maxY = innerH() - 20;
            panX = Math.max(minX, Math.min(maxX, panX + deltaX));
            panY = Math.max(minY, Math.min(maxY, panY + deltaY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragDistance < 3) {
            int i = nodeAt(mouseX, mouseY);
            if (i >= 0) {
                clickNode(i);
                dragDistance = 100;
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clickNode(int i) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(i);
        if (unlocked[i]) {
            ClientPlayNetworking.send(ModNetworking.SELECT_ABILITY, buf);
            this.selected = i;
        } else {
            // сервер сам проверит порядок и уровни и пришлёт обновлённое меню
            ClientPlayNetworking.send(ModNetworking.UNLOCK_ABILITY, buf);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AbilityBooksClient.MENU_KEY != null && AbilityBooksClient.MENU_KEY.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
