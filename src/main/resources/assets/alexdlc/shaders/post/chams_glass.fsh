#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D SceneSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    vec2 texel = 1.0 / vec2(textureSize(MaskSampler, 0));
    float left = texture(MaskSampler, uv - vec2(texel.x * 3.0, 0.0)).a;
    float right = texture(MaskSampler, uv + vec2(texel.x * 3.0, 0.0)).a;
    float down = texture(MaskSampler, uv - vec2(0.0, texel.y * 3.0)).a;
    float up = texture(MaskSampler, uv + vec2(0.0, texel.y * 3.0)).a;
    vec2 gradient = vec2(right - left, up - down);

    // Thicker glass near silhouette edges, thin and clear in the middle.
    float rim = clamp(length(gradient) * 3.0, 0.0, 1.0);
    vec2 base = uv - gradient * 0.14;
    vec2 axis = normalize(gradient + vec2(1e-5));

    // Chromatic dispersion splits the scene into slightly offset R/B taps.
    float dispersion = 0.006 + rim * 0.010;
    vec2 redUv = base - axis * dispersion;
    vec2 blueUv = base + axis * dispersion;
    if (ChamsParams.y > 0.5) {
        base.x = 1.0 - base.x;
        redUv.x = 1.0 - redUv.x;
        blueUv.x = 1.0 - blueUv.x;
    }

    vec3 scene;
    scene.r = texture(SceneSampler, redUv).r;
    scene.g = texture(SceneSampler, base).g;
    scene.b = texture(SceneSampler, blueUv).b;

    vec3 glass = mix(
        scene,
        ChamsColor.rgb * (0.8 + rim * 0.6),
        clamp(ChamsColor.a, 0.0, 1.0) * 0.35
    );
    glass += ChamsColor.rgb * rim * 0.25;
    finalColor = vec4(clamp(glass, 0.0, 1.0), clamp(mask, 0.0, 1.0));
}

