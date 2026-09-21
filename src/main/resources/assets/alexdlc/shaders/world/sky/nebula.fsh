#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Full-res composite: bilinear-upsampled ion storm plus thin electrical arcs
// sweeping between the cells and a layer of deep background stars.
void main() {
    if (texture(DepthSampler, uv).r > 0.000001) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec3 color = texture(CloudSampler, uv).rgb;

    float arc = sin(direction.x * 6.0 + time * 0.35)
            * sin(direction.y * 4.0 - time * 0.22)
            * sin(direction.z * 5.0 + time * 0.15);
    float aurora = smoothstep(0.2, 0.95, abs(arc))
            * smoothstep(-0.3, 0.4, direction.y);
    color += mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.5) * aurora * 0.3;

    float stars = starGlow(direction, 320.0, 0.05, time * 0.8);
    color += vec3(0.88, 0.92, 1.0) * stars * 0.35;

    color *= SkyParams.y;
    color = color / (1.0 + color * 0.85);
    color = pow(color, vec3(0.88)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
