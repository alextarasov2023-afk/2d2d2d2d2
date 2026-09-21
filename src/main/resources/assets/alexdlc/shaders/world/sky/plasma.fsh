#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Full-res composite: bilinear-upsampled magnetic filaments plus auroral
// curtains rippling over the horizon and a sparse high-altitude spark layer.
void main() {
    if (texture(DepthSampler, uv).r > 0.000001) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec3 color = texture(CloudSampler, uv).rgb;

    float azimuth = atan(direction.z, direction.x);
    float curtain = sin(azimuth * 14.0 + time * 0.9)
            * sin(azimuth * 5.0 - time * 0.6 + direction.y * 3.0);
    float shimmer = smoothstep(0.55, 1.0, curtain)
            * smoothstep(0.05, 0.45, direction.y);
    color += mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.5) * shimmer * 0.22;

    float sparks = starGlow(direction * 1.35 + 11.0, 260.0, 0.035, time * 1.6);
    color += vec3(0.85, 0.95, 1.0) * sparks * 0.30;

    color *= SkyParams.y;
    color = color / (1.0 + color * 0.7);
    color = pow(color, vec3(0.90)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
