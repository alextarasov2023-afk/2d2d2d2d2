package org.alexdlc.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.alexdlc.context.RenderContext;
import org.alexdlc.feature.impl.combat.AuraFeature;
import org.alexdlc.feature.impl.combat.TriggerBotFeature;
import org.alexdlc.mixin.accessor.ModelPartAccessor;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.PlayerHead;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.TextAlign;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public final class TargetElement extends HudElement {
    private static final int BAR_TRACK_COLOR = Theme.Colors.OUTLINES_MEDIUM;
    private static final int BAR_SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 26);

    private static final float ITEM_SIZE = 12.0F;
    private static final float ITEM_GAP = 2.0F;
    private static final float TEXT_SIZE = 10.0F;
    private static final float BAR_HEIGHT = 8.0F;
    private static final float BAR_INSET = 2.0F;

    private record FaceIcon(Identifier texture, float u0, float v0, float u1, float v1) {
    }

    private static final EquipmentSlot[] EQUIPMENT_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private record Style(
            float width,
            float height,
            float radius,
            float padding,
            float gap,
            float headSize,
            boolean showItems
    ) {
    }

    private static final Style MINI = new Style(111.0F, 38.0F, 8.0F, 8.0F, 4.0F, 22.0F, false);

    private final Style style = MINI;
    private LivingEntity target;
    private LivingEntity smoothedFor;
    private float smoothedHealth;
    private long lastFrameTime;

    private LivingEntity fadeEntity;
    private float fadeAlpha;
    private long lastFadeTime;

    public TargetElement() {
        super("target", "Target");
    }

    @Override
    protected float defaultY(float unit) {
        return 340.0F * unit;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        this.target = resolveTarget(mc);
        if (this.target != null) {
            this.fadeEntity = this.target;
        }
        updateFade();
        if (this.fadeEntity == null || (this.target == null && this.fadeAlpha <= 0.02F)) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }
        this.width = this.style.width() * unit;
        this.height = this.style.height() * unit;
    }

    private void updateFade() {
        long now = System.currentTimeMillis();
        float delta = this.lastFadeTime == 0L ? 0.016F : Math.min(100L, now - this.lastFadeTime) / 1000.0F;
        this.lastFadeTime = now;
        float goal = this.target != null ? 1.0F : 0.0F;
        float step = Math.clamp(delta * 12.0F, 0.0F, 1.0F);
        this.fadeAlpha += (goal - this.fadeAlpha) * step;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        LivingEntity entity = this.target != null ? this.target : this.fadeEntity;
        if (entity == null) {
            return;
        }
        float alpha = this.fadeAlpha;

        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float padding = this.style.padding() * unit;
        float contentX = this.x + padding;
        float contentRight = this.x + this.width - padding;
        float headTop = this.y + padding;
        float headSize = this.style.headSize() * unit;

        drawHead(entity, contentX, headTop, headSize, alpha);

        float infoX = contentX + headSize + this.style.gap() * unit;
        float health = entity.getHealth() + entity.getAbsorptionAmount();
        String healthText = Integer.toString((int) Math.ceil(health));
        float healthTextWidth = font.measureWidth(healthText, textSize, letterSpacing);

        float nameCenterY = this.style.showItems()
                ? headTop + headSize - (TEXT_SIZE / 2.0F) * unit
                : headTop + (TEXT_SIZE / 2.0F) * unit;
        String name = font.ellipsize(
                entity.getName().getString(), textSize, letterSpacing,
                contentRight - infoX - healthTextWidth - this.style.gap() * unit);
        Render2DUtil.text(infoX, font.centeredTextY(nameCenterY, textSize), textSize, name)
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TITLE, alpha))
                .draw();
        Render2DUtil.text(contentRight, font.centeredTextY(nameCenterY, textSize), textSize, healthText)
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha))
                .align(TextAlign.RIGHT)
                .draw();

        if (this.style.showItems()) {

            if (alpha >= 0.75F) {
                drawItems(mc, entity, infoX, headTop, unit);
            }
            drawHealthBar(entity, health, contentX, this.y + this.height - padding - BAR_HEIGHT * unit,
                    contentRight - contentX, unit, alpha);
        } else {

            drawHealthBar(entity, health, infoX, headTop + headSize - BAR_HEIGHT * unit,
                    contentRight - infoX, unit, alpha);
        }
    }

    private void drawHead(LivingEntity entity, float x, float y, float size, float alpha) {
        float radius = size * 0.2F;
        int tint = ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha);
        if (entity instanceof AbstractClientPlayer player) {
            PlayerHead.draw(x, y, size, player.getSkin().body().texturePath(), radius, tint);
        } else {
            Render2DUtil.rect(x, y, size, size)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.OUTLINES_MEDIUM, alpha))
                    .radius(radius)
                    .draw();
            FaceIcon icon = resolveFaceIcon(entity);
            if (icon != null) {
                Render2DUtil.texture(x, y, size, size, icon.texture())
                        .managed()
                        .uv(icon.u0(), icon.v0(), icon.u1(), icon.v1())
                        .radius(radius)
                        .color(tint)
                        .draw();
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private FaceIcon resolveFaceIcon(LivingEntity entity) {
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = (EntityRenderState) renderer.createRenderState(entity, 1.0F);
        if (!(renderer instanceof LivingEntityRenderer livingRenderer)
                || !(state instanceof LivingEntityRenderState livingState)) {
            return null;
        }

        Identifier texture = livingRenderer.getTextureLocation(livingState);
        Model<?> model = livingRenderer.getModel();
        ModelPart root = model.root();
        var lookup = root.createPartLookup();
        ModelPart iconPart = lookup.apply("head");
        if (iconPart == null) {
            iconPart = lookup.apply("body");
        }
        if (iconPart == null) {
            iconPart = root;
        }

        FaceIcon icon = findLargestFrontFace(texture, iconPart);
        return icon != null || iconPart == root ? icon : findLargestFrontFace(texture, root);
    }

    private static FaceIcon findLargestFrontFace(Identifier texture, ModelPart part) {
        FaceIcon result = null;
        float largestArea = 0.0F;
        for (ModelPart candidate : part.getAllParts()) {
            for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) candidate).alexdlc$getCubes()) {
                for (ModelPart.Polygon polygon : cube.polygons) {
                    if (polygon.normal().z() > -0.9F) {
                        continue;
                    }
                    float minU = Float.POSITIVE_INFINITY;
                    float minV = Float.POSITIVE_INFINITY;
                    float maxU = Float.NEGATIVE_INFINITY;
                    float maxV = Float.NEGATIVE_INFINITY;
                    for (ModelPart.Vertex vertex : polygon.vertices()) {
                        minU = Math.min(minU, vertex.u());
                        minV = Math.min(minV, vertex.v());
                        maxU = Math.max(maxU, vertex.u());
                        maxV = Math.max(maxV, vertex.v());
                    }
                    float area = (maxU - minU) * (maxV - minV);
                    if (area > largestArea) {
                        largestArea = area;
                        result = new FaceIcon(texture, minU, minV, maxU, maxV);
                    }
                }
            }
        }
        return result;
    }

    private void drawItems(Minecraft mc, LivingEntity entity, float x, float y, float unit) {
        List<ItemStack> stacks = new ArrayList<>(EQUIPMENT_ORDER.length);
        for (EquipmentSlot slot : EQUIPMENT_ORDER) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        if (stacks.isEmpty()) {
            return;
        }

        GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
        if (extractor == null) {
            return;
        }

        Render2DUtil.flush();

        Matrix3x2fStack pose = extractor.pose();
        float guiScale = (float) mc.getWindow().getGuiScale();
        float itemSize = ITEM_SIZE * unit;
        float scale = itemSize / 16.0F;
        float itemY = Math.round(y * guiScale) / guiScale;
        float cursor = x;
        for (ItemStack stack : stacks) {
            float itemX = Math.round(cursor * guiScale) / guiScale;
            pose.pushMatrix();
            pose.translate(itemX, itemY);
            pose.scale(scale);
            extractor.item(stack, 0, 0);
            pose.popMatrix();
            cursor += itemSize + ITEM_GAP * unit;
        }
    }

    private void drawHealthBar(LivingEntity entity, float health, float x, float y, float width, float unit, float alpha) {
        float maxHealth = Math.max(1.0F, entity.getMaxHealth() + entity.getAbsorptionAmount());
        float fraction = Math.clamp(smoothHealth(entity, health) / maxHealth, 0.0F, 1.0F);

        float barHeight = BAR_HEIGHT * unit;
        Render2DUtil.rect(x, y, width, barHeight)
                .color(ColorUtil.multiplyAlpha(BAR_TRACK_COLOR, alpha))
                .radius(barHeight / 2.0F)
                .draw();

        float inset = BAR_INSET * unit;
        float fillHeight = barHeight - inset * 2.0F;
        float fillWidth = (width - inset * 2.0F) * fraction;
        if (fillWidth >= fillHeight) {
            Render2DUtil.rect(x + inset, y + inset, fillWidth, fillHeight)
                    .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                    .radius(fillHeight / 2.0F)
                    .shadow(ColorUtil.multiplyAlpha(BAR_SHADOW_COLOR, alpha), 7.5F * unit)
                    .draw();
        }
    }

    private float smoothHealth(LivingEntity entity, float health) {
        long now = System.currentTimeMillis();
        float delta = this.lastFrameTime == 0L ? 0.016F : Math.min(100L, now - this.lastFrameTime) / 1000.0F;
        this.lastFrameTime = now;

        if (this.smoothedFor != entity) {
            this.smoothedFor = entity;
            this.smoothedHealth = health;
        }
        float step = Math.clamp(delta * 10.0F, 0.0F, 1.0F);
        this.smoothedHealth += (health - this.smoothedHealth) * step;
        return this.smoothedHealth;
    }

    private LivingEntity resolveTarget(Minecraft mc) {
        AuraFeature aura = AuraFeature.getMarkerFeature();
        LivingEntity current = aura != null ? aura.getCurrentTarget() : null;
        if (current == null) {
            TriggerBotFeature triggerBot = TriggerBotFeature.getEnabled();
            if (triggerBot != null) {
                current = triggerBot.getCurrentTarget();
            }
        }
        if (current == null && showcase(mc)) {
            current = mc.player;
        }
        return current != null && current.isAlive() ? current : null;
    }
}
