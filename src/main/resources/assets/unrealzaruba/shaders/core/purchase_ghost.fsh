#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GhostTime;
uniform float FillHeight;
uniform float FillSoft;
uniform vec3 FillColor;
uniform vec3 EdgeColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 localPosition;
in vec3 viewPosition;
in vec3 viewNormal;

out vec4 fragColor;

// The vehicle as a white hologram. The part the money covers so far (below FillHeight, in the mesh's own coordinates)
// is filled with FillColor (light blue), with a bright line where the fill ends.
void main() {
    vec4 sampled = texture(Sampler0, texCoord0);
    if (sampled.a < 0.01) {
        discard;
    }

    vec3 viewDirection = normalize(-viewPosition);
    float fresnel = pow(clamp(1.0 - abs(dot(normalize(viewNormal), viewDirection)), 0.0, 1.0), 2.0);
    float pulse = 0.96 + 0.04 * sin(GhostTime * 3.4);

    float soft = max(FillSoft, 0.001);
    float paid = 1.0 - smoothstep(FillHeight - soft, FillHeight, localPosition.y);
    float frontier = 1.0 - smoothstep(0.0, soft, abs(localPosition.y - FillHeight));

    // Unpaid: white, a little brighter at the edges so the shape reads
    vec3 whiteRgb = mix(vec3(0.84, 0.88, 0.92), vec3(1.0), fresnel) * pulse;
    float whiteAlpha = 0.38 + 0.30 * fresnel;

    // Paid: light blue, more solid
    vec3 blueRgb = mix(FillColor * 0.85, FillColor, fresnel) * pulse;
    float blueAlpha = 0.62 + 0.25 * fresnel;

    vec3 rgb = mix(whiteRgb, blueRgb, paid);
    float alpha = mix(whiteAlpha, blueAlpha, paid);

    // The line where the blue ends
    rgb = mix(rgb, vec3(0.85, 0.96, 1.0), frontier * 0.85);
    alpha = max(alpha, frontier * 0.85);

    fragColor = linear_fog(vec4(rgb, clamp(alpha, 0.0, 0.9)), vertexDistance, FogStart, FogEnd, FogColor);
}
