package org.alexdlc.pve.navigation;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.alexdlc.pve.mining.MiningInventory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import java.util.List;

public final class BaritoneNavigator implements Navigator {
    public static final BaritoneNavigator INSTANCE = new BaritoneNavigator();
    private static final List<Block> CLIMBABLE_BLOCKS = List.of(
            Blocks.LADDER,
            Blocks.VINE,
            Blocks.SCAFFOLDING
    );
    private static final List<Block> COMMON_TUNNEL_BLOCKS =
            MiningInventory.commonTunnelBlocks();

    private SettingsSnapshot snapshot;
    private BlockPos currentGoal;
    private boolean active;
    private boolean strictBreakWhitelist;
    private ISelection mineBoundsSelection;

    private BaritoneNavigator() {
    }

    @Override
    public boolean isAvailable() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone() != null;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public synchronized void begin(NavigationOptions options) {
        Objects.requireNonNull(options, "options");
        Settings settings = BaritoneAPI.getSettings();
        if (!this.active) {
            this.snapshot = SettingsSnapshot.capture(settings);
            this.active = true;
        }
        this.strictBreakWhitelist = options.strictBreakWhitelist();
        settings.allowBreak.value = this.strictBreakWhitelist
                ? false
                : options.allowBreak();
        settings.allowBreakAnyway.value = this.strictBreakWhitelist
                ? new ArrayList<>(COMMON_TUNNEL_BLOCKS)
                : new ArrayList<>(this.snapshot.allowBreakAnyway);
        settings.allowPlace.value = options.allowPlace();
        settings.allowSprint.value = options.allowSprint();
        settings.freeLook.value = !options.rotateView();
        settings.blockFreeLook.value = !options.rotateView();
        settings.mineScanDroppedItems.value = options.scanDroppedItems();
        settings.mineSearchRadius.value = options.mineSearchRadius();
        settings.mineAoeLevel.value = this.snapshot.mineAoeLevel;
        restoreMineBounds(settings, this.snapshot);
        settings.allowRightClick.value = options.allowInteract();
        settings.chunkCaching.value = false;
        settings.renderCachedChunks.value = false;
        ArrayList<Block> protectedBlocks = new ArrayList<>(
                this.snapshot.blocksToDisallowBreaking
        );
        if (options.protectClimbables()) {
            for (Block block : CLIMBABLE_BLOCKS) {
                if (!protectedBlocks.contains(block)) {
                    protectedBlocks.add(block);
                }
            }
        }
        settings.blocksToDisallowBreaking.value = protectedBlocks;
        if (options.fastMining()) {
            settings.mineGoalUpdateInterval.value = 5;
            settings.mineMaxOreLocationsCount.value = 6;
            settings.mineDropLoiterDurationMSThanksLouca.value = 0L;
            settings.exploreForBlocks.value = false;
        } else {
            settings.mineGoalUpdateInterval.value = this.snapshot.mineGoalUpdateInterval;
            settings.mineMaxOreLocationsCount.value =
                    this.snapshot.mineMaxOreLocationsCount;
            settings.mineDropLoiterDurationMSThanksLouca.value =
                    this.snapshot.mineDropLoiterMillis;
            settings.exploreForBlocks.value = this.snapshot.exploreForBlocks;
        }
        settings.chatControl.value = false;
        settings.chatControlAnyway.value = false;
        settings.renderPath.value = this.snapshot.renderPath;
        settings.renderGoal.value = this.snapshot.renderGoal;
        settings.renderGoalXZBeacon.value = this.snapshot.renderGoalXZBeacon;
        settings.renderSelectionBoxes.value = this.snapshot.renderSelectionBoxes;
    }

    @Override
    public synchronized void pathTo(BlockPos position, int radius) {
        requireActive();
        this.currentGoal = Objects.requireNonNull(position, "position").immutable();
        baritone().getCustomGoalProcess().setGoalAndPath(
                new GoalNear(this.currentGoal, Math.max(0, radius))
        );
    }

    @Override
    public synchronized void mine(int quantity, Block... blocks) {
        requireActive();
        if (blocks == null || blocks.length == 0) {
            throw new IllegalArgumentException("At least one target block is required");
        }
        if (this.strictBreakWhitelist) {
            LinkedHashSet<Block> allowed = new LinkedHashSet<>(COMMON_TUNNEL_BLOCKS);
            allowed.addAll(List.of(blocks));
            allowed.removeAll(CLIMBABLE_BLOCKS);
            Settings settings = BaritoneAPI.getSettings();
            settings.allowBreak.value = false;
            settings.allowBreakAnyway.value = new ArrayList<>(allowed);
        }
        this.currentGoal = null;
        baritone().getMineProcess().mine(Math.max(0, quantity), blocks);
    }

    @Override
    public synchronized void setMineBounds(BlockPos min, BlockPos max) {
        Settings settings = BaritoneAPI.getSettings();
        removeMineBoundsSelection();
        if (min == null || max == null) {
            settings.mineRegionMinX.value = Integer.MIN_VALUE;
            settings.mineRegionMinY.value = Integer.MIN_VALUE;
            settings.mineRegionMinZ.value = Integer.MIN_VALUE;
            settings.mineRegionMaxX.value = Integer.MAX_VALUE;
            settings.mineRegionMaxY.value = Integer.MAX_VALUE;
            settings.mineRegionMaxZ.value = Integer.MAX_VALUE;
            if (this.snapshot != null) {
                settings.renderSelection.value = this.snapshot.renderSelection;
                settings.colorSelection.value = this.snapshot.colorSelection;
                settings.colorSelectionPos1.value = this.snapshot.colorSelectionPos1;
                settings.colorSelectionPos2.value = this.snapshot.colorSelectionPos2;
            }
            return;
        }
        settings.mineRegionMinX.value = Math.min(min.getX(), max.getX());
        settings.mineRegionMinY.value = Math.min(min.getY(), max.getY());
        settings.mineRegionMinZ.value = Math.min(min.getZ(), max.getZ());
        settings.mineRegionMaxX.value = Math.max(min.getX(), max.getX());
        settings.mineRegionMaxY.value = Math.max(min.getY(), max.getY());
        settings.mineRegionMaxZ.value = Math.max(min.getZ(), max.getZ());
        settings.renderSelection.value = true;
        this.mineBoundsSelection = baritone().getSelectionManager().addSelection(
                BetterBlockPos.from(min),
                BetterBlockPos.from(max)
        );
    }

    @Override
    public synchronized void setMineRenderColor(int argb) {
        if (!this.active) {
            return;
        }
        Color color = new Color(argb, true);
        Settings settings = BaritoneAPI.getSettings();
        settings.colorSelection.value = color;
        settings.colorSelectionPos1.value = color;
        settings.colorSelectionPos2.value = color;
    }

    @Override
    public synchronized void setMineAoeLevel(int level) {
        if (!this.active) {
            return;
        }
        BaritoneAPI.getSettings().mineAoeLevel.value = Math.clamp(level, 0, 2);
    }

    @Override
    public boolean isPathing() {
        return isAvailable() && baritone().getPathingBehavior().isPathing();
    }

    @Override
    public boolean isMining() {
        return isAvailable() && baritone().getMineProcess().isActive();
    }

    @Override
    public List<BlockPos> miningTargets() {
        if (!isAvailable()) {
            return List.of();
        }
        try {
            return List.copyOf(baritone().getMineProcess().knownTargets());
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    @Override
    public Optional<Double> estimatedTicksToGoal() {
        if (!isAvailable()) {
            return Optional.empty();
        }
        return baritone().getPathingBehavior().estimatedTicksToGoal();
    }

    @Override
    public synchronized Optional<BlockPos> currentGoal() {
        return Optional.ofNullable(this.currentGoal);
    }

    @Override
    public synchronized String diagnostics() {
        if (!isAvailable()) {
            return "available=false";
        }
        Settings settings = BaritoneAPI.getSettings();
        IBaritone baritone = baritone();
        List<BlockPos> miningTargets = miningTargets();
        return String.format(
                Locale.ROOT,
                "available=true session=%s mineActive=%s pathing=%s hasPath=%s "
                        + "calculating=%s goal=%s customGoal=%s allowBreak=%s "
                        + "allowPlace=%s allowRightClick=%s sprint=%s "
                        + "freeLook=%s blockFreeLook=%s radius=%d aoe=%d "
                        + "scanDrops=%s goalUpdate=%d maxTargets=%d "
                        + "dropLoiterMs=%d explore=%s "
                        + "chunkCaching=%s renderCachedChunks=%s "
                        + "renderPath=%s renderGoal=%s renderSelections=%s "
                        + "allowedBreakAnyway=%s protectedBlocks=%s "
                        + "mineBounds=[%d,%d,%d -> %d,%d,%d] "
                        + "knownMineTargets=%d targetSample=%s",
                this.active,
                baritone.getMineProcess().isActive(),
                baritone.getPathingBehavior().isPathing(),
                baritone.getPathingBehavior().hasPath(),
                baritone.getPathingBehavior().getInProgress().isPresent(),
                baritone.getPathingBehavior().getGoal(),
                this.currentGoal,
                settings.allowBreak.value,
                settings.allowPlace.value,
                settings.allowRightClick.value,
                settings.allowSprint.value,
                settings.freeLook.value,
                settings.blockFreeLook.value,
                settings.mineSearchRadius.value,
                settings.mineAoeLevel.value,
                settings.mineScanDroppedItems.value,
                settings.mineGoalUpdateInterval.value,
                settings.mineMaxOreLocationsCount.value,
                settings.mineDropLoiterDurationMSThanksLouca.value,
                settings.exploreForBlocks.value,
                settings.chunkCaching.value,
                settings.renderCachedChunks.value,
                settings.renderPath.value,
                settings.renderGoal.value,
                settings.renderSelectionBoxes.value,
                settings.allowBreakAnyway.value,
                settings.blocksToDisallowBreaking.value,
                settings.mineRegionMinX.value,
                settings.mineRegionMinY.value,
                settings.mineRegionMinZ.value,
                settings.mineRegionMaxX.value,
                settings.mineRegionMaxY.value,
                settings.mineRegionMaxZ.value,
                miningTargets.size(),
                miningTargets.stream().limit(8).toList()
        );
    }

    @Override
    public synchronized void cancel() {
        if (isAvailable()) {
            baritone().getPathingBehavior().cancelEverything();
        }
        this.currentGoal = null;
    }

    @Override
    public synchronized void end() {
        removeMineBoundsSelection();
        cancel();
        if (this.snapshot != null) {
            this.snapshot.restore(BaritoneAPI.getSettings());
        }
        this.snapshot = null;
        this.active = false;
        this.strictBreakWhitelist = false;
    }

    private void requireActive() {
        if (!this.active) {
            throw new IllegalStateException("Navigation session has not started");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("Baritone is unavailable");
        }
    }

    private static IBaritone baritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    private void removeMineBoundsSelection() {
        if (this.mineBoundsSelection == null) {
            return;
        }
        if (isAvailable()) {
            baritone().getSelectionManager().removeSelection(this.mineBoundsSelection);
        }
        this.mineBoundsSelection = null;
    }

    private record SettingsSnapshot(
            boolean allowBreak,
            boolean allowPlace,
            boolean allowSprint,
            boolean freeLook,
            boolean blockFreeLook,
            boolean scanDroppedItems,
            int mineSearchRadius,
            int mineAoeLevel,
            int mineRegionMinX,
            int mineRegionMinY,
            int mineRegionMinZ,
            int mineRegionMaxX,
            int mineRegionMaxY,
            int mineRegionMaxZ,
            boolean allowRightClick,
            int mineGoalUpdateInterval,
            int mineMaxOreLocationsCount,
            long mineDropLoiterMillis,
            boolean exploreForBlocks,
            List<Block> allowBreakAnyway,
            List<Block> blocksToDisallowBreaking,
            boolean chatControl,
            boolean chatControlAnyway,
            boolean renderPath,
            boolean renderGoal,
            boolean renderGoalXZBeacon,
            boolean renderSelectionBoxes,
            boolean renderSelection,
            Color colorSelection,
            Color colorSelectionPos1,
            Color colorSelectionPos2,
            boolean chunkCaching,
            boolean renderCachedChunks
    ) {
        private static SettingsSnapshot capture(Settings settings) {
            return new SettingsSnapshot(
                    settings.allowBreak.value,
                    settings.allowPlace.value,
                    settings.allowSprint.value,
                    settings.freeLook.value,
                    settings.blockFreeLook.value,
                    settings.mineScanDroppedItems.value,
                    settings.mineSearchRadius.value,
                    settings.mineAoeLevel.value,
                    settings.mineRegionMinX.value,
                    settings.mineRegionMinY.value,
                    settings.mineRegionMinZ.value,
                    settings.mineRegionMaxX.value,
                    settings.mineRegionMaxY.value,
                    settings.mineRegionMaxZ.value,
                    settings.allowRightClick.value,
                    settings.mineGoalUpdateInterval.value,
                    settings.mineMaxOreLocationsCount.value,
                    settings.mineDropLoiterDurationMSThanksLouca.value,
                    settings.exploreForBlocks.value,
                    List.copyOf(settings.allowBreakAnyway.value),
                    List.copyOf(settings.blocksToDisallowBreaking.value),
                    settings.chatControl.value,
                    settings.chatControlAnyway.value,
                    settings.renderPath.value,
                    settings.renderGoal.value,
                    settings.renderGoalXZBeacon.value,
                    settings.renderSelectionBoxes.value,
                    settings.renderSelection.value,
                    settings.colorSelection.value,
                    settings.colorSelectionPos1.value,
                    settings.colorSelectionPos2.value,
                    settings.chunkCaching.value,
                    settings.renderCachedChunks.value
            );
        }

        private void restore(Settings settings) {
            settings.allowBreak.value = this.allowBreak;
            settings.allowPlace.value = this.allowPlace;
            settings.allowSprint.value = this.allowSprint;
            settings.freeLook.value = this.freeLook;
            settings.blockFreeLook.value = this.blockFreeLook;
            settings.mineScanDroppedItems.value = this.scanDroppedItems;
            settings.mineSearchRadius.value = this.mineSearchRadius;
            settings.mineAoeLevel.value = this.mineAoeLevel;
            restoreMineBounds(settings, this);
            settings.allowRightClick.value = this.allowRightClick;
            settings.mineGoalUpdateInterval.value = this.mineGoalUpdateInterval;
            settings.mineMaxOreLocationsCount.value =
                    this.mineMaxOreLocationsCount;
            settings.mineDropLoiterDurationMSThanksLouca.value = this.mineDropLoiterMillis;
            settings.exploreForBlocks.value = this.exploreForBlocks;
            settings.allowBreakAnyway.value = new ArrayList<>(this.allowBreakAnyway);
            settings.blocksToDisallowBreaking.value =
                    new ArrayList<>(this.blocksToDisallowBreaking);
            settings.chatControl.value = this.chatControl;
            settings.chatControlAnyway.value = this.chatControlAnyway;
            settings.renderPath.value = this.renderPath;
            settings.renderGoal.value = this.renderGoal;
            settings.renderGoalXZBeacon.value = this.renderGoalXZBeacon;
            settings.renderSelectionBoxes.value = this.renderSelectionBoxes;
            settings.renderSelection.value = this.renderSelection;
            settings.colorSelection.value = this.colorSelection;
            settings.colorSelectionPos1.value = this.colorSelectionPos1;
            settings.colorSelectionPos2.value = this.colorSelectionPos2;
            settings.chunkCaching.value = this.chunkCaching;
            settings.renderCachedChunks.value = this.renderCachedChunks;
        }
    }

    private static void restoreMineBounds(Settings settings, SettingsSnapshot snapshot) {
        settings.mineRegionMinX.value = snapshot.mineRegionMinX;
        settings.mineRegionMinY.value = snapshot.mineRegionMinY;
        settings.mineRegionMinZ.value = snapshot.mineRegionMinZ;
        settings.mineRegionMaxX.value = snapshot.mineRegionMaxX;
        settings.mineRegionMaxY.value = snapshot.mineRegionMaxY;
        settings.mineRegionMaxZ.value = snapshot.mineRegionMaxZ;
    }
}
