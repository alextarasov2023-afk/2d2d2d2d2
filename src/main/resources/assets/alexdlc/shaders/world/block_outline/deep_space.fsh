#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

vec3 decodeDirection(vec2 encoded) {
    vec2 value = encoded * 2.0 - 1.0;
    value.y = -value.y;
    vec3 direction = vec3(
        value.x,
        value.y,
        1.0 - abs(value.x) - abs(value.y)
    );
    float correction = clamp(-direction.z, 0.0, 1.0);
    direction.x += direction.x >= 0.0 ? -correction : correction;
    direction.y += direction.y >= 0.0 ? -correction : correction;
    return normalize(direction);
}

// New look: a slow-rolling cosmic horizon - stratified cloud decks flowing
// over one another under a double star field split by a soft atmospheric rim.
void main() {
    vec3 direction = decodeDirection(uv);
    float time = Params.z;
    float up = clamp(direction.y * 0.5 + 0.5, 0.0, 1.0);

    float deckA = fbm(vec3(direction.xz * 3.2, time * 0.03));
    float deckB = fbm(vec3(direction.xz * 5.4 + 9.3, time * 0.05));
    float stratify = smoothstep(0.15, 0.85, up);

    vec3 deckColor = mix(vec3(0.030, 0.036, 0.052), Tint.rgb * 0.55, stratify);
    vec3 color = deckColor * (0.55 + 0.45 * deckA * stratify + 0.30 * deckB * (1.0 - stratify));

    float horizon = exp(-pow((direction.y - 0.05) * 4.5, 2.0));
    color += Tint.rgb * horizon * 0.35;

    float lowStars = starGlow(direction, 240.0, 0.07, time * 0.8);
    float highStars = starGlow(direction * 1.9 + 23.0, 380.0, 0.03, time * 1.4);
    color += vec3(0.92, 0.95, 1.0) * lowStars * (0.4 + (1.0 - stratify) * 0.9);
    color += vec3(1.0, 0.97, 0.90) * highStars * (0.3 + stratify * 0.7);

    color *= Params.w;
    color = color / (1.0 + color * 0.8);
    color = pow(color, vec3(0.85)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0) * Tint.rgb, Tint.a);
}
