package org.alexdlc.utils.render.world;

import org.alexdlc.feature.impl.combat.AuraFeature;
import org.alexdlc.feature.impl.player.FullBrightFeature;
import org.alexdlc.feature.impl.visual.BlockOutlineFeature;
import org.alexdlc.feature.impl.visual.ChamsFeature;
import org.alexdlc.feature.impl.visual.HitParticlesFeature;
import org.alexdlc.feature.impl.visual.JumpCirclesFeature;
import org.alexdlc.feature.impl.visual.ParticleRainFeature;
import org.alexdlc.feature.impl.visual.PopChamsFeature;
import org.alexdlc.feature.impl.visual.ShaderSkyFeature;
import org.alexdlc.feature.impl.visual.TrajectoriesFeature;
import org.alexdlc.feature.impl.visual.WorldParticlesFeature;
import org.alexdlc.utils.render.chams.ChamsPipeline;
import org.alexdlc.utils.render.chams.PopChamsRenderer;
import org.alexdlc.utils.render.target.TargetMarkers;

import java.util.ArrayList;
import java.util.List;

public final class WorldEffects {
    private static final List<WorldEffect> EFFECTS = new ArrayList<>();

    private WorldEffects() {
    }

    public static void bootstrap() {
        if (!EFFECTS.isEmpty()) {
            return;
        }

        register(WorldEffect.lazy(
                ShaderSkyFeature::getEnabled,
                WorldSkyRenderer::new,
                (feature, renderer, context) -> renderer.render(feature, context.cameraRenderState()),
                WorldSkyRenderer::release
        ));

        register(WorldEffect.lazy(
                () -> gate(FullBrightFeature.getEnabled(), FullBrightFeature::usesShaderLights),
                DynamicLightRenderer::new,
                (feature, renderer, context) -> renderer.render(feature, context.cameraRenderState(), context.tickDelta()),
                DynamicLightRenderer::release
        ));

        register(WorldEffect.lazy(
                () -> gate(BlockOutlineFeature.getEnabled(), BlockOutlineFeature::usesShader),
                BlockOutlineRenderer::new,
                (feature, renderer, context) -> renderer.render(feature, context.cameraRenderState()),
                BlockOutlineRenderer::release
        ));

        register(WorldEffect.lazy(
                ChamsFeature::getEnabled,
                ChamsPipeline::new,
                (feature, pipeline, context) -> pipeline.render(context.levelRenderState(), feature),
                ChamsPipeline::release
        ));

        register(WorldEffect.direct(
                JumpCirclesFeature::getEnabled,
                (feature, context) -> feature.renderWorld()
        ));

        register(WorldEffect.direct(
                TrajectoriesFeature::getEnabled,
                (feature, context) -> feature.renderWorld()
        ));

        register(WorldEffect.direct(
                WorldParticlesFeature::getEnabled,
                (feature, context) -> feature.renderWorld(context.levelRenderState().cameraRenderState, context.tickDelta())
        ));

        register(WorldEffect.lazy(
                PopChamsFeature::getEnabled,
                PopChamsRenderer::new,
                (feature, renderer, context) -> renderer.render(context.levelRenderState(), feature),
                PopChamsRenderer::release
        ));

        register(WorldEffect.direct(
                HitParticlesFeature::getEnabled,
                (feature, context) -> feature.renderWorld(context.tickDelta())
        ));

        register(WorldEffect.lazy(
                AuraFeature::getMarkerFeature,
                TargetMarkers::new,
                (feature, markers, context) -> markers.render(feature, context.tickDelta()),
                TargetMarkers::release
        ));

        register(WorldEffect.lazy(
                ParticleRainFeature::getEnabled,
                ParticleRainRenderer::new,
                (feature, renderer, context) -> renderer.render(feature),
                ParticleRainRenderer::release
        ));
    }

    public static void register(WorldEffect effect) {
        EFFECTS.add(effect);
    }

    public static void render(WorldEffectContext context) {
        for (WorldEffect effect : EFFECTS) {
            if (effect.active()) {
                effect.render(context);
            } else {
                effect.release();
            }
        }
    }

    private static <F> F gate(F feature, java.util.function.Predicate<F> enabled) {
        return feature != null && enabled.test(feature) ? feature : null;
    }
}
