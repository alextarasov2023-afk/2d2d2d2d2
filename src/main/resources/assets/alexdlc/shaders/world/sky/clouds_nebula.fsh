#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Half-res pass for the nebula sky: luminous ion-storm cells wandering
// through a gradient-warped field, with charged dust filling the gaps.
float stormCells(vec3 point, float time) {
    vec3 cell = floor(point);
    vec3 local = fract(point);
    float energy = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 id = cell + vec3(float(x), float(y), float(z));
                vec3 hash = hash33(id);
                vec3 offset = 0.5 + 0.42 * sin(time * 0.7 + hash * 6.2831853);
                float distanceToCell = length(
                        local - vec3(float(x), float(y), float(z)) - offset);
                float pulse = 0.55 + 0.45 * sin(time * 1.4 + hash.x * 40.0);
                energy += pulse * exp(-distanceToCell * distanceToCell * 5.5);
            }
        }
    }
    return energy;
}

void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec3 point = direction * 2.6;

    vec3 warp = vec3(
        fbm(point * 0.7 + vec3(time * 0.03, 0.0, 0.0)),
        fbm(point * 0.7 + vec3(5.2, time * 0.024, 1.3)),
        fbm(point * 0.7 + vec3(1.7, 9.2, -time * 0.02))
    );
    point += (warp - 0.5) * 1.6;

    float cells = smoothstep(0.25, 1.35, stormCells(point * 1.7, time));
    float dust = smoothstep(0.28, 0.85, fbm(point * 2.3 + vec3(0.0, time * 0.015, 0.0)));
    float hue = fbm(point * 0.5 + 13.1);

    vec3 core = mix(PrimaryColor.rgb, SecondaryColor.rgb, smoothstep(0.2, 0.8, hue));
    vec3 cloud = core * (dust * 0.35 + cells * 1.30);
    cloud += vec3(1.0) * pow(cells, 3.0) * 0.28;
    finalColor = vec4(cloud, 0.0);
}
