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
    if (mask > 0.45) {
        discard;
    }
    float glow = max(0.0, texture(BlurredSampler, uv).a - mask);
    glow = clamp(glow, 0.0, 1.0);

    // Two-lobe neon bloom: tight core plus a wide soft halo, gently pulsing.
    float core = pow(glow, 1.35);
    float bloom = pow(glow, 0.45) * 0.55;
    float pulse = 0.88 + 0.12
            * sin(ChamsParams.x * 4.0 + uv.x * 18.0 + uv.y * 12.0);
    float luminance = clamp((core + bloom) * pulse * ChamsParams.w, 0.0, 1.6);
    float alpha = clamp(luminance * ChamsColor.a, 0.0, 1.0);
    if (alpha < 0.003) {
        discard;
    }
    vec3 tint = ChamsColor.rgb * (0.85 + 0.9 * luminance);
    tint = mix(tint, vec3(1.0), clamp(core * 0.35, 0.0, 0.5));
    finalColor = vec4(tint, alpha);
}

