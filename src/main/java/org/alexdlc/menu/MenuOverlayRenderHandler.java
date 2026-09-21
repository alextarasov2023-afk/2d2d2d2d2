package org.alexdlc.menu;

import net.minecraft.client.Minecraft;
import org.alexdlc.context.RenderContext;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.render.FinalGuiRenderEvent;
import org.alexdlc.menu.core.MenuOverlay;
import org.alexdlc.menu.core.MenuAppearance;
import org.alexdlc.utils.render.gui.Render2DUtil;

public final class MenuOverlayRenderHandler {
    @EventTarget
    public void onFinalGuiRender(FinalGuiRenderEvent event) {
        Minecraft minecraft = event.getClient();
        RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
        try {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            Render2DUtil.beginFrame();
            Render2DUtil.setBackdropBlurScale(MenuAppearance.glassBlurScale());
            MenuOverlay.render(minecraft, event.getGuiGraphicsExtractor(), screenWidth, screenHeight);
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }
}
