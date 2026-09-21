#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

const float TAU = 6.28318530718;

vec2 rotate(vec2 value, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return mat2(cosine, -sine, sine, cosine) * value;
}

// New look: polarization ribbons - nested rings of light whose phase flips
// each revolution, folded through a spiralling warp and finished with an
// iridescent radial palette and orbiting glints.
vec3 spectral(float value) {
    return 0.5 + 0.5 * cos(
        value * vec3(1.0, 1.25, 1.5) + vec3(0.0, 2.1, 4.2)
    );
}

float foldedRings(vec2 position, float time) {
    float accumulated = 0.0;
    float amplitude = 0.6;
    vec2 value = position;
    for (int index = 0; index < 6; index++) {
        float current = float(index);
        value = rotate(value, time * (0.10 + current * 0.02) + current * 0.55);
        value *= 1.28;
        float ringPhase = length(value) * (8.0 + current * 1.7) - time * (1.3 + current * 0.3);
        accumulated += amplitude * abs(sin(ringPhase));
        amplitude *= 0.72;
    }
    return accumulated;
}

float orbitGlints(vec2 position, float time) {
    vec2 value = position * 3.4;
    vec2 cell = floor(value);
    vec2 local = fract(value) - 0.5;
    float result = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 id = cell + vec2(float(x), float(y));
            float hash = hash21(id);
            float orbit = time * (0.6 + hash * 1.2) + hash * TAU;
            vec2 position2 = vec2(cos(orbit), sin(orbit)) * 0.32;
            float distanceToGlint = length(local - vec2(float(x), float(y)) - position2);
            result += smoothstep(0.13, 0.0, distanceToGlint) * (0.35 + 0.65 * hash);
        }
    }
    return result;
}

void main() {
    vec2 resolution = max(Params.xy, vec2(1.0));
    vec2 position = (uv * resolution * 2.0 - resolution) / resolution.y;
    float time = Params.z * 0.5;
    float radial = length(position);

    vec2 spiral = rotate(position, 0.35 * log(max(radial, 0.05)) + time * 0.15);
    float folds = foldedRings(spiral, time);
    float polarization = pow(1.0 - min(folds / 2.2, 1.0), 2.0);

    float glints = orbitGlints(position + spiral * 0.04, time);
    vec3 color = spectral(radial * 1.4 - time * 0.35 + folds * 0.06) * polarization * 0.85;
    color += Tint.rgb * polarization * polarization * 0.55;
    color += vec3(1.0, 0.98, 0.94) * glints * 0.5;
    color *= smoothstep(1.55, 0.1, radial);
    color = 1.0 - exp(-color * 1.6);
    color = pow(clamp(color, 0.0, 1.0), vec3(0.85));
    finalColor = vec4(clamp(color * Params.w, 0.0, 1.0) * Tint.rgb, Tint.a);
}
