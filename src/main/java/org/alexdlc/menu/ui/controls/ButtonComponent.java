package org.alexdlc.menu.ui.controls;

import org.alexdlc.menu.ui.Component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.alexdlc.menu.i18n.MenuText;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.math.Animation;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.TextAlign;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;

public final class ButtonComponent extends Component {
    private final String label;
    private final Runnable action;
    private final Animation pressAnimation = new Animation(
            480L, Animation.Easing.EASE_OUT_QUAD);
    private float alpha = 1.0F;
    private int mouseX;
    private int mouseY;

    public ButtonComponent(String label, Runnable action) {
        this.label = label;
        this.action = action == null ? () -> {
        } : action;
    }

    public ButtonComponent place(
            Component owner,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY
    ) {
        attach(owner, owner.sx(x), owner.sy(y), owner.px(width), owner.px(height));
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        return this;
    }

    public ButtonComponent alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    public boolean handleClick(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        this.pressAnimation.animate(
                1.0F, 0.0F, 480L, Animation.Easing.EASE_OUT_QUAD);
        this.action.run();
        return true;
    }

    @Override
    public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
        float pressed = this.pressAnimation.getValue();
        boolean hovered = contains(this.mouseX, this.mouseY);
        int baseBackground = hovered
                ? Theme.Colors.BACKGROUND_SURFACE_S
                : Theme.Colors.BACKGROUND_SURFACE_M;
        Render2DUtil.rect(x(), y(), width(), height())
                .color(ColorUtil.multiplyAlpha(
                        ColorUtil.lerp(
                                baseBackground,
                                Theme.getAccent(),
                                pressed
                        ),
                        this.alpha
                ))
                .radius(px(Theme.Sizes.INPUT_RADIUS))
                .border(
                        Math.max(0.5F, px(0.5F)),
                        ColorUtil.multiplyAlpha(
                                Theme.Colors.OUTLINES_SMALL,
                                this.alpha
                        )
                )
                .draw();

        float textSize = px(12.0F);
        Render2DUtil.text(
                        x() + width() / 2.0F,
                        UiFonts.sfProDisplay().centeredTextY(
                                y() + height() / 2.0F, textSize),
                        textSize,
                        MenuText.ui(this.label)
                )
                .style(UiFontStyle.MEDIUM)
                .align(TextAlign.CENTER)
                .color(ColorUtil.multiplyAlpha(
                        ColorUtil.lerp(
                                Theme.Colors.TEXT_GHOST,
                                Theme.Colors.TEXT_TITLE,
                                pressed
                        ),
                        this.alpha
                ))
                .draw();
    }
}
