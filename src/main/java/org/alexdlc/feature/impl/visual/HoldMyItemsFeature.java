package org.alexdlc.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import org.alexdlc.event.EventManager;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.lifecycle.ClientStartEvent;
import org.alexdlc.event.events.lifecycle.ResourceReloadEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.menu.core.MenuOverlay;

public final class HoldMyItemsFeature extends Feature {
    private static final String RESOURCE_PACK_ID = "holdmyitems:pack_test";
    private static final ReloadCompletionListener RELOAD_COMPLETION = new ReloadCompletionListener();

    public HoldMyItemsFeature() {
        super("HoldMyItems", "Animated hands and held items in first person", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public static boolean isActive() {
        return FeatureManager.INSTANCE.getEnabled(HoldMyItemsFeature.class) != null;
    }

    @Override
    protected void onEnable() {
        syncResourcePack(true);
    }

    @Override
    protected void onDisable() {
        syncResourcePack(false);
    }

    @EventTarget
    public void onClientStart(ClientStartEvent event) {
        syncResourcePack(true);
    }

    private static void syncResourcePack(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            PackRepository repository = client.getResourcePackRepository();
            repository.reload();
            boolean selected = repository.getSelectedIds().contains(RESOURCE_PACK_ID);
            boolean changed = enabled
                    ? !selected && repository.addPack(RESOURCE_PACK_ID)
                    : selected && repository.removePack(RESOURCE_PACK_ID);
            if (changed) {
                boolean restoreMenu = MenuOverlay.suspendForReload(client);
                if (restoreMenu) {
                    RELOAD_COMPLETION.arm();
                }
                try {
                    client.options.updateResourcePacks(repository);
                } catch (RuntimeException exception) {
                    if (restoreMenu) {
                        RELOAD_COMPLETION.complete(client);
                    }
                    throw exception;
                }
            }
        });
    }

    private static final class ReloadCompletionListener {
        private boolean armed;

        private void arm() {
            if (this.armed) {
                return;
            }
            this.armed = true;
            EventManager.subscribe(this);
        }

        private void complete(Minecraft client) {
            if (!this.armed) {
                return;
            }
            this.armed = false;
            EventManager.unsubscribe(this);
            MenuOverlay.resumeAfterReload(client);
        }

        @EventTarget
        public void onResourceReload(ResourceReloadEvent event) {
            complete(event.getClient());
        }
    }
}
