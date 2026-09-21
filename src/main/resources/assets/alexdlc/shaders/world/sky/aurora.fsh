#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Full-res composite for the aurora sky: dark zenith, upsampled curtains,
// a fine star field brightened where the curtains are strong.
void main() {
    if (texture(DepthSampler, uv).r > 0.000001) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec4 curtain = texture(CloudSampler, uv);
    float strength = curtain.a;

    float altitude = clamp(direction.y, -0.2, 1.0);
    vec3 zenith = PrimaryColor.rgb * 0.12;
    vec3 horizon = mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.35) * 0.25;
    vec3 base = mix(horizon, zenith, smoothstep(0.0, 0.6, altitude));

    float stars = starGlow(direction * 1.4 + 9.0, 300.0, 0.035, time * 1.2)
            * (0.5 + strength * 0.9);
    vec3 color = base
            + curtain.rgb
            + vec3(0.9, 1.0, 0.95) * stars * 0.55;

    color *= SkyParams.y;
    color = color / (1.0 + color * 0.85);
    color = pow(color, vec3(0.88)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
