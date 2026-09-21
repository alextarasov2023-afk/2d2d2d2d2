package org.alexdlc.feature.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.alexdlc.context.MinecraftContext;
import org.alexdlc.utils.inventory.InventoryUtil;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.input.KeyboardInputEvent;
import org.alexdlc.event.events.input.MouseInputEvent;
import org.alexdlc.event.events.render.Render2DEvent;
import org.alexdlc.event.events.screen.ScreenCloseEvent;
import org.alexdlc.event.events.screen.ScreenMouseButtonEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.InputBindSetting;
import org.alexdlc.feature.setting.NumberSetting;
import org.alexdlc.menu.core.MenuConfigStore;
import org.alexdlc.menu.core.MenuOverlay;
import org.alexdlc.mixin.accessor.AbstractContainerScreenAccessor;
import org.alexdlc.pve.AutomationResource;
import org.alexdlc.pve.PveAutomationCoordinator;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.inventory.InventorySwap;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.lwjgl.glfw.GLFW;

public final class AutoSwapFeature extends Feature implements MinecraftContext {
    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final int HOTBAR_TEXTURE_WIDTH = 182;
    private static final int HOTBAR_TEXTURE_HEIGHT = 22;
    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 10;
    private static final int INVENTORY_SLOTS_START = 9;
    private static final int INVENTORY_SLOTS_END = 45;
    private static final float DIRECTION_DEAD_ZONE = 10.0F;
    private static final int MAX_SLOTS = 8;
    private static final long TAP_MS = 250L;
    private static final String CONFIG_KEY_PREFIX = "autoswap.item.";

    public final InputBindSetting key = register(new InputBindSetting("Key", GLFW.GLFW_KEY_R));
    public final NumberSetting slots = register(new NumberSetting("Slots", 4.0, 2.0, 8.0, 1.0, ""));
    public final BooleanSetting strict = register(new BooleanSetting("Strict", false));

    private final Item[] assignedItems = new Item[MAX_SLOTS];
    private boolean assignedLoaded;
    private boolean spaceOpen;
    private boolean editMode;
    private long pressTimeMs;
    private int hoveredSlot = -1;
    private int pickTargetSlot = -1;

    public AutoSwapFeature() {
        super("AutoSwap", "Pick a hotbar item on screen while holding a key", FeatureCategory.PLAYER, BindSetting.UNBOUND);
    }

    @Override
    protected void onDisable() {
        closeSpace();
        this.pickTargetSlot = -1;
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (this.key.matches(event.getKey()) && handleBindAction(event.getAction())) {
            event.cancel();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (this.key.matchesMouse(event.getButton()) && handleBindAction(event.getAction())) {
            event.cancel();
            return;
        }
        if (!this.spaceOpen) {
            return;
        }
        if (this.editMode && event.getAction() == GLFW.GLFW_PRESS && this.hoveredSlot >= 0) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginAssign(this.hoveredSlot);
            } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                setAssignedItem(this.hoveredSlot, null);
            }
        }
        event.cancel();
    }

    private boolean handleBindAction(int action) {
        if (action == GLFW.GLFW_PRESS) {
            if (this.spaceOpen && this.editMode) {
                closeSpace();
                return true;
            }
            this.pressTimeMs = System.currentTimeMillis();
            return openSpace();
        }
        if (action == GLFW.GLFW_RELEASE && this.spaceOpen) {
            if (this.editMode) {
                return true;
            }

            if (System.currentTimeMillis() - this.pressTimeMs < TAP_MS) {
                this.editMode = true;
                return true;
            }
            int hovered = this.hoveredSlot;
            closeSpace();
            applyHoveredSlot(hovered);
            return true;
        }
        return action == GLFW.GLFW_REPEAT && this.spaceOpen;
    }

    @EventTarget
    public void onScreenMouseButton(ScreenMouseButtonEvent event) {
        if (this.pickTargetSlot < 0
                || event.getAction() != ScreenMouseButtonEvent.Action.CLICK
                || !(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        Slot hovered = ((AbstractContainerScreenAccessor) (Object) screen).getHoveredSlot();
        if (hovered == null || !hovered.hasItem()
                || hovered.index < INVENTORY_SLOTS_START || hovered.index >= INVENTORY_SLOTS_END) {
            return;
        }

        setAssignedItem(this.pickTargetSlot, hovered.getItem().getItem());
        this.pickTargetSlot = -1;
        event.cancel();
        mc.gui.setScreen(null);
    }

    @EventTarget
    public void onScreenClose(ScreenCloseEvent event) {
        if (event.getScreen() instanceof InventoryScreen) {
            this.pickTargetSlot = -1;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.spaceOpen) {
            return;
        }
        Minecraft mc = event.getClient();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null || MenuOverlay.isOpen()) {
            closeSpace();
            return;
        }

        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
        int count = slotCount();
        float mouseX = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float mouseY = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
        this.hoveredSlot = -1;

        int[][] offsets = slotOffsets(count);
        int centerX = extractor.guiWidth() / 2;
        int centerY = extractor.guiHeight() / 2;
        this.hoveredSlot = findHoveredSlot(offsets, count, centerX, centerY, mouseX, mouseY);
        for (int index = 0; index < count; index++) {
            int x = centerX + offsets[index][0] - SLOT_SIZE / 2;
            int y = centerY + offsets[index][1] - SLOT_SIZE / 2;
            drawSlot(extractor, player, index, x, y, this.hoveredSlot == index);
        }
    }

    private int findHoveredSlot(int[][] offsets, int count, int centerX, int centerY, float mouseX, float mouseY) {
        for (int index = 0; index < count; index++) {
            int x = centerX + offsets[index][0] - SLOT_SIZE / 2;
            int y = centerY + offsets[index][1] - SLOT_SIZE / 2;
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return index;
            }
        }

        if (this.strict.getValue() || this.editMode) {
            return -1;
        }

        float dragX = mouseX - centerX;
        float dragY = mouseY - centerY;
        if (dragX * dragX + dragY * dragY < DIRECTION_DEAD_ZONE * DIRECTION_DEAD_ZONE) {
            return -1;
        }

        int best = -1;
        double bestAlignment = -Double.MAX_VALUE;
        double dragLength = Math.sqrt(dragX * dragX + dragY * dragY);
        for (int index = 0; index < count; index++) {
            double slotLength = Math.sqrt((double) offsets[index][0] * offsets[index][0] + (double) offsets[index][1] * offsets[index][1]);
            if (slotLength < 0.001D) {
                continue;
            }
            double alignment = (dragX * offsets[index][0] + dragY * offsets[index][1]) / (dragLength * slotLength);
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                best = index;
            }
        }
        return best;
    }

    private static int[][] slotOffsets(int count) {
        int step = SLOT_SIZE + SLOT_GAP;
        return switch (count) {
            case 2 -> new int[][]{{-step, 0}, {step, 0}};
            case 3 -> ring(3, Math.round(step * 1.3F), -90.0F);
            case 4 -> new int[][]{
                    {-step, -step}, {step, -step},
                    {-step, step}, {step, step}
            };
            case 5, 6 -> ring(count, Math.round(step * 1.55F), -90.0F);
            case 7 -> ring(7, Math.round(step * 1.85F), -90.0F);
            default -> new int[][]{
                    {0, -step * 2 - 8}, {step + 4, -step - 4}, {step * 2 + 8, 0}, {step + 4, step + 4},
                    {0, step * 2 + 8}, {-step - 4, step + 4}, {-step * 2 - 8, 0}, {-step - 4, -step - 4}
            };
        };
    }

    private static int[][] ring(int count, int radius, float startAngleDeg) {
        int[][] offsets = new int[count][2];
        for (int index = 0; index < count; index++) {
            double angle = Math.toRadians(startAngleDeg + index * 360.0D / count);
            offsets[index][0] = (int) Math.round(Math.cos(angle) * radius);
            offsets[index][1] = (int) Math.round(Math.sin(angle) * radius);
        }
        return offsets;
    }

    private void drawSlot(GuiGraphicsExtractor extractor, LocalPlayer player, int slot, int x, int y, boolean hovered) {

        int half = SLOT_SIZE / 2;
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE,
                HOTBAR_TEXTURE_WIDTH, HOTBAR_TEXTURE_HEIGHT,
                0, 0, x, y, half, SLOT_SIZE);
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE,
                HOTBAR_TEXTURE_WIDTH, HOTBAR_TEXTURE_HEIGHT,
                HOTBAR_TEXTURE_WIDTH - half, 0, x + half, y, half, SLOT_SIZE);
        if (hovered) {
            int accent = Theme.getAccent();
            Render2DUtil.rect(x + 1, y + 1, SLOT_SIZE - 2, SLOT_SIZE - 2)
                    .color(ColorUtil.withAlpha(accent, 90))
                    .draw();

            Render2DUtil.flush();
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE,
                    x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 1, accent);
        }

        Item item = assignedItem(slot);
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            extractor.item(stack, x + 3, y + 3);
            extractor.itemDecorations(mc.font, stack, x + 3, y + 3);
        }
    }

    private boolean openSpace() {
        if (this.spaceOpen || !inGame() || screen() != null || MenuOverlay.isOpen() || !mc.mouseHandler.isMouseGrabbed()) {
            return false;
        }

        mc.mouseHandler.releaseMouse();
        this.spaceOpen = true;
        this.hoveredSlot = -1;
        this.pickTargetSlot = -1;
        return true;
    }

    private void applyHoveredSlot(int hovered) {
        LocalPlayer player = player();
        if (player == null
                || hovered < 0
                || hovered >= slotCount()
                || PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)) {
            return;
        }
        Item item = assignedItem(hovered);
        if (item == null) {
            beginAssign(hovered);
            return;
        }
        int containerSlot = findItemSlot(player, item);
        if (containerSlot >= 0) {

            InventorySwap.equip(containerSlot);
        }
    }

    private static int findItemSlot(LocalPlayer player, Item item) {
        return InventoryUtil.findPlayerMenuSlot(player, stack -> stack.is(item));
    }

    private void beginAssign(int slot) {
        LocalPlayer player = player();
        if (player == null) {
            return;
        }
        this.pickTargetSlot = slot;
        closeSpace();
        mc.gui.setScreen(new InventoryScreen(player));
    }

    private void closeSpace() {
        if (!this.spaceOpen) {
            return;
        }
        this.spaceOpen = false;
        this.editMode = false;
        this.hoveredSlot = -1;
        if (inGame() && screen() == null && !MenuOverlay.isOpen()) {
            mc.mouseHandler.grabMouse();
        }
    }

    private int slotCount() {
        return (int) Math.round(this.slots.getValue());
    }

    private Item assignedItem(int slot) {
        ensureAssignedLoaded();
        return slot >= 0 && slot < MAX_SLOTS ? this.assignedItems[slot] : null;
    }

    private void setAssignedItem(int slot, Item item) {
        ensureAssignedLoaded();
        if (slot < 0 || slot >= MAX_SLOTS) {
            return;
        }
        this.assignedItems[slot] = item;
        String id = item == null ? "" : BuiltInRegistries.ITEM.getKey(item).toString();
        MenuConfigStore.save(data -> data.addProperty(CONFIG_KEY_PREFIX + slot, id));
    }

    private void ensureAssignedLoaded() {
        if (this.assignedLoaded) {
            return;
        }
        this.assignedLoaded = true;
        for (int slot = 0; slot < MAX_SLOTS; slot++) {
            String id = MenuConfigStore.getString(CONFIG_KEY_PREFIX + slot, "");
            if (id.isEmpty()) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            this.assignedItems[slot] = item == Items.AIR ? null : item;
        }
    }
}
