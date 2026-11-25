#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform float Dist;

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1,0)), f.x),
               mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x), f.y);
}

void main() {
    vec2 uv = texCoord;
    vec4 color = texture(DiffuseSampler, uv);

    float distNorm = clamp(Dist / 250.0, 0.0, 1.0);
    float t = Time;

    // Signal loss at extreme distance
    if (Dist > 999.0) {
        float n = noise(uv * 400 + t * 2);
        fragColor = vec4(vec3(n * 0.5), 1.0);
        return;
    }

    // Jitter / displacement
    float jitX = (hash(vec2(t * 13, 7)) - 0.5) * distNorm * 0.008;
    float jitY = (hash(vec2(t * 17, 5)) - 0.5) * distNorm * 0.006;
    uv += vec2(jitX, jitY);
    color = texture(DiffuseSampler, uv);

    // White noise
    float n = noise(uv * 400 + t * 2);
    float noiseAmt = 0.08 + distNorm * 0.6;
    color.rgb = mix(color.rgb, vec3(n * 0.5), noiseAmt);

    // Horizontal glitch bars
    float glitch = smoothstep(0.97, 1.0, fract(uv.y * 60 + t * 4 * distNorm));
    glitch *= distNorm * 0.3;
    color.rgb += vec3(glitch);

    // Scanlines
    float scan = sin(uv.y * 600) * 0.04 + 1.0;
    scan = mix(1.0, scan, 0.4 + distNorm * 0.4);
    color.rgb *= scan;

    // Vignette
    vec2 vigUv = uv - 0.5;
    float vig = 1.0 - dot(vigUv, vigUv) * 1.4;
    vig = mix(1.0, vig, 0.4 + distNorm * 0.6);
    color.rgb *= vig;

    // Slight desaturation at distance
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(color.rgb, vec3(gray), distNorm * 0.3);

    // Brightness flicker at high distance
    float flicker = 1.0 - (hash(vec2(t * 7, 11)) * 0.15 * distNorm);
    color.rgb *= flicker;

    fragColor = vec4(color.rgb, color.a);
}
