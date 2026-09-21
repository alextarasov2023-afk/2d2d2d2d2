package org.alexdlc;

import net.fabricmc.api.ModInitializer;
import org.alexdlc.command.CommandManager;
import org.alexdlc.event.EventManager;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.lifecycle.ClientStartEvent;
import org.alexdlc.event.events.lifecycle.WorldLeaveEvent;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.utils.FriendManager;
import org.alexdlc.hud.NotificationsElement;
import org.alexdlc.menu.MenuKeyHandler;
import org.alexdlc.menu.MenuOverlayRenderHandler;
import org.alexdlc.menu.i18n.UiLanguage;
import org.alexdlc.pve.PveAutomationCoordinator;
import org.alexdlc.pve.PvpStateTracker;
import org.alexdlc.utils.AccountSwitcher;
import org.alexdlc.utils.combat.ServerSprintTracker;
import org.alexdlc.utils.combat.ServerTickSync;
import org.alexdlc.utils.ScreenNbtParser;
import org.alexdlc.utils.inventory.DropAllInventoryController;
import org.alexdlc.utils.inventory.InventorySwap;
import org.alexdlc.utils.render.EntityEspStateCache;
import org.alexdlc.utils.render.Textures;
import org.alexdlc.utils.render.gui.GuiTexture;
import org.alexdlc.utils.render.world.WorldEffects;

public class AlexDlc implements ModInitializer {
    private final MenuKeyHandler menuKeyHandler = new MenuKeyHandler();
    private final MenuOverlayRenderHandler menuOverlayRenderHandler = new MenuOverlayRenderHandler();

    @Override
    public void onInitialize() {
        UiLanguage.loadSaved();
        FriendManager.INSTANCE.initialize();
        FeatureManager.INSTANCE.initialize();
        CommandManager.INSTANCE.initialize();
        WorldEffects.bootstrap();
        EventManager.subscribe(FeatureManager.INSTANCE);
        EventManager.subscribe(CommandManager.INSTANCE);
        EventManager.subscribe(FriendManager.INSTANCE);
        EventManager.subscribe(new NotificationsElement.ToggleListener());
        EventManager.subscribe(menuKeyHandler);
        EventManager.subscribe(menuOverlayRenderHandler);
        EventManager.subscribe(InventorySwap.INSTANCE);
        EventManager.subscribe(DropAllInventoryController.INSTANCE);
        EventManager.subscribe(ServerTickSync.INSTANCE);
        EventManager.subscribe(ServerSprintTracker.INSTANCE);
        EventManager.subscribe(PveAutomationCoordinator.INSTANCE);
        EventManager.subscribe(PvpStateTracker.INSTANCE);
        EventManager.subscribe(ScreenNbtParser.INSTANCE);
        EventManager.subscribe(this);
    }

    @EventTarget
    public void onClientStart(ClientStartEvent event) {
        AccountSwitcher.applyStartupAccount();

        GuiTexture.prewarm(Textures.all());
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {
        EntityEspStateCache.clear();
    }
}
