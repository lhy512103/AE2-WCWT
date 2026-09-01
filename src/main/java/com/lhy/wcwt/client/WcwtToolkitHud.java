package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.lhy.wcwt.network.WcwtToolkitHotbarActionPacket;
import com.lhy.wcwt.network.WcwtToolkitHotbarSelectionPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = WcwtMod.MOD_ID, value = Dist.CLIENT)
public final class WcwtToolkitHud {
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation SELECTION = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final WcwtToolkitHotbarState.Bar[] CYCLE_ORDER = {
            WcwtToolkitHotbarState.Bar.LEFT,
            WcwtToolkitHotbarState.Bar.CENTER,
            WcwtToolkitHotbarState.Bar.RIGHT
    };
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 22;

    private WcwtToolkitHud() {
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHudVisible(minecraft)) {
            if (minecraft.player != null
                    && !WcwtClientConfig.showToolkitHotbars()
                    && WcwtToolkitHotbarState.isToolkitSelected(minecraft.player)) {
                setSelection(WcwtToolkitHotbarState.Bar.CENTER, minecraft.player.getInventory().selected);
            }
            return;
        }

        while (WcwtKeybindings.TOOLKIT_BAR_LEFT.consumeClick()) {
            cycle(-1);
        }
        while (WcwtKeybindings.TOOLKIT_BAR_RIGHT.consumeClick()) {
            cycle(1);
        }

        if (!WcwtToolkitHotbarState.isToolkitSelected(minecraft.player)) {
            return;
        }
        for (int i = 0; i < minecraft.options.keyHotbarSlots.length; i++) {
            while (minecraft.options.keyHotbarSlots[i].consumeClick()) {
                setSelection(WcwtToolkitHotbarState.getBar(minecraft.player), i);
            }
        }
        while (minecraft.options.keyDrop.consumeClick()) {
            int index = WcwtToolkitHotbarState.toolkitIndex(minecraft.player);
            PacketDistributor.sendToServer(new WcwtToolkitHotbarActionPacket(
                    WcwtToolkitHotbarActionPacket.DROP, index, net.minecraft.client.gui.screens.Screen.hasControlDown()));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHudVisible(minecraft)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int y = graphics.guiHeight() - BAR_HEIGHT;
        int centerX = graphics.guiWidth() / 2 - 91;
        ItemStack[] toolkit = WcwtToolkitHotbarState.getClientSnapshot(minecraft.player);
        ItemStack[] memory = WcwtToolkitHotbarState.getClientMemorySnapshot(minecraft.player);
        renderBar(graphics, centerX - BAR_WIDTH, y, java.util.Arrays.asList(toolkit).subList(0, 9),
                java.util.Arrays.asList(memory).subList(0, 9),
                WcwtToolkitHotbarState.getBar(minecraft.player) == WcwtToolkitHotbarState.Bar.LEFT,
                WcwtToolkitHotbarState.getSlot(minecraft.player), 10);
        renderBar(graphics, centerX + BAR_WIDTH, y, java.util.Arrays.asList(toolkit).subList(9, 18),
                java.util.Arrays.asList(memory).subList(9, 18),
                WcwtToolkitHotbarState.getBar(minecraft.player) == WcwtToolkitHotbarState.Bar.RIGHT,
                WcwtToolkitHotbarState.getSlot(minecraft.player), 20);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHudVisible(minecraft) || minecraft.player.isSpectator()) {
            return;
        }
        int delta = (int) Math.signum(event.getScrollDeltaY() != 0.0
                ? event.getScrollDeltaY() : -event.getScrollDeltaX());
        if (delta == 0) {
            return;
        }
        var player = minecraft.player;
        int barIndex = 1;
        for (int i = 0; i < CYCLE_ORDER.length; i++) {
            if (CYCLE_ORDER[i] == WcwtToolkitHotbarState.getBar(player)) {
                barIndex = i;
                break;
            }
        }
        int index = Math.floorMod(barIndex * 9 + WcwtToolkitHotbarState.getSlot(player) - delta, 27);
        setSelection(CYCLE_ORDER[index / 9], index % 9);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHudVisible(minecraft) || event.getAction() != GLFW.GLFW_PRESS
                || (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            return;
        }
        var window = minecraft.getWindow();
        int mouseX = Mth.floor(minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
        int mouseY = Mth.floor(minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        int y = window.getGuiScaledHeight() - BAR_HEIGHT;
        if (mouseY < y || mouseY >= y + BAR_HEIGHT) {
            return;
        }
        int centerX = window.getGuiScaledWidth() / 2 - 91;
        int slot = slotAt(mouseX, centerX - BAR_WIDTH);
        int toolkitIndex = slot;
        WcwtToolkitHotbarState.Bar bar = WcwtToolkitHotbarState.Bar.LEFT;
        if (slot < 0) {
            slot = slotAt(mouseX, centerX + BAR_WIDTH);
            toolkitIndex = slot < 0 ? -1 : 9 + slot;
            bar = WcwtToolkitHotbarState.Bar.RIGHT;
        }
        if (toolkitIndex < 0) {
            return;
        }
        setSelection(bar, slot);
        PacketDistributor.sendToServer(new WcwtToolkitHotbarActionPacket(
                WcwtToolkitHotbarActionPacket.CLICK, toolkitIndex,
                event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT));
        event.setCanceled(true);
    }

    private static void renderBar(GuiGraphics graphics, int x, int y, java.util.List<ItemStack> stacks,
                                   java.util.List<ItemStack> memories, boolean selected, int selectedSlot,
                                   int seedOffset) {
        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, -90.0F);
        graphics.blitSprite(HOTBAR, x, y, BAR_WIDTH, BAR_HEIGHT);
        if (selected) {
            graphics.blitSprite(SELECTION, x - 1 + selectedSlot * 20, y - 1, 24, 23);
        }
        graphics.pose().popPose();
        RenderSystem.disableBlend();
        Minecraft minecraft = Minecraft.getInstance();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = stacks.get(i);
            ItemStack memory = i < memories.size() ? memories.get(i) : ItemStack.EMPTY;
            int itemX = x + 3 + i * 20;
            int itemY = y + 3;
            if (!stack.isEmpty()) {
                graphics.renderItem(minecraft.player, stack, itemX, itemY, seedOffset + i);
                graphics.renderItemDecorations(minecraft.font, stack, itemX, itemY);
            } else if (!memory.isEmpty()) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 0.38F);
                graphics.renderItem(minecraft.player, memory, itemX, itemY, seedOffset + i);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private static int slotAt(int mouseX, int barX) {
        if (mouseX < barX || mouseX >= barX + BAR_WIDTH) {
            return -1;
        }
        int slot = (mouseX - barX - 1) / 20;
        return slot >= 0 && slot < 9 ? slot : -1;
    }

    private static void cycle(int direction) {
        Minecraft minecraft = Minecraft.getInstance();
        WcwtToolkitHotbarState.Bar current = WcwtToolkitHotbarState.getBar(minecraft.player);
        int index = 1;
        for (int i = 0; i < CYCLE_ORDER.length; i++) {
            if (CYCLE_ORDER[i] == current) {
                index = i;
                break;
            }
        }
        WcwtToolkitHotbarState.Bar next = CYCLE_ORDER[Math.floorMod(index + direction, CYCLE_ORDER.length)];
        int slot = current == WcwtToolkitHotbarState.Bar.CENTER
                ? minecraft.player.getInventory().selected : WcwtToolkitHotbarState.getSlot(minecraft.player);
        setSelection(next, slot);
    }

    private static void setSelection(WcwtToolkitHotbarState.Bar bar, int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (bar == WcwtToolkitHotbarState.Bar.CENTER) {
            minecraft.player.getInventory().selected = slot;
        }
        WcwtToolkitHotbarState.setSelection(minecraft.player, bar, slot);
        PacketDistributor.sendToServer(new WcwtToolkitHotbarSelectionPacket(bar.ordinal(), slot));
    }

    private static boolean isHudVisible(Minecraft minecraft) {
        return WcwtClientConfig.showToolkitHotbars()
                && minecraft.player != null
                && !minecraft.player.isSpectator()
                && minecraft.screen == null
                && minecraft.getOverlay() == null;
    }
}
