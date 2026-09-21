package org.alexdlc.feature.impl.visual;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.render.Render2DEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.NumberSetting;
import org.alexdlc.utils.ColorUtil;
import org.alexdlc.utils.render.Render3DUtil;
import org.alexdlc.utils.render.Theme;
import org.alexdlc.utils.render.gui.MsdfFont;
import org.alexdlc.utils.render.gui.Render2DUtil;
import org.alexdlc.utils.render.gui.UiFontStyle;
import org.alexdlc.utils.render.gui.UiFonts;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import org.alexdlc.utils.render.Textures;

public final class NameTagsFeature extends Feature {
    private static final int DIVIDER_COLOR = ColorUtil.rgba(255, 255, 255, 20);

    private static final float SCALE = 1.1F;
    private static final float PILL_HEIGHT = 40.0F;
    private static final float RADIUS = 16.0F;
    private static final float PADDING = 10.0F;
    private static final float GAP = 8.0F;
    private static final float HEAD_SIZE = 20.0F;
    private static final float ICON_SIZE = 16.0F;
    private static final float ITEM_SIZE = 16.0F;
    private static final float DIVIDER_HEIGHT = 12.0F;
    private static final float TEXT_SIZE = 12.0F;
    private static final float HEAD_OFFSET = 0.25F;

    private static final EquipmentSlot[] EQUIPMENT_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private record Privilege(String name, int color) {
    }

    public final NumberSetting scale = register(new NumberSetting("Scale", 1.0, 0.95, 1.2, 0.05, "x"));
    public final BooleanSetting privilege = register(new BooleanSetting("Privilege", true));
    public final BooleanSetting health = register(new BooleanSetting("Health", true));
    public final BooleanSetting items = register(new BooleanSetting("Items", true));
    public final NumberSetting distance = register(new NumberSetting("Distance", 64, 16, 128, 8, " blocks"));

    public NameTagsFeature() {
        super("NameTags", "Draws styled nametags above players", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public static boolean shouldHideVanillaTag() {
        return FeatureManager.INSTANCE.getEnabled(NameTagsFeature.class) != null;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float unit = (float) (SCALE * this.scale.getValue() / mc.getWindow().getGuiScale());
        double maxDistanceSqr = this.distance.getValue() * this.distance.getValue();

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }
            if (player.isRemoved() || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (mc.player.distanceToSqr(player) > maxDistanceSqr) {
                continue;
            }
            drawTag(event, player, tickDelta, unit);
        }
    }

    private void drawTag(Render2DEvent event, AbstractClientPlayer player, float tickDelta, float unit) {
        Minecraft mc = event.getClient();
        Vec3 position = Render3DUtil.interpolatedPosition(player, tickDelta)
                .add(0.0D, player.getBbHeight() + HEAD_OFFSET, 0.0D);
        Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, position);
        if (anchor == null) {
            return;
        }

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float headSize = HEAD_SIZE * unit;
        float iconSize = ICON_SIZE * unit;
        float itemSize = ITEM_SIZE * unit;
        float gap = GAP * unit;
        float dividerWidth = Math.max(0.5F, 0.5F * unit);

        String name = player.getGameProfile().name();
        Privilege donat = this.privilege.getValue() ? resolvePrivilege(player) : null;
        String healthText = this.health.getValue()
                ? (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount()) + "HP"
                : null;
        List<ItemStack> equipment = this.items.getValue() ? equipment(player) : List.of();

        float width = PADDING * unit + headSize + gap + font.measureWidth(name, textSize, letterSpacing);
        if (donat != null) {
            width += gap + dividerWidth + gap + iconSize + gap
                    + font.measureWidth(donat.name(), textSize, letterSpacing);
        }
        if (healthText != null) {
            width += gap + dividerWidth + gap + iconSize + gap
                    + font.measureWidth(healthText, textSize, letterSpacing);
        }
        if (!equipment.isEmpty()) {
            width += gap + dividerWidth + gap
                    + equipment.size() * itemSize + (equipment.size() - 1) * gap;
        }
        width += PADDING * unit;

        float pillHeight = PILL_HEIGHT * unit;
        float pillX = anchor.x() - width / 2.0F;
        float pillY = anchor.y() - pillHeight;
        float centerY = pillY + pillHeight / 2.0F;
        float textY = font.centeredTextY(centerY, textSize);

        Render2DUtil.rect(pillX, pillY, width, pillHeight)
                .color(Theme.Colors.BACKGROUND_PRIMARY_50)
                .radius(RADIUS * unit)
                .border(Math.max(0.5F, 0.5F * unit), Theme.Colors.OUTLINES_MEDIUM)
                .blur(8.0F * unit)
                .draw();

        float cursor = pillX + PADDING * unit;

        drawHead(player, cursor, centerY - headSize / 2.0F, headSize);
        cursor += headSize + gap;

        Render2DUtil.text(cursor, textY, textSize, name)
                .style(UiFontStyle.MEDIUM)
                .color(Theme.Colors.TEXT_TITLE)
                .draw();
        cursor += font.measureWidth(name, textSize, letterSpacing);

        if (donat != null) {
            cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);

            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.SPARKLES)
                    .color(Theme.Colors.ICON_GHOST)
                    .draw();
            cursor += iconSize + gap;

            Render2DUtil.text(cursor, textY, textSize, donat.name())
                    .style(UiFontStyle.MEDIUM)
                    .color(donat.color())
                    .draw();
            cursor += font.measureWidth(donat.name(), textSize, letterSpacing);
        }

        if (healthText != null) {
            cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);

            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.SCAN_HEART)
                    .color(Theme.Colors.ICON_GHOST)
                    .draw();
            cursor += iconSize + gap;

            Render2DUtil.text(cursor, textY, textSize, healthText)
                    .style(UiFontStyle.MEDIUM)
                    .color(Theme.Colors.TEXT_TITLE)
                    .draw();
            cursor += font.measureWidth(healthText, textSize, letterSpacing);
        }

        if (!equipment.isEmpty()) {
            cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);

            Render2DUtil.flush();

            Matrix3x2fStack pose = event.getGuiGraphicsExtractor().pose();
            float guiScale = (float) mc.getWindow().getGuiScale();
            float scale = itemSize / 16.0F;
            float itemY = Math.round((centerY - itemSize / 2.0F) * guiScale) / guiScale;
            for (ItemStack stack : equipment) {
                float itemX = Math.round(cursor * guiScale) / guiScale;
                pose.pushMatrix();
                pose.translate(itemX, itemY);
                pose.scale(scale);
                event.getGuiGraphicsExtractor().item(stack, 0, 0);
                pose.popMatrix();
                cursor += itemSize + gap;
            }
        }
    }

    private static void drawHead(AbstractClientPlayer player, float x, float y, float size) {
        Identifier skin = player.getSkin().body().texturePath();
        float radius = size * 0.2F;
        Render2DUtil.texture(x, y, size, size, skin)
                .managed()
                .uv(8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F)
                .radius(radius)
                .draw();
        Render2DUtil.texture(x, y, size, size, skin)
                .managed()
                .uv(40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F)
                .radius(radius)
                .draw();
    }

    private float drawDivider(float cursor, float centerY, float dividerWidth, float gap, float unit) {
        cursor += gap;
        Render2DUtil.rect(cursor, centerY - DIVIDER_HEIGHT * unit / 2.0F, dividerWidth, DIVIDER_HEIGHT * unit)
                .color(DIVIDER_COLOR)
                .draw();
        return cursor + dividerWidth + gap;
    }

    private static List<ItemStack> equipment(AbstractClientPlayer player) {
        List<ItemStack> stacks = new ArrayList<>(EQUIPMENT_ORDER.length);
        for (EquipmentSlot slot : EQUIPMENT_ORDER) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static Privilege resolvePrivilege(AbstractClientPlayer player) {
        PlayerTeam team = player.getTeam();
        if (team == null) {
            return null;
        }
        Component prefix = team.getPlayerPrefix();
        if (prefix == null) {
            return null;
        }

        String raw = prefix.getString();
        String name = cleanName(raw);
        if (name.isEmpty()) {
            return null;
        }

        Integer color = extractColor(prefix);
        if (color == null) {
            color = legacyColor(raw);
        }
        int rgb = color != null
                ? ColorUtil.rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
                : Theme.getAccent();
        return new Privilege(name, rgb);
    }

    private static String cleanName(String text) {
        String stripped = text.replaceAll("§.", "");
        StringBuilder clean = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char character = stripped.charAt(i);
            if (isBasicLetterOrDigit(character) || character == ' ') {
                clean.append(character);
            }
        }
        return clean.toString().replaceAll("\\s+", " ").trim();
    }

    private static boolean isBasicLetterOrDigit(char character) {
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9')
                || (character >= 'а' && character <= 'я')
                || (character >= 'А' && character <= 'Я')
                || character == 'ё' || character == 'Ё';
    }

    private static Integer legacyColor(String text) {
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != '§') {
                continue;
            }
            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting == null) {
                continue;
            }
            TextColor color = TextColor.fromLegacyFormat(formatting);
            if (color != null) {
                return color.getValue();
            }
        }
        return null;
    }

    private static Integer extractColor(Component component) {
        TextColor color = component.getStyle().getColor();
        if (color != null) {
            return color.getValue();
        }
        for (Component sibling : component.getSiblings()) {
            Integer siblingColor = extractColor(sibling);
            if (siblingColor != null) {
                return siblingColor;
            }
        }
        return null;
    }
}
