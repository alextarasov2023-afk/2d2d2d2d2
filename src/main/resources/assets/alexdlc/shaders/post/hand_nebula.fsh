#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

vec3 hash3(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
             dot(p, vec3(269.5, 183.3, 246.1)),
             dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(p) * 43758.5453123);
}

float noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    return mix(mix(mix(dot(hash3(i + vec3(0,0,0)), f - vec3(0,0,0)),
                       dot(hash3(i + vec3(1,0,0)), f - vec3(1,0,0)), u.x),
                   mix(dot(hash3(i + vec3(0,1,0)), f - vec3(0,1,0)),
                       dot(hash3(i + vec3(1,1,0)), f - vec3(1,1,0)), u.x), u.y),
               mix(mix(dot(hash3(i + vec3(0,0,1)), f - vec3(0,0,1)),
                       dot(hash3(i + vec3(1,0,1)), f - vec3(1,0,1)), u.x),
                   mix(dot(hash3(i + vec3(0,1,1)), f - vec3(0,1,1)),
                       dot(hash3(i + vec3(1,1,1)), f - vec3(1,1,1)), u.x), u.y), u.z);
}

float fbm(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    vec3 shift = vec3(100.0);
    for (int i = 0; i < 4; ++i) {
        v += a * noise3D(p);
        p = p * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }

    float time = FillParams.x * 0.4;
    vec3 pos = vec3(uv * 2.5, time * 0.3);

    float n1 = fbm(pos);
    float n2 = fbm(pos * 2.0 + vec3(n1 * 1.5, time * 0.2, n1));

    vec3 deepCosmos = vec3(0.02, 0.01, 0.05);
    vec3 nebulaCore = FillColor.rgb * 1.3;
    vec3 stardust = mix(FillColor.rgb, vec3(0.9, 0.8, 1.0), 0.7);

    vec3 color = mix(deepCosmos, nebulaCore, smoothstep(0.1, 0.65, n2));

    float filaments = pow(clamp(n2, 0.0, 1.0), 2.5) * 1.5;
    color += stardust * filaments;

    float stars = pow(fract(sin(dot(uv * 150.0 + vec2(time * 0.1), vec2(12.9898, 78.233))) * 43758.5453), 24.0) * 1.2;
    color += vec3(stars);

    finalColor = vec4(clamp(color, 0.0, 1.0), clamp(mask * FillColor.a, 0.0, 1.0));
}
