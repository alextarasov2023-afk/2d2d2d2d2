package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.NumberSetting;

public final class ShaderHandsFeature extends Feature {
    public static final String SHADER_SOLID = "Solid";
    public static final String SHADER_SILK = "Silk";
    public static final String SHADER_PLASMA = "Plasma";
    public static final String SHADER_GLASS = "Glass";
    public static final String SHADER_AURORA = "Aurora";
    public static final String SHADER_PRISM = "Prism";
    public static final String SHADER_LIQUID = "Liquid Metal";
    public static final String SHADER_HOLOGRAM = "Hologram";
    public static final String SHADER_NEBULA = "Nebula";

    public final ModeSetting shader = register(new ModeSetting(
            "Shader",
            SHADER_SILK,
            SHADER_SOLID,
            SHADER_SILK,
            SHADER_PLASMA,
            SHADER_GLASS,
            SHADER_AURORA,
            SHADER_PRISM,
            SHADER_LIQUID,
            SHADER_HOLOGRAM,
            SHADER_NEBULA
    ).configKey("render.armtweaks.fill.type"));
    public final ModeSetting colorMode = register(ColorMode.setting()
            .configKey("render.armtweaks.fill.colorMode"));
    public final ColorSetting color = register(new ColorSetting(
            "Color",
            0xFF7986CB
    ).configKey("render.armtweaks.fill.color")
            .visibleWhen(() -> ColorMode.isCustom(this.colorMode)));
    public final NumberSetting opacity = register(new NumberSetting(
            "Opacity",
            0.42D,
            0.05D,
            1.0D,
            0.01D,
            ""
    ).configKey("render.armtweaks.fill.opacity"));
    public final NumberSetting speed = register(new NumberSetting(
            "Speed",
            1.0D,
            0.1D,
            8.0D,
            0.1D,
            "x"
    ).configKey("render.armtweaks.fill.plasma.speed"));
    public final NumberSetting glassBlur = register(new NumberSetting(
            "Glass Blur",
            14.0D,
            0.0D,
            60.0D,
            1.0D,
            "px"
    ).configKey("render.armtweaks.fill.glass.blur").visibleWhen(() -> this.shader.is(SHADER_GLASS)));
    public final BooleanSetting glassMirror = register(new BooleanSetting(
            "Mirror Reflection",
            true
    ).configKey("render.armtweaks.fill.glass.mirror").visibleWhen(() -> this.shader.is(SHADER_GLASS)));
    public final BooleanSetting outline = register(new BooleanSetting(
            "Edge Outline",
            false
    ).configKey("render.armtweaks.fill.outline"));
    public final NumberSetting outlineThickness = register(new NumberSetting(
            "Outline Thickness",
            1.5D,
            0.5D,
            5.0D,
            0.1D,
            "px"
    ).configKey("render.armtweaks.fill.outline.thickness").visibleWhen(() -> this.outline.getValue()));
    public final BooleanSetting glow = register(new BooleanSetting(
            "Glow",
            false
    ).configKey("render.armtweaks.fill.glow"));
    public final NumberSetting glowStrength = register(new NumberSetting(
            "Glow Strength",
            1.0D,
            0.0D,
            3.0D,
            0.05D,
            "x"
    ).configKey("render.armtweaks.fill.glow.strength").visibleWhen(() -> this.glow.getValue()));
    public final NumberSetting glowRadius = register(new NumberSetting(
            "Glow Radius",
            20.0D,
            1.0D,
            60.0D,
            1.0D,
            "px"
    ).configKey("render.armtweaks.fill.glow.radius").visibleWhen(() -> this.glow.getValue()));
    public final BooleanSetting trail = register(new BooleanSetting(
            "Flame Trail",
            false
    ).configKey("render.armtweaks.fill.glow.flame").visibleWhen(() -> this.glow.getValue()));
    public final NumberSetting trailLength = register(new NumberSetting(
            "Trail Length",
            1.0D,
            0.0D,
            1.0D,
            0.01D,
            ""
    ).configKey("render.armtweaks.fill.glow.flameTrail").visibleWhen(() -> this.trail.getValue()));
    public final BooleanSetting bothHands = register(new BooleanSetting(
            "Both Hands",
            true
    ).configKey("render.armtweaks.fill.bothHands"));

    public ShaderHandsFeature() {
        super("ShaderHands", "Paints your first-person hands with an animated shader", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public int resolvedColor() {
        return ColorMode.resolve(this.colorMode, this.color);
    }

    public boolean usesGlass() {
        return this.shader.is(SHADER_GLASS);
    }

    public boolean usesOutline() {
        return this.outline.getValue();
    }

    public boolean usesGlow() {
        return this.glow.getValue();
    }

    public boolean usesTrail() {
        return this.usesGlow() && this.trail.getValue();
    }
}
