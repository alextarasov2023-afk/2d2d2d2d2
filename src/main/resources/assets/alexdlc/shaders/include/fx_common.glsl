// Shared 2D noise / color helpers for hand and chams fill shaders.

float fxHash21(vec2 value) {
    value = fract(value * vec2(213.34, 435.45));
    value += dot(value, value + 54.345);
    return fract(value.x * value.y);
}

float fxNoise(vec2 value) {
    vec2 cell = floor(value);
    vec2 local = fract(value);
    local = local * local * (3.0 - 2.0 * local);
    float a = fxHash21(cell);
    float b = fxHash21(cell + vec2(1.0, 0.0));
    float c = fxHash21(cell + vec2(0.0, 1.0));
    float d = fxHash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fxFbm(vec2 value) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += amplitude * fxNoise(value);
        value = value * 2.03 + vec2(17.13, 9.71);
        amplitude *= 0.5;
    }
    return sum;
}

vec3 hsv2rgb(vec3 color) {
    vec4 rgb = vec4(
        abs(fract(color.x * 6.0 + vec4(0.0, 4.0 / 6.0, 2.0 / 6.0, 0.0)) - 3.0) - 1.0,
        color.y,
        0.0,
        0.0
    );
    vec3 result = color.z * min(rgb.x, vec3(1.0));
    result = mix(result, vec3(color.z), rgb.y);
    return result;
}

vec3 rgb2hsv(vec3 color) {
    vec4 sorted = vec4(
        color.r,
        color.g,
        color.b,
        max(color.b, max(color.r, color.g))
    );
    float delta = sorted.w - min(min(sorted.x, sorted.y), sorted.z);
    float hue = delta == 0.0
            ? 0.0
            : (sorted.w == sorted.x
                    ? (sorted.y - sorted.z) / delta + (sorted.y < sorted.z ? 6.0 : 0.0)
                    : sorted.w == sorted.y
                            ? (sorted.z - sorted.x) / delta + 2.0
                            : (sorted.x - sorted.y) / delta + 4.0) / 6.0;
    return vec3(
        fract(hue),
        sorted.w == 0.0 ? 0.0 : delta / sorted.w,
        sorted.w
    );
}
