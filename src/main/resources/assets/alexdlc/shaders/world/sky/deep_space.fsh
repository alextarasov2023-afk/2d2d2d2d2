#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Full-res composite: bilinear-upsampled galactic drift plus two star
// populations - sparse bright stars and a dense fine twinkle concentrated
// on the galactic band via the cloud alpha mask.
void main() {
    if (texture(DepthSampler, uv).r > 0.000001) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec4 cloud = texture(CloudSampler, uv);
    float band = cloud.a;

    float brightStars = starGlow(direction, 260.0, 0.045, time * 0.9);
    float denseStars = starGlow(direction * 2.1 + 17.0, 420.0, 0.02, time * 1.7);
    vec3 color = cloud.rgb
            + vec3(1.0, 0.98, 0.92) * brightStars * (0.55 + band * 1.1)
            + vec3(0.82, 0.88, 1.0) * denseStars * (0.25 + band * 0.9);

    color *= SkyParams.y;
    color = color / (1.0 + color * 0.9);
    color = pow(color, vec3(0.86)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
