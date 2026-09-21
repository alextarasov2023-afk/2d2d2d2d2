package org.alexdlc.utils.render.target;

import net.minecraft.world.entity.LivingEntity;
import org.alexdlc.feature.impl.combat.AuraFeature;
import org.alexdlc.feature.impl.combat.TriggerBotFeature;

public final class TargetMarkers {
    private final AuraMarkerRenderer marker = new AuraMarkerRenderer();
    private final GhostTargetRenderer ghost = new GhostTargetRenderer();
    private final CircleTargetRenderer circle = new CircleTargetRenderer();
    private final DeadheadTargetRenderer deadhead = new DeadheadTargetRenderer();

    public void render(AuraFeature feature, float tickDelta) {

        LivingEntity target = feature != null && feature.isEnabled() ? feature.getCurrentTarget() : null;
        if (target == null) {
            TriggerBotFeature triggerBot = TriggerBotFeature.getEnabled();
            if (triggerBot != null) {
                target = triggerBot.getCurrentTarget();
            }
        }
        boolean ghosts = feature != null && feature.usesGhostTargetEsp();
        boolean circles = feature != null && feature.usesCircleTargetEsp();
        boolean deadheads = feature != null && feature.usesDeadheadTargetEsp();
        int color = feature != null ? feature.getMarkerColor() : 0xFF00FF88;
        this.marker.render(ghosts || circles || deadheads ? null : target, tickDelta, color);
        this.ghost.render(ghosts ? target : null, tickDelta, color);
        this.circle.render(circles ? target : null, tickDelta, color);
        this.deadhead.render(deadheads ? target : null, tickDelta, color);
    }

    public void release() {
        this.marker.release();
        this.deadhead.release();
    }
}
