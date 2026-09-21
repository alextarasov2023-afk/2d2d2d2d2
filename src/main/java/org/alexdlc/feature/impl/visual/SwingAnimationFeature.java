package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class SwingAnimationFeature extends Feature {
    public enum Style {
        SLICE,
        SPIRAL,
        THRUST,
        SPEAR
    }

    public final ModeSetting mode = register(new ModeSetting("Mode", "Slice", "Slice", "Spiral", "Thrust", "Spear"));
    public final NumberSetting speed = register(new NumberSetting("Speed", 6.0, 1.0, 20.0, 1.0, ""));

    public SwingAnimationFeature() {
        super("SwingAnimation", "Custom hand swing animation", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public Style style() {
        return Style.valueOf(mode.getValue().toUpperCase(java.util.Locale.ROOT));
    }

    public int swingDurationTicks() {
        return speed.getValue().intValue();
    }
}
