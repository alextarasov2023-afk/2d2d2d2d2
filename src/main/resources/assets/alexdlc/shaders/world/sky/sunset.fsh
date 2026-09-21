#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;

#moj_import <alexdlc:block_outline_common.glsl>
#moj_import <alexdlc:world_sky_common.glsl>

// Full-res composite for the sunset sky: warm horizon grade, a soft sun
// bloom toward the fixed sun direction and a few faint stars up high.
void main() {
    if (texture(DepthSampler, uv).r > 0.000001) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    vec4 haze = texture(CloudSampler, uv);

    vec3 sunDirection = normalize(vec3(0.55, 0.16, -0.82));
    float towardSun = clamp(dot(direction, sunDirection), 0.0, 1.0);
    float altitude = clamp(direction.y, -0.4, 1.0);

    vec3 warm = mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.65);
    vec3 zenith = PrimaryColor.rgb * 0.3;
    vec3 horizon = warm * 1.15;
    vec3 base = mix(horizon, zenith, smoothstep(-0.05, 0.55, altitude));

    float sunCore = smoothstep(0.9975, 0.9995, towardSun);
    float sunGlow = exp(-pow((1.0 - towardSun) * 7.0, 2.0));
    vec3 sunColor = mix(warm, vec3(1.0), 0.45);
    vec3 color = base
            + haze.rgb
            + sunColor * (sunGlow * 0.55 + sunCore * 1.6)
            + vec3(1.0, 0.95, 0.85) * starGlow(direction * 2.0 + 6.0, 240.0, 0.02, time * 0.8)
            * smoothstep(0.25, 0.75, altitude) * 0.4;

    color *= SkyParams.y;
    color = color / (1.0 + color * 0.8);
    color = pow(color, vec3(0.9)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
