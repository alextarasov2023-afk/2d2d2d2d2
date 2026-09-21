package org.alexdlc.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.event.events.input.KeyboardInputEvent;
import org.alexdlc.event.events.input.MouseInputEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.InputBindSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.pve.AutomationResource;
import org.alexdlc.pve.PveAutomationCoordinator;
import org.alexdlc.utils.inventory.InventorySwap;
import org.alexdlc.utils.text.ChatUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class ServerHelperFeature extends Feature {
    public final ModeSetting server = register(new ModeSetting(
            "Server",
            "FunTime",
            "FunTime",
            "HolyWorld"
    ));

    public final InputBindSetting ftDisorientation = register(funTimeBind("Дезориентация"));
    public final InputBindSetting ftSheerDust = register(funTimeBind("Явная пыль"));
    public final InputBindSetting ftGodsAura = register(funTimeBind("Божья аура"));
    public final InputBindSetting ftFreezeSnowball = register(funTimeBind("Снежок заморозка"));
    public final InputBindSetting ftFieryTornado = register(funTimeBind("Огненный смерч"));
    public final InputBindSetting ftStratum = register(funTimeBind("Пласт"));
    public final InputBindSetting ftTrap = register(funTimeBind("Трапка"));

    public final InputBindSetting ftStrengthPotion = register(funTimeBind("Зелье силы"));
    public final InputBindSetting ftInvisibilityPotion = register(funTimeBind("Зелье невидимости"));
    public final InputBindSetting ftSpeedPotion = register(funTimeBind("Зелье скорости"));
    public final InputBindSetting ftLeapingPotion = register(funTimeBind("Зелье прыгучести"));
    public final InputBindSetting ftRegenerationPotion = register(funTimeBind("Зелье регенерации"));
    public final InputBindSetting ftNightVisionPotion = register(funTimeBind("Зелье ночного зрения"));
    public final InputBindSetting ftFireResistancePotion = register(funTimeBind("Зелье огнестойкости"));
    public final InputBindSetting ftWaterBreathingPotion = register(funTimeBind("Зелье водного дыхания"));
    public final InputBindSetting ftPopper = register(funTimeBind("Хлопушка"));
    public final InputBindSetting ftHolyWater = register(funTimeBind("Святая вода"));
    public final InputBindSetting ftRagePotion = register(funTimeBind("Зелье Гнева"));
    public final InputBindSetting ftPaladinPotion = register(funTimeBind("Зелье Палладина"));
    public final InputBindSetting ftAssassinPotion = register(funTimeBind("Зелье Ассасина"));
    public final InputBindSetting ftRadiationPotion = register(funTimeBind("Зелье Радиации"));
    public final InputBindSetting ftDrowsinessPotion = register(funTimeBind("Снотворное"));

    public final InputBindSetting hwWinnerPotion = register(holyWorldBind("Зелье победителя"));
    public final InputBindSetting hwStrengthPotion = register(holyWorldBind("Улучшенное зелье силы"));
    public final InputBindSetting hwSpeedPotion = register(holyWorldBind("Улучшенное зелье скорости"));
    public final InputBindSetting hwStun = register(holyWorldBind("Стан"));
    public final InputBindSetting hwExplosiveTrap = register(holyWorldBind("Взрывная трапка"));
    public final InputBindSetting hwTrap = register(holyWorldBind("Трапка"));
    public final InputBindSetting hwTntCannon = register(holyWorldBind("Тнт-Пушка"));
    public final InputBindSetting hwDynamite = register(holyWorldBind("Динамит"));
    public final InputBindSetting hwDynamiteA = register(holyWorldBind("Динамит A"));
    public final InputBindSetting hwDynamiteB = register(holyWorldBind("Динамит B"));
    public final InputBindSetting hwC4 = register(holyWorldBind("C4"));
    public final InputBindSetting hwBlastWave = register(holyWorldBind("Разрывная волна"));
    public final InputBindSetting hwDynamiteB2 = register(holyWorldBind("Динамит Б2"));
    public final InputBindSetting hwStealer = register(holyWorldBind("Стиллер"));
    public final InputBindSetting hwReliableStealer = register(holyWorldBind("Надёжный стиллер"));
    public final InputBindSetting hwIceWave = register(holyWorldBind("Ледяная волна"));
    public final InputBindSetting hwSpecialCompass = register(holyWorldBind("Особый компас"));
    public final InputBindSetting hwEnchantedApple = register(holyWorldBind("Зачарованное яблоко"));
    public final InputBindSetting hwUniversalKey = register(holyWorldBind("Универсальный ключ"));
    public final InputBindSetting hwVexEgg = register(holyWorldBind("Vex Spawn Egg"));
    public final InputBindSetting hwGoldenSpawner = register(holyWorldBind("Золотой спавнер"));
    public final InputBindSetting hwUniqueClaim = register(holyWorldBind("Уникальный приват"));
    public final InputBindSetting hwOpenBackpack = register(holyWorldBind("Открыть рюкзак"));

    private static final List<DonItems.DonItem> BACKPACK_PRIORITY = List.of(
            DonItems.HolyWorld.INFINITY_BACKPACK,
            DonItems.HolyWorld.BACKPACK_IV,
            DonItems.HolyWorld.BACKPACK_III,
            DonItems.HolyWorld.BACKPACK_II,
            DonItems.HolyWorld.BACKPACK_I
    );

    private final List<QuickAction> funTimeActions = List.of(
            action(ftDisorientation, DonItems.FunTime.DISORIENTATION),
            action(ftSheerDust, DonItems.FunTime.SHEER_DUST),
            action(ftGodsAura, DonItems.FunTime.GODS_AURA),
            action(ftFreezeSnowball, DonItems.FunTime.FREEZE_SNOWBALL),
            action(ftFieryTornado, DonItems.FunTime.FIERY_TORNADO),
            action(ftStratum, DonItems.FunTime.STRATUM),
            action(ftTrap, DonItems.FunTime.TRAP),
            action(ftStrengthPotion, DonItems.FunTime.ENHANCED_STRENGTH_POTION),
            action(ftInvisibilityPotion, DonItems.FunTime.ENHANCED_INVISIBILITY_POTION),
            action(ftSpeedPotion, DonItems.FunTime.ENHANCED_SPEED_POTION),
            action(ftLeapingPotion, DonItems.FunTime.ENHANCED_LEAPING_POTION),
            action(ftRegenerationPotion, DonItems.FunTime.ENHANCED_REGENERATION_POTION),
            action(ftNightVisionPotion, DonItems.FunTime.ENHANCED_NIGHT_VISION_POTION),
            action(ftFireResistancePotion, DonItems.FunTime.ENHANCED_FIRE_RESISTANCE_POTION),
            action(ftWaterBreathingPotion, DonItems.FunTime.ENHANCED_WATER_BREATHING_POTION),
            action(ftPopper, DonItems.FunTime.POPPER),
            action(ftHolyWater, DonItems.FunTime.HOLY_WATER),
            action(ftRagePotion, DonItems.FunTime.RAGE_POTION),
            action(ftPaladinPotion, DonItems.FunTime.PALADIN_POTION),
            action(ftAssassinPotion, DonItems.FunTime.ASSASSIN_POTION),
            action(ftRadiationPotion, DonItems.FunTime.RADIATION_POTION),
            action(ftDrowsinessPotion, DonItems.FunTime.DROWSINESS_POTION)
    );

    private final List<QuickAction> holyWorldActions = List.of(
            action(hwWinnerPotion, DonItems.HolyWorld.WINNER_POTION),
            action(hwStrengthPotion, DonItems.HolyWorld.ENHANCED_STRENGTH_POTION),
            action(hwSpeedPotion, DonItems.HolyWorld.ENHANCED_SPEED_POTION),
            action(hwStun, DonItems.HolyWorld.STAN),
            action(hwExplosiveTrap, DonItems.HolyWorld.EXPLOSIVE_TRAP),
            action(hwTrap, DonItems.HolyWorld.TRAP),
            action(hwTntCannon, DonItems.HolyWorld.TNT_CANNON),
            action(hwDynamite, DonItems.HolyWorld.DYNAMITE),
            action(hwDynamiteA, DonItems.HolyWorld.DYNAMITE_A),
            action(hwDynamiteB, DonItems.HolyWorld.DYNAMITE_B),
            action(hwC4, DonItems.HolyWorld.C4),
            action(hwBlastWave, DonItems.HolyWorld.BLAST_WAVE),
            action(hwDynamiteB2, DonItems.HolyWorld.DYNAMITE_B2),
            action(hwStealer, DonItems.HolyWorld.STEALER),
            action(hwReliableStealer, DonItems.HolyWorld.RELIABLE_STEALER),
            action(hwIceWave, DonItems.HolyWorld.ICE_WAVE),
            action(hwSpecialCompass, DonItems.HolyWorld.SPECIAL_COMPASS),
            action(hwEnchantedApple, DonItems.HolyWorld.ENCHANTED_APPLE),
            action(hwUniversalKey, DonItems.HolyWorld.UNIVERSAL_KEY),
            action(hwVexEgg, DonItems.HolyWorld.VEX_SPAWN_EGG),
            action(hwGoldenSpawner, DonItems.HolyWorld.GOLDEN_SPAWNER),
            action(hwUniqueClaim, DonItems.HolyWorld.UNIQUE_CLAIM)
    );

    private final Map<DonItems.DonItem, ItemStack> quickUseIconCache = new HashMap<>();
    private PendingAction pendingAction;

    public ServerHelperFeature() {
        super("ServerHelper", "Quick FunTime/HolyWorld DonItems actions", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    protected void onDisable() {
        this.pendingAction = null;
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && queueMatching(bind -> bind.matches(event.getKey()))) {
            event.cancel();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && queueMatching(bind -> bind.matchesMouse(event.getButton()))) {
            event.cancel();
        }
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (this.pendingAction == null
                || InventorySwap.isBusy()
                || PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)) {
            return;
        }

        PendingAction action = this.pendingAction;
        this.pendingAction = null;
        Minecraft client = event.getClient();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        FoundItem found = findItem(player, action.candidates);
        if (found == null) {
            ChatUtil.error("Не найдено: " + action.label);
            return;
        }

        boolean waitForUse = requiresHeldUse(found.stack);
        if (found.selected) {
            InventorySwap.useSelected(waitForUse);
        } else {
            InventorySwap.useFromSlot(found.containerSlot, waitForUse, true);
        }
    }

    public List<QuickUseEntry> quickUseEntries(LocalPlayer player) {
        if (!isEnabled() || player == null) {
            return List.of();
        }

        DonItems.Server selected = selectedServer().orElse(DonItems.Server.FUNTIME);
        Map<DonItems.DonItem, ItemSummary> inventory = indexQuickUseInventory(player, selected);
        List<QuickUseEntry> entries = new ArrayList<>();
        List<QuickAction> actions = selected == DonItems.Server.FUNTIME
                ? this.funTimeActions
                : this.holyWorldActions;

        for (QuickAction action : actions) {
            if (action.bind.isBound()) {
                entries.add(quickUseEntry(action.bind, List.of(action.item), inventory));
            }
        }
        if (selected == DonItems.Server.HOLYWORLD && this.hwOpenBackpack.isBound()) {
            entries.add(quickUseEntry(this.hwOpenBackpack, BACKPACK_PRIORITY, inventory));
        }
        return List.copyOf(entries);
    }

    private Map<DonItems.DonItem, ItemSummary> indexQuickUseInventory(
            LocalPlayer player,
            DonItems.Server selected
    ) {
        Map<DonItems.DonItem, ItemSummary> inventory = new HashMap<>();
        for (int slot = InventoryMenu.INV_SLOT_START; slot < InventoryMenu.USE_ROW_SLOT_END; slot++) {
            ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            DonItems.find(stack, selected).ifPresent(item -> {
                ItemSummary summary = inventory.computeIfAbsent(item, ignored -> new ItemSummary());
                summary.add(stack);
                ItemStack icon = stack.copy();
                icon.setCount(1);
                this.quickUseIconCache.put(item, icon);
            });
        }
        return inventory;
    }

    private QuickUseEntry quickUseEntry(
            InputBindSetting bind,
            List<DonItems.DonItem> candidates,
            Map<DonItems.DonItem, ItemSummary> inventory
    ) {
        int count = 0;
        ItemStack icon = ItemStack.EMPTY;
        for (DonItems.DonItem candidate : candidates) {
            ItemSummary summary = inventory.get(candidate);
            if (summary != null) {
                count += summary.count;
                if (icon.isEmpty()) {
                    icon = summary.icon;
                }
            }
        }
        if (icon.isEmpty()) {
            for (DonItems.DonItem candidate : candidates) {
                ItemStack cached = this.quickUseIconCache.get(candidate);
                if (cached != null && !cached.isEmpty()) {
                    icon = cached;
                    break;
                }
            }
        }
        if (icon.isEmpty() && !candidates.isEmpty()) {
            icon = DonItems.displayStack(candidates.getFirst());
        }
        return new QuickUseEntry(icon, count, bind.getDisplayValue());
    }

    private boolean queueMatching(Predicate<InputBindSetting> matcher) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || client.gameMode == null
                || client.gui.screen() != null
                || this.pendingAction != null
                || InventorySwap.isBusy()
                || PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)) {
            return false;
        }

        Optional<DonItems.Server> selected = selectedServer();
        if (selected.isEmpty()) {
            return false;
        }

        List<QuickAction> actions = selected.get() == DonItems.Server.FUNTIME
                ? this.funTimeActions
                : this.holyWorldActions;
        for (QuickAction action : actions) {
            if (matcher.test(action.bind)) {
                this.pendingAction = new PendingAction(
                        List.of(action.item),
                        action.item.displayName()
                );
                return true;
            }
        }

        if (selected.get() == DonItems.Server.HOLYWORLD && matcher.test(this.hwOpenBackpack)) {
            this.pendingAction = new PendingAction(BACKPACK_PRIORITY, "рюкзак");
            return true;
        }
        return false;
    }

    private FoundItem findItem(LocalPlayer player, List<DonItems.DonItem> candidates) {
        int selectedHotbar = player.getInventory().getSelectedSlot();
        for (DonItems.DonItem candidate : candidates) {
            ItemStack selected = player.getMainHandItem();
            if (candidate.matches(selected)) {
                return new FoundItem(-1, selected.copy(), true);
            }

            for (int slot = InventoryMenu.USE_ROW_SLOT_START;
                 slot < InventoryMenu.USE_ROW_SLOT_END;
                 slot++) {
                if (slot - InventoryMenu.USE_ROW_SLOT_START == selectedHotbar) {
                    continue;
                }
                ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
                if (candidate.matches(stack)) {
                    return new FoundItem(slot, stack.copy(), false);
                }
            }

            for (int slot = InventoryMenu.INV_SLOT_START;
                 slot < InventoryMenu.USE_ROW_SLOT_START;
                 slot++) {
                ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
                if (candidate.matches(stack)) {
                    return new FoundItem(slot, stack.copy(), false);
                }
            }
        }
        return null;
    }

    private boolean requiresHeldUse(ItemStack stack) {
        ItemUseAnimation animation = stack.getUseAnimation();
        return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK;
    }

    private Optional<DonItems.Server> selectedServer() {
        return Optional.of(this.server.is("HolyWorld")
                ? DonItems.Server.HOLYWORLD
                : DonItems.Server.FUNTIME);
    }

    private boolean showFunTimeSettings() {
        return this.server.is("FunTime");
    }

    private boolean showHolyWorldSettings() {
        return this.server.is("HolyWorld");
    }

    private InputBindSetting funTimeBind(String item) {
        return new InputBindSetting(
                item,
                InputBindSetting.UNBOUND
        ).configKey("funtime." + item).visibleWhen(this::showFunTimeSettings);
    }

    private InputBindSetting holyWorldBind(String item) {
        return new InputBindSetting(
                item,
                InputBindSetting.UNBOUND
        ).configKey("holyworld." + item).visibleWhen(this::showHolyWorldSettings);
    }

    private QuickAction action(InputBindSetting bind, DonItems.DonItem item) {
        return new QuickAction(bind, item);
    }

    public record QuickUseEntry(ItemStack icon, int count, String bindLabel) {
        public QuickUseEntry {
            icon = icon == null ? ItemStack.EMPTY : icon.copy();
            count = Math.max(0, count);
            bindLabel = bindLabel == null ? "" : bindLabel;
        }

        @Override
        public ItemStack icon() {
            return this.icon.copy();
        }
    }

    private static final class ItemSummary {
        private ItemStack icon = ItemStack.EMPTY;
        private int count;

        private void add(ItemStack stack) {
            this.count += stack.getCount();
            if (this.icon.isEmpty()) {
                this.icon = stack.copy();
                this.icon.setCount(1);
            }
        }
    }

    private record QuickAction(InputBindSetting bind, DonItems.DonItem item) {
    }

    private record PendingAction(List<DonItems.DonItem> candidates, String label) {
        private PendingAction {
            candidates = List.copyOf(candidates);
        }
    }

    private record FoundItem(int containerSlot, ItemStack stack, boolean selected) {
    }
}
