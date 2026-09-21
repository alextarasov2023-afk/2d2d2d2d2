#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Half-res pass for the deep space sky: a tilted galactic drift assembled
// from interfering wave fronts and dust lanes. Alpha carries the disc mask
// so the composite can weight its star populations.
void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec3 discNormal = normalize(vec3(0.42, 0.78, -0.46));
    float plane = dot(direction, discNormal);

    float wave = sin(plane * 7.0 - time * 0.05)
            + 0.6 * sin(plane * 13.0 + time * 0.08);
    float band = exp(-plane * plane * 5.5) * (0.75 + 0.25 * wave);

    float lane = fbm(direction * 4.5 + vec3(time * 0.012, -time * 0.006, 0.0));
    float dust = smoothstep(0.34, 0.62, lane) * band;

    float sheen = fbm(direction * 2.2 + vec3(-time * 0.008, time * 0.004, 0.0));
    vec3 tint = mix(PrimaryColor.rgb, SecondaryColor.rgb, smoothstep(0.25, 0.75, sheen));
    vec3 cloud = vec3(0.005, 0.007, 0.014)
            + tint * band * (0.12 + 0.30 * sheen)
            - tint * dust * 0.10;
    finalColor = vec4(max(cloud, vec3(0.0)), band);
}
