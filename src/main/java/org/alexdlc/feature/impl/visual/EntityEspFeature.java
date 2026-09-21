package org.alexdlc.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.render.Render2DEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.MultiSelectSetting;
import org.alexdlc.feature.setting.NumberSetting;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.HurtUtil;
import org.alexdlc.utils.render.Render3DUtil;
import org.alexdlc.utils.render.Render3DUtil.ScreenBounds;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public final class EntityEspFeature extends Feature {
    private static final int BASE_COLOR = 0xFF277EFF;
    private static final float ESP_ALPHA = 0.92F;
    private static final float FADE_START_DISTANCE = 0.4F;
    private static final float FADE_FULL_DISTANCE = 2.0F;
    private static final float MIN_RENDER_ALPHA = 0.05F;
    private static final float BOX_LINE_WIDTH = 1.0F;

    private final MultiSelectSetting targets = register(new MultiSelectSetting(
            "Targets",
            Set.of("Players", "Hostile"),
            "Players",
            "Hostile",
            "Passive",
            "Items",
            "Projectiles"
    ));
    private final ModeSetting boxMode = register(new ModeSetting(
            "Box Mode",
            "Corners",
            "Corners",
            "Box"
    ));
    private final BooleanSetting rounded = register(new BooleanSetting(
            "Rounded",
            false
    ));
    private final BooleanSetting healthBar = register(new BooleanSetting(
            "Health Bar",
            true
    ));
    private final BooleanSetting box = register(new BooleanSetting(
            "Box",
            true
    ));
    private final NumberSetting distance = register(new NumberSetting(
            "Distance",
            64.0,
            5.0,
            128.0,
            1.0,
            " blocks"
    ));
    private final ModeSetting colorMode = register(ColorMode.setting());

    private final ColorSetting espColor = register(new ColorSetting(
            "Color",
            BASE_COLOR
    ).configKey("Glow Color").visibleWhen(() -> ColorMode.isCustom(this.colorMode)));

    public EntityEspFeature() {
        super("EntityESP", "Draws boxes and glow around selected entities.", FeatureCategory.VISUAL, GLFW.GLFW_KEY_G);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isValidOverlayTarget(mc, entity)) {
                continue;
            }

            float alpha = overlayAlpha(mc.player, entity);
            if (alpha < MIN_RENDER_ALPHA) {
                continue;
            }

            ScreenBounds bounds = projectEntityBounds(mc, entity, tickDelta);
            if (bounds == null || bounds.width() < 2.0F || bounds.height() < 2.0F) {
                continue;
            }

            int color = HurtUtil.blend(resolveColor(), entity, alpha);
            if (box.getValue()) {
                if (boxMode.is("Corners")) {
                    if (rounded.getValue()) {
                        drawRoundedCornerBox(bounds, color);
                    } else {
                        drawCornerBox(bounds, color);
                    }
                } else if (rounded.getValue()) {
                    drawRoundedBox(bounds, color);
                } else {
                    drawBox(bounds, color);
                }
            }

            if (healthBar.getValue() && entity instanceof LivingEntity livingEntity) {
                drawHealthBar(bounds, livingEntity, alpha);
            }
        }
    }

    private boolean isValidOverlayTarget(Minecraft mc, Entity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }
        if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return false;
        }
        if (!isWithinRenderDistance(mc.player, entity)) {
            return false;
        }
        return isTarget(entity);
    }

    private boolean isWithinRenderDistance(Player viewer, Entity entity) {
        if (viewer == null) {
            return false;
        }

        double maxDistance = distance.getValue();
        return viewer.distanceToSqr(entity) <= maxDistance * maxDistance;
    }

    private boolean isTarget(Entity entity) {
        if (entity instanceof Player) {
            return targets.isSelected("Players");
        }
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            return targets.isSelected("Items");
        }
        if (entity instanceof Projectile || entity instanceof EyeOfEnder || entity instanceof EvokerFangs) {
            return targets.isSelected("Projectiles");
        }

        MobCategory category = entity.getType().getCategory();
        if (category == MobCategory.MONSTER) {
            return targets.isSelected("Hostile");
        }
        if (category == MobCategory.CREATURE
                || category == MobCategory.AXOLOTLS
                || category == MobCategory.AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT) {
            return targets.isSelected("Passive");
        }

        return false;
    }

    private float overlayAlpha(Player viewer, Entity entity) {
        float distance = viewer.distanceTo(entity);
        float progress = (distance - FADE_START_DISTANCE) / (FADE_FULL_DISTANCE - FADE_START_DISTANCE);
        return ESP_ALPHA * Mth.clamp(progress, 0.0F, 1.0F);
    }

    private int resolveColor() {
        return ColorMode.resolve(this.colorMode, this.espColor);
    }

    private ScreenBounds projectEntityBounds(Minecraft mc, Entity entity, float tickDelta) {
        Vec3 renderPosition = Render3DUtil.interpolatedPosition(entity, tickDelta);
        double halfWidth = entity.getBbWidth() / 1.5D;
        double height = entity.getBbHeight() + 0.1D - (entity.isShiftKeyDown() ? 0.2D : 0.0D);

        return Render3DUtil.projectBoxBounds(
                mc,
                renderPosition.x - halfWidth, renderPosition.y, renderPosition.z - halfWidth,
                renderPosition.x + halfWidth, renderPosition.y + height, renderPosition.z + halfWidth
        );
    }

    private void drawBox(ScreenBounds bounds, int color) {
        int minX = Math.round(bounds.minX());
        int minY = Math.round(bounds.minY());
        int maxX = Math.round(bounds.maxX());
        int maxY = Math.round(bounds.maxY());
        int width = maxX - minX;
        int height = maxY - minY;
        if (width <= 0 || height <= 0) {
            return;
        }

        Render2DUtil.rect(minX, minY, width, Math.max(1, Math.round(BOX_LINE_WIDTH))).color(color).draw();
        Render2DUtil.rect(minX, maxY - 1, width, 1).color(color).draw();
        Render2DUtil.rect(minX, minY, 1, height).color(color).draw();
        Render2DUtil.rect(maxX - 1, minY, 1, height).color(color).draw();
    }

    private void drawCornerBox(ScreenBounds bounds, int color) {
        int minX = Math.round(bounds.minX());
        int minY = Math.round(bounds.minY());
        int maxX = Math.round(bounds.maxX());
        int maxY = Math.round(bounds.maxY());
        int width = maxX - minX;
        int height = maxY - minY;
        if (width <= 0 || height <= 0) {
            return;
        }

        int cornerWidth = Math.max(2, Math.round(width / 4.0F));
        int cornerHeight = Math.max(2, Math.round(height / 4.0F));

        Render2DUtil.rect(minX, minY, cornerWidth, 1).color(color).draw();
        Render2DUtil.rect(minX, minY, 1, cornerHeight).color(color).draw();

        Render2DUtil.rect(maxX - cornerWidth, minY, cornerWidth, 1).color(color).draw();
        Render2DUtil.rect(maxX - 1, minY, 1, cornerHeight).color(color).draw();

        Render2DUtil.rect(minX, maxY - 1, cornerWidth, 1).color(color).draw();
        Render2DUtil.rect(minX, maxY - cornerHeight, 1, cornerHeight).color(color).draw();

        Render2DUtil.rect(maxX - cornerWidth, maxY - 1, cornerWidth, 1).color(color).draw();
        Render2DUtil.rect(maxX - 1, maxY - cornerHeight, 1, cornerHeight).color(color).draw();
    }

    private void drawRoundedBox(ScreenBounds bounds, int color) {
        float minX = Math.round(bounds.minX());
        float minY = Math.round(bounds.minY());
        float width = Math.round(bounds.maxX()) - minX;
        float height = Math.round(bounds.maxY()) - minY;
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        float radius = Math.min(width, height) * 0.75F / 4.0F;
        Render2DUtil.rect(minX, minY, width, height)
                .color(0x00000000)
                .radius(radius)
                .border(BOX_LINE_WIDTH, color)
                .draw();
    }

    private void drawRoundedCornerBox(ScreenBounds bounds, int color) {
        float minX = Math.round(bounds.minX());
        float minY = Math.round(bounds.minY());
        float width = Math.round(bounds.maxX()) - minX;
        float height = Math.round(bounds.maxY()) - minY;
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        float cornerWidth = Math.max(3.0F, width / 4.0F);
        float cornerHeight = Math.max(3.0F, height / 4.0F);
        float radius = Math.min(cornerWidth, cornerHeight) * 0.75F;
        float maxY = minY + height;
        float maxX = minX + width;

        float[][] corners = {
                {minX, minY},
                {maxX - cornerWidth, minY},
                {minX, maxY - cornerHeight},
                {maxX - cornerWidth, maxY - cornerHeight}
        };
        for (float[] corner : corners) {
            Render2DUtil.pushScissor(corner[0], corner[1], cornerWidth, cornerHeight);
            Render2DUtil.rect(minX, minY, width, height)
                    .color(0x00000000)
                    .radius(radius)
                    .border(BOX_LINE_WIDTH, color)
                    .draw();
            Render2DUtil.popScissor();
        }
    }

    private void drawHealthBar(ScreenBounds bounds, LivingEntity entity, float alpha) {
        float health = Mth.clamp(entity.getHealth(), 0.0F, entity.getMaxHealth());
        float ratio = entity.getMaxHealth() <= 0.0F ? 0.0F : health / entity.getMaxHealth();
        int barHeight = Math.max(1, Math.round(bounds.height()));
        int filledHeight = Math.max(0, Math.round(barHeight * ratio));
        int x = Math.round(bounds.minX()) - 4;
        int y = Math.round(bounds.minY());

        Render2DUtil.rect(x, y, 2, barHeight).color(ColorUtil.multiplyAlpha(0x80000000, alpha)).draw();
        if (filledHeight <= 0) {
            return;
        }

        int fillY = y + (barHeight - filledHeight);
        int fillColor = ColorUtil.multiplyAlpha(ColorUtil.lerp(ColorUtil.RED, ColorUtil.GREEN, ratio), alpha);
        Render2DUtil.rect(x, fillY, 2, filledHeight).color(fillColor).draw();
    }

}
