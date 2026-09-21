package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class ParticleRainFeature extends Feature {
    public static final String COLOR_CUSTOM = "Custom";
    public static final String COLOR_WATER = "Water";
    public static final String COLOR_MAGIC = "Magic";

    public final NumberSetting intensity = register(new NumberSetting(
            "Intensity",
            30.0D,
            5.0D,
            200.0D,
            1.0D,
            " drops"
    ).configKey("render.particlerain.intensity"));
    public final NumberSetting radius = register(new NumberSetting(
            "Radius",
            24.0D,
            8.0D,
            64.0D,
            1.0D,
            " blocks"
    ).configKey("render.particlerain.radius"));
    public final NumberSetting height = register(new NumberSetting(
            "Spawn Height",
            12.0D,
            4.0D,
            40.0D,
            1.0D,
            " blocks"
    ).configKey("render.particlerain.height"));
    public final NumberSetting fallSpeed = register(new NumberSetting(
            "Fall Speed",
            9.0D,
            2.0D,
            30.0D,
            0.5D,
            " blocks/s"
    ).configKey("render.particlerain.fallSpeed"));
    public final NumberSetting size = register(new NumberSetting(
            "Drop Size",
            0.05D,
            0.01D,
            0.2D,
            0.005D,
            " blocks"
    ).configKey("render.particlerain.size"));
    public final NumberSetting maxDrops = register(new NumberSetting(
            "Max Drops",
            120.0D,
            10.0D,
            400.0D,
            10.0D,
            ""
    ).configKey("render.particlerain.maxDrops"));
    public final BooleanSetting ripples = register(new BooleanSetting(
            "Ground Ripples",
            true
    ).configKey("render.particlerain.ripples"));
    public final NumberSetting rippleRadius = register(new NumberSetting(
            "Ripple Radius",
            0.8D,
            0.2D,
            3.0D,
            0.05D,
            " blocks"
    ).configKey("render.particlerain.rippleRadius").visibleWhen(() -> this.ripples.getValue()));
    public final NumberSetting rippleTime = register(new NumberSetting(
            "Ripple Time",
            0.9D,
            0.2D,
            3.0D,
            0.05D,
            " s"
    ).configKey("render.particlerain.rippleTime").visibleWhen(() -> this.ripples.getValue()));
    public final BooleanSetting glow = register(new BooleanSetting(
            "Glow Drops",
            true
    ).configKey("render.particlerain.glow"));
    public final ModeSetting colorMode = register(new ModeSetting(
            "Color",
            COLOR_WATER,
            COLOR_WATER,
            COLOR_MAGIC,
            COLOR_CUSTOM
    ).configKey("render.particlerain.colorMode"));
    public final ColorSetting color = register(new ColorSetting(
            "Custom Color",
            0xFF6FC3FF
    ).configKey("render.particlerain.color")
            .visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));

    public ParticleRainFeature() {
        super("ParticleRain", "3D drops fall from the sky and ripple on the ground like jump circles", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public static ParticleRainFeature getEnabled() {
        return FeatureManager.INSTANCE.getEnabled(ParticleRainFeature.class);
    }

    public int resolvedColor() {
        return switch (this.colorMode.getValue()) {
            case COLOR_WATER -> 0xFF6FC3FF;
            case COLOR_MAGIC -> 0xFFB48CFF;
            default -> this.color.getValue();
        };
    }
}
