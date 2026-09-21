#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

// New look: obsidian surface lit by a slow liquid-metal sheen - layered
// sine stripes distorted by a smooth noise gradient, rim-lit at the edges.
void main() {
    vec2 resolution = max(Params.xy, vec2(1.0));
    vec2 position = (uv * resolution * 2.0 - resolution) / resolution.y;
    float time = Params.z * 0.6;

    float height = fbm(vec3(position * 1.6, time * 0.05));
    vec2 gradient = vec2(
        fbm(vec3(position * 1.6 + vec2(0.08, 0.0), time * 0.05)) - height,
        fbm(vec3(position * 1.6 + vec2(0.0, 0.08), time * 0.05)) - height
    );

    vec2 normal = normalize(vec3(gradient * 6.0, 1.0)).xy;
    vec2 lightDirection = normalize(vec2(cos(time * 0.4), sin(time * 0.4)));
    float diffuse = clamp(dot(normal, lightDirection) * 0.5 + 0.5, 0.0, 1.0);

    float stripe = sin(position.x * 9.0 + height * 14.0 + time * 1.2)
            * sin(position.y * 7.0 - height * 11.0 - time * 0.8);
    float sheen = pow(abs(stripe), 3.0);

    float radial = length(position);
    float rim = smoothstep(0.9, 1.35, radial) * smoothstep(1.7, 1.2, radial);

    vec3 color = vec3(0.03, 0.03, 0.05)
            + Tint.rgb * diffuse * 0.22
            + mix(vec3(1.0), Tint.rgb, 0.35) * sheen * 0.8
            + Tint.rgb * rim * 0.9;
    color = clamp(color, 0.0, 1.0) * Params.w;
    finalColor = vec4(color, Tint.a);
}
