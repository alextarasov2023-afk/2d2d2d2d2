#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Half-res pass for the plasma sky: braided magnetic filaments built from a
// ridged multifractal, sheared along latitude rings and pooled into charged
// regions. Alpha unused; the composite adds auroral shimmer and sparks.
void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    float elevation = asin(clamp(direction.y, -1.0, 1.0));
    float azimuth = atan(direction.z, direction.x);

    vec3 base = direction * 3.1;
    float drift = time * 0.06;
    float ridge = 0.0;
    float weight = 0.62;
    float scale = 1.0;
    for (int octave = 0; octave < 4; octave++) {
        float layer = fbm(base * scale + vec3(drift, -drift * 0.6, drift * 0.3));
        ridge += weight * (1.0 - abs(2.0 * layer - 1.0));
        weight *= 0.52;
        scale *= 1.95;
        drift *= 1.35;
    }
    float shear = sin(elevation * 9.0 + azimuth * 2.0 + time * 0.35);
    ridge = ridge / 1.35 + shear * 0.06;
    float filaments = pow(smoothstep(0.55, 1.25, ridge), 1.6);

    float charge = fbm(base * 0.55 + vec3(7.3, -time * 0.045, 2.1));
    charge = smoothstep(0.30, 0.78, charge);

    float hue = fbm(base * 0.4 - vec3(time * 0.02, 0.0, time * 0.015));
    vec3 color = mix(PrimaryColor.rgb, SecondaryColor.rgb, smoothstep(0.15, 0.90, hue));
    vec3 cloud = color * (0.10 + charge * 0.55) + color * filaments * 1.45;
    cloud += vec3(1.0) * pow(filaments, 3.0) * 0.35;
    finalColor = vec4(cloud, 0.0);
}
