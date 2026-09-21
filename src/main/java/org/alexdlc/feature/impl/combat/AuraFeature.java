package org.alexdlc.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import org.alexdlc.context.RotationContext;
import org.alexdlc.utils.math.NoiseUtil;
import org.alexdlc.event.EventTarget;
import org.alexdlc.event.events.game.GameTickEvent;
import org.alexdlc.event.events.game.PlayerTickEvent;
import org.alexdlc.event.events.input.PlayerInputEvent;
import org.alexdlc.event.events.lifecycle.WorldLeaveEvent;
import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.FeatureManager;
import org.alexdlc.feature.setting.BindSetting;
import org.alexdlc.feature.setting.BooleanSetting;
import org.alexdlc.feature.setting.ModeSetting;
import org.alexdlc.feature.setting.ColorMode;
import org.alexdlc.feature.setting.ColorSetting;
import org.alexdlc.feature.setting.MultiSelectSetting;
import org.alexdlc.feature.setting.NumberSetting;
import org.alexdlc.utils.FriendManager;
import org.alexdlc.utils.combat.rotations.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AuraFeature extends Feature {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static LivingEntity target = null;
    public static double bpsTarget = 0.0D;

    public final ModeSetting componentMode = register(new ModeSetting(
            "Rotation Mode",
            "Legit",
            "Legit", "Плавный", "FunTime", "SpookyTime", "Reallyworld", "Droid", "HolyWorld", "PhotoGraf"
    ));

    public final MultiSelectSetting checks = register(new MultiSelectSetting(
            "Checks",
            List.of("Disable on death"),
            "Disable on death", "Don't eat & hit", "Weapon only", "TPSSync"
    ));

    private static final String TARGET_ESP_MARKER = "Marker";
    private static final String TARGET_ESP_GHOSTS = "Ghosts";
    private static final String TARGET_ESP_CIRCLE = "Circle";
    private static final String TARGET_ESP_DEADHEADS = "Deadheads";

    public final ModeSetting targetEsp = register(new ModeSetting(
            "Target ESP",
            TARGET_ESP_MARKER,
            TARGET_ESP_MARKER,
            TARGET_ESP_GHOSTS,
            TARGET_ESP_CIRCLE,
            TARGET_ESP_DEADHEADS
    ));

    public final ModeSetting targetEspColorMode = register(ColorMode.setting());
    public final ColorSetting targetEspColor = register(new ColorSetting(
            "Target ESP Color",
            0xFF00FF88
    ).visibleWhen(() -> ColorMode.isCustom(this.targetEspColorMode)));

    public final MultiSelectSetting targets = register(new MultiSelectSetting(
            "Targets",
            List.of("Players", "Naked"),
            "Players", "Friends", "Naked", "Animals", "Mobs"
    ));

    public final ModeSetting sortMode = register(new ModeSetting(
            "Sort Mode",
            "Всему сразу",
            "Всему сразу", "Дистанции", "Здоровью", "Броне"
    ));

    public final NumberSetting attackRange = register(new NumberSetting(
            "Attack Range", 3.0D, 2.5D, 6.0D, 0.1D, " blocks"
    ));

    public final NumberSetting rotateDistance = register(new NumberSetting(
            "Rotate Distance", 1.5D, 0.0D, 3.0D, 0.1D, " blocks"
    ));

    public final NumberSetting minAttackDelay = register(new NumberSetting(
            "Min Delay", 0.0D, 0.0D, 500.0D, 1.0D, " ms"
    ));

    public final NumberSetting maxAttackDelay = register(new NumberSetting(
            "Max Delay", 0.0D, 0.0D, 500.0D, 1.0D, " ms"
    ));

    public final BooleanSetting onlycrit = register(new BooleanSetting(
            "Only Criticals", true
    ));

    public final BooleanSetting onlySpaceCritical = register(new BooleanSetting(
            "Smart Criticals", false
    ));

    public final BooleanSetting bypassWalls = register(new BooleanSetting(
            "Bypass Walls RW", false
    ));

    public final BooleanSetting ray = register(new BooleanSetting(
            "Ray Check", false
    ).visibleWhen(() -> this.componentMode.is("SpookyTime")));

    public final ModeSetting correctionType = register(new ModeSetting(
            "Movement Correction",
            "Свободная",
            "Свободная", "Сфокусированная", "Фулл Таргет"
    ));


    public final BooleanSetting throughWalls = register(new BooleanSetting(
            "Through Walls", true
    ));

    public final BooleanSetting shielbreaker = register(new BooleanSetting(
            "Break Shield", true
    ));

    public final BooleanSetting SHEIS = register(new BooleanSetting(
            "Unblock Shield", true
    ));

    public final BooleanSetting clientLook = register(new BooleanSetting(
            "Client Look", false
    ));


    public boolean canCrit;
    private long cps = 0L;
    private int counter = 0;
    private int count = 0;
    private int rayTicks = 0;
    private int groundTicks = 0;
    private long rayTraceDisabledTime = -1L;
    private float lastYaw = 0.0F;
    private long attackDelayTime = 0L;
    private float lastPitch = 0.0F;
    private int sprintWaitTicks = 0;
    private boolean wasEating = false;
    private long lastEatingEndTime = -1L;
    private long currentEatingBoostDuration = 1500L;
    private Vec2 lerpRotation = Vec2.ZERO;
    private Vec2 lerpRot = Vec2.ZERO;

    private long stopWatch = System.currentTimeMillis();

    private float bezierT = 1.0F;
    private float strokeStartYaw = 0.0F;
    private float strokeStartPitch = 0.0F;
    private float strokeTargetYaw = 0.0F;
    private float strokeTargetPitch = 0.0F;
    private Vec2 controlPoint1 = Vec2.ZERO;
    private Vec2 controlPoint2 = Vec2.ZERO;

    private float muscleVelYaw = 0.0F;
    private float muscleVelPitch = 0.0F;

    private Vec3 smoothedTargetVel = Vec3.ZERO;
    private float currentFrictionScale = 1.0F;
    private float currentClickSyncScale = 1.0F;
    private float currentPitchSpeedScale = 1.0F;
    private float currentPlayerHealthScale = 1.0F;
    private float currentPanicTremorScale = 1.0F;
    private float currentTargetHealthScale = 1.0F;
    private float lastDeltaYaw = 0.0F;

    // --- ML Bypass State Fields ---
    private final java.util.LinkedList<net.minecraft.world.phys.Vec3> targetPositionHistory = new java.util.LinkedList<>();
    private float overshootYaw = 0.0F;
    private float driftOffsetX = 0.0F;
    private float driftOffsetZ = 0.0F;
    private float driftSpeedX = 0.0F;
    private float driftSpeedZ = 0.0F;
    private long lastTickTime = 0;
    private float tremorYawField = 0.0F;
    private float tremorPitchField = 0.0F;
    private float overshootPitch = 0.0F;

    private final LegitRotation legitRotation = new LegitRotation();
    private final FunTimeRotation funTimeRotation = new FunTimeRotation();
    private final SpookyTimeRotation spookyTimeRotation = new SpookyTimeRotation();
    private final HolyWorldRotation holyWorldRotation = new HolyWorldRotation();
    private final PhotoGrafRotation photoGrafRotation = new PhotoGrafRotation();
    private final ReallyworldRotation reallyworldRotation = new ReallyworldRotation();
    private final DroidRotation droidRotation = new DroidRotation();

    public AuraFeature() {
        super("Aura", "Automatically attacks selected entitiess", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    public static AuraFeature getEnabled() {
        return FeatureManager.INSTANCE.getEnabled(AuraFeature.class);
    }

    public static AuraFeature getMarkerFeature() {
        return FeatureManager.INSTANCE.getFeature(AuraFeature.class);
    }

    public LivingEntity getTarget() {
        return isEnabled() ? target : null;
    }

    public LivingEntity getCurrentTarget() {
        return getTarget();
    }

    public boolean usesGhostTargetEsp() {
        return TARGET_ESP_GHOSTS.equals(this.targetEsp.getValue());
    }

    public boolean usesCircleTargetEsp() {
        return TARGET_ESP_CIRCLE.equals(this.targetEsp.getValue());
    }

    public boolean usesDeadheadTargetEsp() {
        return TARGET_ESP_DEADHEADS.equals(this.targetEsp.getValue());
    }

    public boolean usesRhombusTargetEsp() {
        return false;
    }

    public int getMarkerColor() {
        return ColorMode.resolve(this.targetEspColorMode, this.targetEspColor);
    }

    public boolean shouldAutoJump(LocalPlayer player) {
        return isEnabled() && target != null && onlycrit.getValue();
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            lerpRotation = new Vec2(player.getYRot(), player.getXRot());
            lerpRot = new Vec2(player.getYRot(), player.getXRot());
        }
        stopWatch = System.currentTimeMillis();
        resetLegitRotationState();
    }

    @Override
    protected void onDisable() {
        target = null;
        counter = 9;
        lerpRotation = Vec2.ZERO;
        lerpRot = Vec2.ZERO;
        resetLegitRotationState();
        RotationContext.clear();
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {
        target = null;
        canCrit = false;
        rayTraceDisabledTime = -1L;
        RotationContext.clear();
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent event) {
        if (!event.isPre()) return;
        LocalPlayer player = event.getPlayer();
        if (player == null || Minecraft.getInstance().level == null) {
            canCrit = false;
            return;
        }

        canCrit = !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isInLava()
                && !player.isPassenger();
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (target == null || !isValidTarget(target)) {
            target = findTarget();
        }

        if (target == null) return;

        boolean rotateActive = RotationContext.isActive();
        if (this.correctionType.is("Свободная")) {
            // Only use free movement correction if the camera is NOT locked to the aura
            if (rotateActive && !this.clientLook.getValue()) {
                fixMovement(event, RotationContext.getFreeYaw());
            }
        } else if (this.correctionType.is("Фулл Таргет")) {
            if (!rotateActive) {
                updateRotation();
            }
            moveToPosition(event, target.position(), RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot());
        }
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = event.getClient();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            reset();
            return;
        }

        if (player.onGround()) {
            groundTicks++;
        } else {
            groundTicks = 0;
        }

        if (checks.isSelected("Disable on death") && !player.isAlive()) {
            setEnabled(false);
            return;
        }



        if (target != null) {
            Vec3 delta = target.position().subtract(new Vec3(target.xo, target.yo, target.zo));
            bpsTarget = delta.length() * 20.0D;
        }

        if (target == null || !isValidTarget(target)) {
            target = findTarget();
        }

        if (target == null) {
            reset();
            return;
        }

        // Delay queue for ML Reaction Time bypass (Delaying velocity perception)
        targetPositionHistory.addLast(target.getDeltaMovement());
        while (targetPositionHistory.size() > (int) randomLerp(5, 8)) { // 250-400ms reaction time jitter
            targetPositionHistory.removeFirst();
        }

        if (rayTrace()) {
            rayTicks++;
        } else {
            rayTicks = 0;
        }

        boolean shouldAttack = false;
        
        if (onlycrit.getValue() || onlySpaceCritical.getValue()) {
            if (canAttack()) {
                shouldAttack = true;
            }
        } else {
            if (player.getAttackStrengthScale(checks.isSelected("TPSSync") ? 1.0F : 0.0F) >= 0.85F) {
                if (componentMode.is("SpookyTime")) {
                    if (!ray.getValue() || rayTicks > 1) {
                        shouldAttack = true;
                    }
                } else {
                    shouldAttack = true;
                }
            }
        }

        if (shouldAttack) {
            long now = System.currentTimeMillis();
            if (attackDelayTime == 0L) {
                long minD = (long) minAttackDelay.getValue().doubleValue();
                long maxD = (long) maxAttackDelay.getValue().doubleValue();
                long delay = minD + (long) (Math.random() * Math.max(maxD - minD, 1));
                attackDelayTime = now + delay;
            }
            if (now >= attackDelayTime) {
                updateAttack();
                attackDelayTime = 0L;
            }
        } else {
            attackDelayTime = 0L;
        }

        updateRotation();
    }

    private void resetLegitRotationState() {
        bezierT = 1.0F;
        muscleVelYaw = 0.0F;
        muscleVelPitch = 0.0F;
        smoothedTargetVel = Vec3.ZERO;
        currentFrictionScale = 1.0F;
        currentClickSyncScale = 1.0F;
        currentPitchSpeedScale = 1.0F;
        currentPlayerHealthScale = 1.0F;
        currentPanicTremorScale = 1.0F;
        currentTargetHealthScale = 1.0F;
        legitRotation.reset();
    }

    private void updateRotation() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || target == null) return;

        boolean attackLikely = canAttack();
        Vec3 eyePos = player.getEyePosition(1.0F);

        switch (componentMode.getValue()) {
            case "FunTime" -> funTimeRotation.tick(player, target, target.getEyePosition(), attackLikely);
            case "SpookyTime" -> spookyTimeRotation.tick(player, target, target.getEyePosition(), attackLikely);
            case "HolyWorld" -> holyWorldRotation.tick(player, target, target.getEyePosition(), attackLikely);
            case "PhotoGraf" -> photoGrafRotation.tick(player, target, target.getEyePosition(), attackLikely);
            case "Reallyworld" -> reallyworldRotation.tick(player, target, target.getEyePosition(), attackLikely);
            case "Droid" -> droidRotation.tick(player, target, target.getEyePosition(), attackLikely);
            case "Плавный" -> updateSmoothRotation(player, eyePos);
            default -> legitRotation.tick(player, target, target.getEyePosition(), attackLikely);
        }
    }



    private void updateSmoothRotation(LocalPlayer player, Vec3 eyePos) {
        Vec3 targetPos = target.position();
        double targetY = targetPos.y + target.getBbHeight() * 0.5D;
        Vec3 aimVec = new Vec3(targetPos.x, targetY, targetPos.z).subtract(eyePos);
        double horizDist = Math.hypot(aimVec.x, aimVec.z);

        float rawYaw = (float) Math.toDegrees(Math.atan2(-aimVec.x, aimVec.z));
        float rawPitch = (float) Math.clamp(-Math.toDegrees(Math.atan2(aimVec.y, horizDist)), -90.0D, 90.0D);

        float currentYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
        float currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();

        float deltaYaw = wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        float gcd = getGCDValue();
        float targetYaw = currentYaw + deltaYaw;
        float targetPitch = currentPitch + deltaPitch;

        float deltaTargetYaw = wrapDegrees(targetYaw - currentYaw);
        float deltaTargetPitch = targetPitch - currentPitch;

        float finalDeltaYaw = deltaTargetYaw - (deltaTargetYaw % gcd);
        float finalDeltaPitch = deltaTargetPitch - (deltaTargetPitch % gcd);

        RotationContext.setRotation(currentYaw + finalDeltaYaw, Math.clamp(currentPitch + finalDeltaPitch, -90.0F, 90.0F));
    }

    private void fastRotation(float targetYaw, float targetPitch) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || target == null) return;

        float currentYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
        float currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();

        float deltaYaw = wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = Math.clamp(targetPitch - currentPitch, -90.0F, 90.0F);

        float noiseYaw = (float) random(-3.0D, 3.0D);
        float noisePitch = (float) random(-2.0D, 2.0D);

        RotationContext.setRotation(currentYaw + deltaYaw + noiseYaw, Math.clamp(currentPitch + deltaPitch + noisePitch, -90.0F, 90.0F));
    }

    private void updateAttack() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || target == null) return;

        boolean isLegit = componentMode.is("Legit") || componentMode.is("Плавный");
        boolean checkRay = (!this.ray.getValue() && !isLegit) || rayTrace();
        boolean inRange = checkRay && getStrictDistance(target) <= attackDistance();

        // Disable sprint entirely while target is in range
        if (inRange && player.isSprinting()) {
            player.setSprinting(false);
            org.alexdlc.utils.combat.SprintManager.markAttackImminent();
        }

        if (checkReturn()) return;

        if (inRange) {

            if (bypassWalls.getValue()) {
                Vec3 startVec = player.getEyePosition();
                Vec3 targetPos = target.getEyePosition();
                Vec3 direction = targetPos.subtract(startVec);
                double distance = direction.length();

                if (distance >= 1.0E-3) {
                    Vec3 normalizedDir = direction.normalize();
                    for (double i = 0.0D; i < distance; i += 0.5D) {
                        Vec3 point = startVec.add(normalizedDir.scale(i));
                        BlockPos pos = BlockPos.containing(point);
                        if (mc.level != null && !mc.level.getBlockState(pos).isAir()) {
                            if (player.connection != null) {
                                player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                                player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
                            }
                        }
                    }
                }
            }

            if (target instanceof Player targetPlayer) {
                if (this.shielbreaker.getValue()) {
                    breakShieldPlayer(targetPlayer);
                }
            }

            if (this.SHEIS.getValue() && player.isUsingItem() && player.getUseItem().getItem() == Items.SHIELD) {
                if (mc.gameMode != null) {
                    mc.gameMode.releaseUsingItem(player);
                }
            }

            attackEntity(target);
            stopWatch = System.currentTimeMillis();

            org.alexdlc.utils.combat.SprintManager.reset();
            org.alexdlc.feature.impl.movement.SprintFeature sprintFeature = org.alexdlc.feature.impl.movement.SprintFeature.getEnabled();
            if (sprintFeature != null && sprintFeature.keepSprint.getValue()) {
                player.setSprinting(true);
            }

            count = (count + 1) % 2;
            counter++;
            canCrit = false;
        }
    }

    private void breakShieldPlayer(Player entity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        if (entity.isBlocking()) {
            int axeSlot = findAxeSlot(player);
            if (axeSlot != -1) {
                int oldSlot = player.getInventory().getSelectedSlot();
                if (axeSlot < 9) {
                    player.getInventory().setSelectedSlot(axeSlot);
                    mc.gameMode.attack(player, entity);
                    player.swing(InteractionHand.MAIN_HAND);
                    player.getInventory().setSelectedSlot(oldSlot);
                } else {
                    mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, axeSlot, oldSlot, ContainerInput.SWAP, player);
                    mc.gameMode.attack(player, entity);
                    player.swing(InteractionHand.MAIN_HAND);
                    mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, axeSlot, oldSlot, ContainerInput.SWAP, player);
                }
            }
        }
    }

    private int findAxeSlot(LocalPlayer player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.AXES)) {
                return i;
            }
        }
        return -1;
    }

    private boolean checkReturn() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return true;
        boolean eating = player.isUsingItem();
        boolean noWeapon = !(player.getMainHandItem().is(ItemTags.AXES) || player.getMainHandItem().is(ItemTags.SWORDS));
        return (eating && checks.isSelected("Don't eat & hit")) || (noWeapon && checks.isSelected("Weapon only"));
    }

    private void attackEntity(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        mc.gameMode.attack(player, entity);
        player.swing(InteractionHand.MAIN_HAND);
        org.alexdlc.utils.render.target.DeadheadTargetRenderer.triggerBite();
    }

    public boolean canAttack() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || checkReturn()) return false;

        boolean attackReady = player.getAttackStrengthScale(checks.isSelected("TPSSync") ? 1.0F : 0.0F) >= 0.85F;
        if (!attackReady) return false;

        boolean isJumpPressed = (player.input != null && player.input.keyPresses.jump()) || mc.options.keyJump.isDown();
        boolean wantCrit = onlycrit.getValue() || (onlySpaceCritical.getValue() && isJumpPressed);

        if (wantCrit) {
            if (player.onGround()) return false;
            
            // Trigger as soon as the player begins falling downward
            boolean isFalling = player.fallDistance > 0.0F && player.getDeltaMovement().y < 0.0D;
            boolean canCritEnvironment = !player.onClimbable() && !player.isInWater() && !player.isInLava() && !player.isPassenger();
            
            return isFalling && canCritEnvironment;
        }

        return true;
    }

    private LivingEntity findTarget() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return null;

        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                potentialTargets.add(living);
            }
        }

        if (potentialTargets.isEmpty()) {
            return null;
        }

        if (potentialTargets.size() > 1) {
            switch (sortMode.getValue()) {
                case "Всему сразу" -> potentialTargets.sort(Comparator.comparingDouble((LivingEntity target) -> {
                    if (target instanceof Player p) return -getEntityArmor(p);
                    return -target.getArmorValue();
                }).thenComparing((o1, o2) -> Double.compare(getEntityHealth(o1), getEntityHealth(o2)))
                  .thenComparing((o1, o2) -> Double.compare(player.distanceTo(o1), player.distanceTo(o2))));

                case "Дистанции" -> potentialTargets.sort(Comparator.comparingDouble(player::distanceTo));
                case "Броне" -> potentialTargets.sort(Comparator.comparingDouble(entity -> entity instanceof Player p ? getEntityArmor(p) : entity.getArmorValue()));
                case "Здоровью" -> potentialTargets.sort(Comparator.comparingDouble(this::getEntityHealth));
            }
        }

        return potentialTargets.get(0);
    }

    private boolean isValidTarget(LivingEntity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player) return false;
        if (entity.tickCount < 3) return false;
        if (getStrictDistance(entity) >= getMaxAimRange()) return false;

        if (entity instanceof Player playerEntity) {
            if (!targets.isSelected("Friends") && FriendManager.INSTANCE.isFriend(playerEntity.getGameProfile().name())) {
                return false;
            }
            if (playerEntity.getGameProfile().name().equalsIgnoreCase(player.getGameProfile().name())) {
                return false;
            }
            if (playerEntity.getArmorValue() == 0 && !targets.isSelected("Naked")) return false;
            if (!targets.isSelected("Players")) return false;
        }

        if ((entity instanceof Monster || entity instanceof Phantom || entity instanceof Bat || entity instanceof Shulker || entity instanceof Villager) && !targets.isSelected("Mobs")) {
            return false;
        }

        if ((entity instanceof Animal || entity instanceof AmbientCreature) && !targets.isSelected("Animals")) {
            return false;
        }

        return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStand);
    }

    public double getEntityArmor(Player targetPlayer) {
        return targetPlayer.getArmorValue();
    }

    public double getEntityHealth(Entity ent) {
        if (ent instanceof Player p) {
            double armorFactor = getEntityArmor(p) / 20.0D;
            return (p.getHealth() + p.getAbsorptionAmount()) * armorFactor;
        } else if (ent instanceof LivingEntity living) {
            return living.getHealth() + living.getAbsorptionAmount();
        }
        return 0.0D;
    }

    public boolean rayTrace() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) return false;

        Vec3 eye = player.getEyePosition();
        float serverYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
        float serverPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
        Vec3 look = Vec3.directionFromRotation(serverPitch, serverYaw);
        Vec3 end = eye.add(look.scale(attackDistance()));

        AABB box = target.getBoundingBox();
        return box.contains(eye) || box.clip(eye, end).isPresent();
    }

    public double getMaxAimRange() {
        double attackDist = attackRange.getValue();
        double rotateDist = rotateDistance.getValue();
        return attackDist + rotateDist;
    }

    public double attackDistance() {
        return attackRange.getValue();
    }

    public double getStrictDistance(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0.0D;
        return player.distanceTo(entity);
    }

    private void reset() {
        target = null;
        canCrit = false;
        rayTraceDisabledTime = -1L;
        targetPositionHistory.clear();
        legitRotation.reset();
        RotationContext.disengage();
    }

    private static float wrapDegrees(float angle) {
        float wrapped = angle % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    private static float lerp(float start, float end, double t) {
        return (float) (start + t * (end - start));
    }

    private static float wrapLerp(float step, float input, float target) {
        return input + step * wrapDegrees(target - input);
    }

    private static float randomLerp(float min, float max) {
        return min + SECURE_RANDOM.nextFloat() * (max - min);
    }

    private static double random(double min, double max) {
        return min + Math.random() * (max - min);
    }

    private float getGCDValue() {
        float sensitivity = Minecraft.getInstance().options.sensitivity().get().floatValue() * 0.6F + 0.2F;
        return sensitivity * sensitivity * sensitivity * 8.0F * 0.15F;
    }

    private float cooldownFromLastSwing() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 1.0F;
        return Math.clamp(player.getAttackStrengthScale(0.5F), 0.0F, 1.0F);
    }

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0D) rotationYaw += 180.0F;

        float forward = 1.0F;
        if (moveForward < 0.0D) forward = -0.5F;
        else if (moveForward > 0.0D) forward = 0.5F;

        if (moveStrafing > 0.0D) rotationYaw -= 90.0F * forward;
        if (moveStrafing < 0.0D) rotationYaw += 90.0F * forward;

        return Math.toRadians(rotationYaw);
    }

    private void fixMovement(PlayerInputEvent event, float targetYaw) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Vec2 moveVec = event.getMoveVector();
        float forward = moveVec.y;
        float strafe = moveVec.x;

        if (forward == 0.0F && strafe == 0.0F) return;

        double angle = wrapDegrees((float) Math.toDegrees(direction(targetYaw, forward, strafe)));
        float serverYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();

        float closestForward = 0.0F, closestStrafe = 0.0F;
        double closestDifference = Double.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                if (predictedStrafe == 0.0F && predictedForward == 0.0F) continue;
                double predictedAngle = wrapDegrees((float) Math.toDegrees(direction(serverYaw, predictedForward, predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);
                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setMoveVector(new Vec2(closestStrafe, closestForward));

        boolean fwd = closestForward > 0.0F;
        boolean bwd = closestForward < 0.0F;
        boolean left = closestStrafe > 0.0F;
        boolean right = closestStrafe < 0.0F;
        net.minecraft.world.entity.player.Input in = event.getKeyPresses();
        event.setKeyPresses(new net.minecraft.world.entity.player.Input(
                fwd, bwd, left, right,
                in.jump(), in.shift(), in.sprint()
        ));
    }

    private void moveToPosition(PlayerInputEvent event, Vec3 position, float currentYaw) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        double deltaX = position.x - player.getX();
        double deltaZ = position.z - player.getZ();

        double angleToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        angleToTarget = wrapDegrees((float) angleToTarget);

        float bestForward = 0.0F;
        float bestStrafe = 0.0F;
        double minDifference = Double.MAX_VALUE;

        for (float forward = -1.0F; forward <= 1.0F; forward += 1.0F) {
            for (float strafe = -1.0F; strafe <= 1.0F; strafe += 1.0F) {
                if (forward == 0.0F && strafe == 0.0F) continue;

                double moveAngle = Math.toDegrees(direction(currentYaw, forward, strafe));
                double difference = Math.abs(angleToTarget - wrapDegrees((float) moveAngle));

                if (difference < minDifference) {
                    minDifference = difference;
                    bestForward = forward;
                    bestStrafe = strafe;
                }
            }
        }

        event.setMoveVector(new Vec2(bestStrafe, bestForward));

        boolean fwd = bestForward > 0.0F;
        boolean bwd = bestForward < 0.0F;
        boolean left = bestStrafe > 0.0F;
        boolean right = bestStrafe < 0.0F;
        net.minecraft.world.entity.player.Input in = event.getKeyPresses();
        event.setKeyPresses(new net.minecraft.world.entity.player.Input(
                fwd, bwd, left, right,
                in.jump(), in.shift(), in.sprint()
        ));
    }
}
