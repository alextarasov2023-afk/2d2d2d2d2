#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Full-res composite for the galaxy sky: dense star fields that thicken
// over the galactic disc, plus a slow bright core bloom.
void main() {
    if (texture(DepthSampler, uv).r > 0.000001) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec4 disc = texture(CloudSampler, uv);

    float fieldA = starGlow(direction * 1.7 + 4.0, 420.0, 0.05, time * 0.7)
            * (0.4 + disc.a * 1.4);
    float fieldB = starGlow(direction * 3.1 + 23.0, 560.0, 0.03, time * 1.5)
            * (0.3 + disc.a * 1.1);

    vec3 base = PrimaryColor.rgb * 0.06;
    vec3 color = base
            + disc.rgb
            + vec3(1.0, 0.97, 0.9) * fieldA * 0.5
            + vec3(0.85, 0.9, 1.0) * fieldB * 0.4;

    float altitude = clamp(direction.y, -0.2, 1.0);
    color += mix(SecondaryColor.rgb, vec3(1.0), 0.3) * disc.a * disc.a * 0.12 * smoothstep(0.9, 0.2, altitude);

    color *= SkyParams.y;
    color = color / (1.0 + color * 0.9);
    color = pow(color, vec3(0.86)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
