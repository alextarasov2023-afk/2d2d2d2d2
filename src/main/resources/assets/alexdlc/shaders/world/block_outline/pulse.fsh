#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

// Pulse outline: the whole block breathes with a slow heartbeat - a wide
// soft swell followed by a crisp bright flash on the edges of the unwrap.
void main() {
    float time = Params.z;
    float heartbeat = pow(0.5 + 0.5 * sin(time * 2.1), 2.2);

    float radial = length(uv * 2.0 - 1.0) * 0.7071;
    float swell = smoothstep(0.85, 0.15, radial);
    float edge = smoothstep(0.55, 0.95, radial);

    float ripple = sin((radial - time * 0.35) * 9.0) * 0.5 + 0.5;
    vec3 color = Tint.rgb * (swell * 0.55 + edge * 0.45 * ripple);
    color += mix(Tint.rgb, vec3(1.0), 0.4) * heartbeat * (0.3 + edge * 0.8);

    color = clamp(color * (0.6 + heartbeat * 0.8) * Params.w, 0.0, 1.0);
    finalColor = vec4(color, Tint.a);
}
