#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D BlurredSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform HandCompositeUniforms {
    vec4 FlameColor;
    float Time;
};

void main() {
    float maskAlpha = texture(MaskSampler, uv).a;
    if (maskAlpha > 0.85) {
        discard;
    }

    vec2 p = uv * 10.0;
    float plasma = sin(p.x + Time * 3.0) + cos(p.y - Time * 2.0) + sin((p.x + p.y) * 0.5 + Time);
    float glow = smoothstep(-1.5, 1.5, plasma);

    vec3 col = mix(FlameColor.rgb * 0.2, FlameColor.rgb * 2.5, glow);
    float alpha = clamp(glow * FlameColor.a * 0.9, 0.0, 1.0);
    finalColor = vec4(col, alpha);
}

