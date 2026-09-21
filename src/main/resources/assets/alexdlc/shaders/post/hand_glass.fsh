#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D SceneSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform HandGlassUniforms {
    vec4 GlassColor;
    float Mirror;
};

void main() {
    float maskAlpha = texture(MaskSampler, uv).a;
    if (maskAlpha < 0.01) {
        discard;
    }

    vec2 texel = 1.0 / vec2(textureSize(MaskSampler, 0));
    float left = texture(MaskSampler, uv - vec2(texel.x * 2.0, 0.0)).a;
    float right = texture(MaskSampler, uv + vec2(texel.x * 2.0, 0.0)).a;
    float down = texture(MaskSampler, uv - vec2(0.0, texel.y * 2.0)).a;
    float up = texture(MaskSampler, uv + vec2(0.0, texel.y * 2.0)).a;
    float rim = clamp(length(vec2(right - left, up - down)) * 2.5, 0.0, 1.0);

    // Chromatic dispersion: the scene is split into slightly offset R/B taps
    // along the silhouette gradient, which sells the "thick glass" look.
    vec2 axis = normalize(vec2(right - left, up - down) + vec2(1e-5, 0.0));
    float dispersion = 0.004 + rim * 0.008;

    vec2 baseUv = uv;
    vec2 redUv = uv - axis * dispersion;
    vec2 blueUv = uv + axis * dispersion;
    if (Mirror > 0.5) {
        baseUv.x = 1.0 - baseUv.x;
        redUv.x = 1.0 - redUv.x;
        blueUv.x = 1.0 - blueUv.x;
    }

    vec3 scene;
    scene.r = texture(SceneSampler, redUv).r;
    scene.g = texture(SceneSampler, baseUv).g;
    scene.b = texture(SceneSampler, blueUv).b;

    float tintAmount = clamp(GlassColor.a, 0.0, 1.0) * 0.5;
    vec3 tinted = mix(scene, GlassColor.rgb * (0.85 + rim * 0.5), tintAmount);
    tinted += GlassColor.rgb * rim * 0.2;
    finalColor = vec4(clamp(tinted, 0.0, 1.0), clamp(maskAlpha, 0.0, 1.0));
}

