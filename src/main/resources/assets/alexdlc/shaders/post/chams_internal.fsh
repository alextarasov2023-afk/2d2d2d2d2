#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D BlurredSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

void main() {
    float mask = texture(MaskSampler, uv).a;
    float silhouette = smoothstep(0.1, 0.55, mask);
    if (silhouette < 0.005) {
        discard;
    }
    float blurred = texture(BlurredSampler, uv).a;
    float time = ChamsParams.x;
    float depth = smoothstep(0.5, 1.0, blurred);
    float fresnel = 1.0 - depth;

    // Hologram body: horizontal scan bands, a data sweep and rim energy.
    float scan = 0.5 + 0.5 * sin(uv.y * 220.0 - time * 6.0);
    float sweep = smoothstep(
        0.75,
        1.0,
        sin(uv.x * 3.0 - time * 1.4) * 0.5 + 0.5
    );
    float bands = smoothstep(0.35, 0.95, scan);
    float flicker = 0.86 + 0.14 * sin(time * 27.0 + uv.y * 40.0);

    float energy = clamp(
        fresnel * 0.75 + bands * 0.55 + sweep * 0.4,
        0.0,
        1.5
    ) * flicker;
    vec3 cool = ChamsColor.rgb * 0.35;
    vec3 hot = mix(ChamsColor.rgb, vec3(0.75, 0.98, 1.0), 0.45) * 1.25;
    vec3 holo = mix(cool, hot, clamp(energy, 0.0, 1.0));
    holo += ChamsColor.rgb * pow(bands, 3.0) * 0.5;

    float alpha = silhouette * clamp(
        fresnel * 0.55 + bands * 0.5 + sweep * 0.35,
        0.0,
        1.0
    );
    finalColor = vec4(holo, clamp(alpha * ChamsColor.a, 0.0, 0.85));
}

