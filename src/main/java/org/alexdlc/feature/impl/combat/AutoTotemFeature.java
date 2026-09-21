package org.alexdlc.feature.impl.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.alexdlc.context.MinecraftContext;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.MultiSelectSetting;
import org.alexdlc.feature.setting.NumberSetting;
import org.alexdlc.pve.AutomationOwner;
import org.alexdlc.pve.AutomationPriority;
import org.alexdlc.pve.AutomationResource;
import org.alexdlc.pve.PveAutomationCoordinator;
import org.alexdlc.utils.inventory.InventorySwap;
import org.alexdlc.utils.inventory.InventoryUtil;

import java.util.EnumSet;
import java.util.Set;

public final class AutoTotemFeature extends Feature implements MinecraftContext, AutomationOwner {
    private static final double DANGER_RADIUS = 6.0;
    private static final int ANCHOR_RADIUS_XZ = 4;
    private static final int ANCHOR_RADIUS_Y = 2;
    private static final double FALL_CLIP_DEPTH = 128.0;
    private static final double SAFE_FALL_DISTANCE = 3.0;

    public final NumberSetting health = register(new NumberSetting("Health", 16.0, 1.0, 20.0, 0.5, ""));
    public final BooleanSetting skipTalismans = register(new BooleanSetting("Skip Talismans", true));
    public final BooleanSetting notWhileEating = register(new BooleanSetting("Not While Eating", true));
    public final BooleanSetting notWithHead = register(new BooleanSetting("Not With Head", true));
    public final MultiSelectSetting dangers = register(new MultiSelectSetting(
            "Dangers",
            Set.of("Fall", "Crystal", "Explosion", "Obsidian", "Anchor", "Mace", "Spear"),
            "Fall",
            "Crystal",
            "Explosion",
            "Obsidian",
            "Anchor",
            "Mace",
            "Spear"
    ));

    private boolean releasePending;

    public AutoTotemFeature() {
        super("AutoTotem", "Keeps a totem of undying in your offhand", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    @Override
    protected void onDisable() {
        this.releasePending = false;
        PveAutomationCoordinator.INSTANCE.release(this);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (this.releasePending && !InventorySwap.isBusy()) {
            this.releasePending = false;
            PveAutomationCoordinator.INSTANCE.release(this);
        }
        LocalPlayer player = player();
        if (player == null
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || InventorySwap.isBusy()) {
            return;
        }
        if (notWhileEating.getValue() && isEating(player)) {
            return;
        }
        if (notWithHead.getValue() && player.getMainHandItem().is(Items.PLAYER_HEAD)) {
            return;
        }
        if (player.getHealth() > health.getValue() && !dangerNearby(player)) {
            return;
        }
        int containerSlot = findTotemSlot(player);
        if (containerSlot != -1
                && PveAutomationCoordinator.INSTANCE.acquire(
                this,
                AutomationPriority.EMERGENCY,
                EnumSet.of(AutomationResource.INVENTORY)
        )) {
            InventorySwap.equip(containerSlot);
            this.releasePending = true;
        }
    }

    private static boolean isEating(LocalPlayer player) {
        return player.isUsingItem() && player.getUseItem().has(DataComponents.FOOD);
    }

    private boolean dangerNearby(LocalPlayer player) {
        AABB box = player.getBoundingBox().inflate(DANGER_RADIUS);
        return (dangers.isSelected("Fall") && lethalFall(player))
                || (dangers.isSelected("Crystal") && !mc.level.getEntitiesOfClass(EndCrystal.class, box).isEmpty())
                || (dangers.isSelected("Explosion") && explosionNearby(box))
                || (dangers.isSelected("Anchor") && anchorNearby(player))
                || armedPlayerNearby(player, box);
    }

    private boolean lethalFall(LocalPlayer player) {
        if (player.onGround()
                || player.isInWater()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.hasEffect(MobEffects.SLOW_FALLING)
                || player.getDeltaMovement().y >= 0.0) {
            return false;
        }

        Vec3 from = player.position();
        BlockHitResult hit = mc.level.clip(new ClipContext(
                from,
                from.add(0.0, -FALL_CLIP_DEPTH, 0.0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                player
        ));

        if (hit.getType() != HitResult.Type.MISS
                && !mc.level.getFluidState(hit.getBlockPos()).isEmpty()) {
            return false;
        }
        double groundY = hit.getType() == HitResult.Type.MISS
                ? from.y - FALL_CLIP_DEPTH
                : hit.getLocation().y;
        double predictedDamage = player.fallDistance + (from.y - groundY) - SAFE_FALL_DISTANCE;
        return predictedDamage >= player.getHealth();
    }

    private boolean explosionNearby(AABB box) {
        if (!mc.level.getEntitiesOfClass(PrimedTnt.class, box).isEmpty()) {
            return true;
        }
        return !mc.level.getEntitiesOfClass(Creeper.class, box,
                creeper -> creeper.isIgnited() || creeper.getSwellDir() > 0).isEmpty();
    }

    private boolean anchorNearby(LocalPlayer player) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-ANCHOR_RADIUS_XZ, -ANCHOR_RADIUS_Y, -ANCHOR_RADIUS_XZ),
                center.offset(ANCHOR_RADIUS_XZ, ANCHOR_RADIUS_Y, ANCHOR_RADIUS_XZ))) {
            if (mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) {
                return true;
            }
        }
        return false;
    }

    private boolean armedPlayerNearby(LocalPlayer player, AABB box) {
        boolean obsidian = dangers.isSelected("Obsidian");
        boolean mace = dangers.isSelected("Mace");
        boolean spear = dangers.isSelected("Spear");
        if (!obsidian && !mace && !spear) {
            return false;
        }
        for (Player other : mc.level.getEntitiesOfClass(Player.class, box, p -> p != player)) {
            if (isThreatItem(other.getMainHandItem(), obsidian, mace, spear)
                    || isThreatItem(other.getOffhandItem(), obsidian, mace, spear)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isThreatItem(ItemStack held, boolean obsidian, boolean mace, boolean spear) {
        return (obsidian && (held.is(Items.OBSIDIAN) || held.is(Items.CRYING_OBSIDIAN)))
                || (mace && held.is(Items.MACE))
                || (spear && held.is(ItemTags.SPEARS));
    }

    private int findTotemSlot(LocalPlayer player) {
        return InventoryUtil.findPlayerMenuSlot(player, this::isUsableTotem);
    }

    private boolean isUsableTotem(ItemStack stack) {
        return stack.is(Items.TOTEM_OF_UNDYING)
                && !(skipTalismans.getValue() && stack.isEnchanted());
    }
}
