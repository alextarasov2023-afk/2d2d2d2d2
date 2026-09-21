#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Half-res pass: slow aurora curtains hanging over a dark horizon. Alpha
// carries the curtain strength so the composite can weight the stars.
void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    float altitude = clamp(direction.y, -0.2, 1.0);

    float curtain = sin(direction.x * 4.5 + sin(direction.z * 2.2 + time * 0.08) * 1.4 + time * 0.05);
    float curtain2 = sin(direction.x * 7.0 - direction.z * 1.8 - time * 0.04);
    float band = smoothstep(-0.1, 0.9, curtain) * (0.55 + 0.45 * curtain2);
    float heightGate = smoothstep(0.05, 0.45, altitude) * (1.0 - smoothstep(0.85, 1.0, altitude));
    float strength = band * heightGate;

    vec3 base = mix(PrimaryColor.rgb, SecondaryColor.rgb, smoothstep(0.1, 0.9, band * curtain2 + 0.5));
    vec3 color = PrimaryColor.rgb * 0.05
            + base * strength * 0.5
            + SecondaryColor.rgb * pow(band, 3.0) * heightGate * 0.35;
    finalColor = vec4(max(color, vec3(0.0)), strength);
}
