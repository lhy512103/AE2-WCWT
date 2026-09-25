package com.lhy.wcwt.client;

import com.lhy.wcwt.WcwtMod;
import com.lhy.wcwt.config.WcwtClientConfig;
import com.lhy.wcwt.helpers.WcwtToolkitAccess;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState;
import com.lhy.wcwt.helpers.WcwtToolkitHotbarState.Bar;
import com.lhy.wcwt.network.WcwtToolkitHotbarDropPacket;
import com.lhy.wcwt.network.WcwtToolkitHotbarSelectionPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * HUD and input for the 27-cell ring: left toolkit bar, vanilla hotbar, right toolkit bar.
 *
 * <p>Scroll and the left/right keys move around the ring, number keys stay vanilla and pick a slot
 * inside the current page, and a left click on a toolkit cell selects it. Shift+scroll and scrolls
 * another mod already consumed are left alone, so tools that cycle modes on sneak-scroll keep working.
 */
@EventBusSubscriber(modid = WcwtMod.MOD_ID, value = Dist.CLIENT)
public final class WcwtToolkitHud {
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation SELECTION = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final Bar[] CYCLE_ORDER = {Bar.LEFT, Bar.CENTER, Bar.RIGHT};
    private static final int CELLS = CYCLE_ORDER.length * WcwtToolkitAccess.HOTBAR_SIZE;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 22;

    private WcwtToolkitHud() {
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!isHudVisible(minecraft)) {
            if (player != null && WcwtToolkitHotbarState.isToolkitSelected(player)
                    && (!WcwtClientConfig.showToolkitHotbars() || !WcwtToolkitHotbarState.hasToolkitCard(player))) {
                setSelection(player, Bar.CENTER, player.getInventory().selected);
            }
            return;
        }

        while (WcwtKeybindings.TOOLKIT_BAR_LEFT.consumeClick()) {
            cyclePage(player, -1);
        }
        while (WcwtKeybindings.TOOLKIT_BAR_RIGHT.consumeClick()) {
            cyclePage(player, 1);
        }
        if (WcwtToolkitHotbarState.isToolkitSelected(player)) {
            while (minecraft.options.keyDrop.consumeClick()) {
                PacketDistributor.sendToServer(new WcwtToolkitHotbarDropPacket(Screen.hasControlDown()));
            }
        }
    }

    public static void renderGuiLayer(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHudVisible(minecraft) || minecraft.options.hideGui) {
            return;
        }
        LocalPlayer player = minecraft.player;
        int y = graphics.guiHeight() - BAR_HEIGHT;
        int centerX = graphics.guiWidth() / 2 - 91;
        Bar bar = WcwtToolkitHotbarState.getBar(player);
        int slot = WcwtToolkitHotbarState.getSlot(player);
        renderBar(graphics, player, centerX - BAR_WIDTH, y, 0, bar == Bar.LEFT, slot, 10);
        renderBar(graphics, player, centerX + BAR_WIDTH, y, WcwtToolkitAccess.HOTBAR_SIZE, bar == Bar.RIGHT, slot, 20);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.isCanceled() || !isHudVisible(minecraft) || player.isShiftKeyDown()) {
            return;
        }
        int delta = (int) Math.signum(event.getScrollDeltaY() != 0.0
                ? event.getScrollDeltaY() : -event.getScrollDeltaX());
        if (delta == 0) {
            return;
        }
        int index = Math.floorMod(ringIndex(player) - delta, CELLS);
        setSelection(player, CYCLE_ORDER[index / WcwtToolkitAccess.HOTBAR_SIZE], index % WcwtToolkitAccess.HOTBAR_SIZE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHudVisible(minecraft) || event.getAction() != GLFW.GLFW_PRESS
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
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
        Bar bar = Bar.LEFT;
        if (slot < 0) {
            slot = slotAt(mouseX, centerX + BAR_WIDTH);
            bar = Bar.RIGHT;
        }
        if (slot < 0) {
            return;
        }
        setSelection(minecraft.player, bar, slot);
        event.setCanceled(true);
    }

    /**
     * Pick block selects an extra-bar cell that already holds the item; otherwise it returns to the
     * vanilla page first, since vanilla picks into {@code Inventory.selected} of the real hotbar.
     */
    @SubscribeEvent
    public static void onPickBlock(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!event.isPickBlock() || !isHudVisible(minecraft)) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            ItemStack picked = pickedStack(minecraft);
            for (int index = 0; !picked.isEmpty() && index < WcwtToolkitAccess.HOTBAR_SLOTS; index++) {
                if (ItemStack.isSameItemSameComponents(WcwtToolkitHotbarState.stackAt(player, index), picked)) {
                    setSelection(player, index < WcwtToolkitAccess.HOTBAR_SIZE ? Bar.LEFT : Bar.RIGHT,
                            index % WcwtToolkitAccess.HOTBAR_SIZE);
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (WcwtToolkitHotbarState.isToolkitSelected(player)) {
            setSelection(player, Bar.CENTER, player.getInventory().selected);
        }
    }

    private static ItemStack pickedStack(Minecraft minecraft) {
        if (minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        if (minecraft.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
            return state.getCloneItemStack(blockHit, minecraft.level, blockHit.getBlockPos(), minecraft.player);
        }
        if (minecraft.hitResult instanceof EntityHitResult entityHit) {
            ItemStack stack = entityHit.getEntity().getPickedResult(entityHit);
            return stack == null ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }

    private static void renderBar(GuiGraphics graphics, LocalPlayer player, int x, int y, int firstIndex,
                                  boolean selected, int selectedSlot, int seedOffset) {
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
        for (int i = 0; i < WcwtToolkitAccess.HOTBAR_SIZE; i++) {
            ItemStack stack = WcwtToolkitHotbarState.stackAt(player, firstIndex + i);
            ItemStack memory = WcwtToolkitHotbarState.memoryAt(player, firstIndex + i);
            int itemX = x + 3 + i * 20;
            int itemY = y + 3;
            if (!stack.isEmpty()) {
                graphics.renderItem(player, stack, itemX, itemY, seedOffset + i);
                graphics.renderItemDecorations(minecraft.font, stack, itemX, itemY);
            } else if (!memory.isEmpty()) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 0.38F);
                graphics.renderItem(player, memory, itemX, itemY, seedOffset + i);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private static int slotAt(int mouseX, int barX) {
        if (mouseX < barX || mouseX >= barX + BAR_WIDTH) {
            return -1;
        }
        int slot = (mouseX - barX - 1) / 20;
        return slot >= 0 && slot < WcwtToolkitAccess.HOTBAR_SIZE ? slot : -1;
    }

    private static int ringIndex(LocalPlayer player) {
        Bar bar = WcwtToolkitHotbarState.getBar(player);
        int page = 1;
        for (int i = 0; i < CYCLE_ORDER.length; i++) {
            if (CYCLE_ORDER[i] == bar) {
                page = i;
            }
        }
        return page * WcwtToolkitAccess.HOTBAR_SIZE + WcwtToolkitHotbarState.getSlot(player);
    }

    private static void cyclePage(LocalPlayer player, int direction) {
        int page = ringIndex(player) / WcwtToolkitAccess.HOTBAR_SIZE;
        Bar next = CYCLE_ORDER[Math.floorMod(page + direction, CYCLE_ORDER.length)];
        setSelection(player, next, WcwtToolkitHotbarState.getSlot(player));
    }

    private static void setSelection(LocalPlayer player, Bar bar, int slot) {
        WcwtToolkitHotbarState.setSelection(player, bar, slot);
        PacketDistributor.sendToServer(new WcwtToolkitHotbarSelectionPacket(
                WcwtToolkitHotbarState.getBar(player).ordinal(), WcwtToolkitHotbarState.getSlot(player)));
    }

    private static boolean isHudVisible(Minecraft minecraft) {
        return WcwtClientConfig.showToolkitHotbars()
                && minecraft.player != null
                && WcwtToolkitHotbarState.hasToolkitCard(minecraft.player)
                && !minecraft.player.isSpectator()
                && minecraft.screen == null
                && minecraft.getOverlay() == null;
    }
}
