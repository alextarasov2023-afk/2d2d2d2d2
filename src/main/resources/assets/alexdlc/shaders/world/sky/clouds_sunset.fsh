#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Half-res pass: thin haze bands stacked toward a fixed sun direction.
// Alpha carries the haze density so the composite can add the sun glow.
void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;

    vec3 sunDirection = normalize(vec3(0.55, 0.16, -0.82));
    float towardSun = clamp(dot(direction, sunDirection), 0.0, 1.0);

    float latitude = direction.y;
    float band = sin(latitude * 22.0 - time * 0.03) * 0.5 + 0.5;
    float band2 = sin(latitude * 37.0 + time * 0.022 + 2.0) * 0.5 + 0.5;
    float haze = smoothstep(0.55, 0.95, band * 0.6 + band2 * 0.4)
            * smoothstep(0.75, 0.05, latitude + 0.1);

    float density = haze * (0.35 + 0.65 * towardSun);
    vec3 tint = mix(PrimaryColor.rgb, SecondaryColor.rgb, towardSun * 0.8 + band * 0.2);
    vec3 color = tint * density * 0.16;
    finalColor = vec4(max(color, vec3(0.0)), density);
}
