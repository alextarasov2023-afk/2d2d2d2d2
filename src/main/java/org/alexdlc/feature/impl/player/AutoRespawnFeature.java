package org.alexdlc.feature.impl.player;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.event.events.lifecycle.WorldLeaveEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.NumberSetting;
import org.alexdlc.feature.setting.TextSetting;

public final class AutoRespawnFeature extends Feature {
    public final NumberSetting delay = register(new NumberSetting(
            "Respawn Delay", 0.0D, 0.0D, 100.0D, 1.0D, " ticks"
    ));
    public final BooleanSetting teleportHome = register(new BooleanSetting(
            "Teleport Home", false
    ));
    public final TextSetting command = register(new TextSetting(
            "Command", "/home", 128
    ).visibleWhen(this.teleportHome::getValue));

    private int deathTicks;
    private int commandDelay = -1;

    public AutoRespawnFeature() {
        super("AutoRespawn", "Respawns automatically after death", FeatureCategory.PLAYER, BindSetting.UNBOUND);
    }

    @Override
    protected void onDisable() {
        reset();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        LocalPlayer player = event.getClient().player;
        if (player == null) {
            reset();
            return;
        }

        if (event.getClient().gui.screen() instanceof DeathScreen) {
            if (this.deathTicks++ <= this.delay.getValue().intValue()) {
                return;
            }
            player.respawn();
            event.getClient().gui.setScreen(null);
            this.deathTicks = 0;
            this.commandDelay = this.teleportHome.getValue() ? 1 : -1;
            return;
        }

        this.deathTicks = 0;
        if (this.commandDelay > 0) {
            this.commandDelay--;
            return;
        }
        if (this.commandDelay == 0) {
            this.commandDelay = -1;
            String value = this.command.getValue().trim();
            if (value.startsWith("/")) {
                value = value.substring(1);
            }
            if (!value.isBlank()) {
                player.connection.sendCommand(value);
            }
        }
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {
        reset();
    }

    private void reset() {
        this.deathTicks = 0;
        this.commandDelay = -1;
    }
}
