#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

// New look: fluid caustic web built from a curl-warped Voronoi field.
// Each cell's edge glints where the shared border aligns with the flow.
void main() {
    vec2 resolution = max(Params.xy, vec2(1.0));
    vec2 position = (uv * resolution * 2.0 - resolution) / resolution.y;
    float time = Params.z;

    vec2 flow = vec2(
        fbm(vec3(position * 1.4, time * 0.10)) - 0.5,
        fbm(vec3(position * 1.4 + 31.7, time * 0.08)) - 0.5
    );
    vec2 point = position * 2.3 + flow * 1.1;

    vec2 cell = floor(point);
    vec2 local = fract(point);
    float edgeDistance = 8.0;
    float cellSeed = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y));
            vec2 hash = hash33(vec3(cell + offset, 7.0)).xy;
            vec2 center = offset + hash - local;
            edgeDistance = min(edgeDistance, length(center));
            if (x == 0 && y == 0) {
                cellSeed = hash.x;
            }
        }
    }

    float web = 1.0 - smoothstep(0.0, 0.42, edgeDistance);
    float glint = pow(web, 3.0) * (0.55 + 0.45 * sin(time * 2.0 + cellSeed * 6.2831853));
    float interior = smoothstep(0.42, 0.05, edgeDistance) * 0.16;

    vec3 color = Tint.rgb * (interior + glint * 1.9);
    color += vec3(1.0) * pow(glint, 2.0) * 0.4;
    color = clamp(color, 0.0, 1.0) * Params.w;
    finalColor = vec4(color, Tint.a);
}
