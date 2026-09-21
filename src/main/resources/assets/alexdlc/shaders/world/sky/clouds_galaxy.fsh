#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Half-res pass: a bright galactic core smeared along a tilted plane with
// faint nebula wisps. Alpha carries the disc density for the star fields.
void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;

    vec3 planeNormal = normalize(vec3(0.35, 0.85, -0.38));
    float plane = dot(direction, planeNormal);
    float core = exp(-plane * plane * 9.0);

    float swirl = fbm(direction * 3.4 + vec3(time * 0.008, 0.0, -time * 0.005));
    float wisps = smoothstep(0.42, 0.72, swirl) * smoothstep(0.45, 0.05, abs(plane));

    vec3 tint = mix(PrimaryColor.rgb, SecondaryColor.rgb, smoothstep(0.2, 0.8, swirl));
    vec3 color = vec3(0.004, 0.005, 0.01)
            + tint * core * 0.22
            + tint * wisps * 0.16
            + SecondaryColor.rgb * core * wisps * 0.2;
    finalColor = vec4(max(color, vec3(0.0)), core * 0.7 + wisps * 0.5);
}
