shader_type canvas_item;

// GPU wash for plan-scale grass. Large related masses, paper in the gaps,
// sediment on evaporating fronts. Seeded and object-space. Not a stamp field.
uniform vec2 u_size = vec2(2048.0, 1434.0);
uniform float u_seed = 0.37;
uniform int u_stage = 5;

float hash21(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973) + u_seed);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

vec2 hash22(vec2 p) {
	return vec2(hash21(p), hash21(p + 19.1));
}

float noise(vec2 p) {
	vec2 i = floor(p);
	vec2 f = fract(p);
	f = f * f * (3.0 - 2.0 * f);
	float a = hash21(i);
	float b = hash21(i + vec2(1.0, 0.0));
	float c = hash21(i + vec2(0.0, 1.0));
	float d = hash21(i + vec2(1.0, 1.0));
	return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p, int oct) {
	float v = 0.0;
	float a = 0.5;
	for (int i = 0; i < 6; i++) {
		if (i >= oct) {
			break;
		}
		v += a * noise(p);
		p = p * 2.03 + vec2(17.1, 9.7);
		a *= 0.5;
	}
	return v;
}

vec3 stain(vec3 base, vec3 pigment, float thickness) {
	thickness = max(thickness, 0.0);
	float t = 1.0 - exp(-thickness * 2.6);
	return mix(base, pigment, t) * (1.0 - 0.16 * t);
}

void fragment() {
	vec2 uv = UV;
	vec2 px = uv * u_size;
	float aspect = u_size.x / u_size.y;
	vec2 p = vec2(uv.x * aspect, uv.y);

	float paper_fine = noise(px * 0.55);
	float paper_fiber = noise(px * vec2(0.18, 0.31) + 4.2);
	float paper = 0.58 * paper_fine + 0.42 * paper_fiber;
	vec3 color = vec3(0.982, 0.968, 0.905) + (paper - 0.5) * vec3(0.022, 0.020, 0.016);

	vec2 warp = vec2(
		fbm(p * 1.15 + 1.7, 3) - 0.5,
		fbm(p * 1.05 + 6.2, 3) - 0.5
	);
	vec2 q = p + 0.10 * warp;

	float blobs = 0.0;
	for (int i = 0; i < 8; i++) {
		vec2 rnd = hash22(vec2(float(i) * 3.1, u_seed * 8.0 + 2.4));
		vec2 center = vec2(0.08 + 0.84 * rnd.x * aspect, 0.12 + 0.76 * rnd.y);
		center.x += 0.06 * float(i) / 8.0;
		float radius = 0.20 + 0.16 * hash21(rnd + 4.4);
		vec2 d = (q - center) / vec2(radius * (0.85 + 0.4 * rnd.x), radius);
		float scallop = 0.08 * sin(atan(d.y, d.x) * (7.0 + 3.0 * rnd.y) + warp.x * 8.0);
		blobs += exp(-dot(d, d) * (1.55 - scallop));
	}
	blobs += 0.12 * uv.x;
	float island = clamp(blobs, 0.0, 2.2);
	float wash = smoothstep(0.28, 0.72, island);
	float wash_mid = smoothstep(0.42, 0.88, island);
	float wash_deep = smoothstep(0.62, 1.15, island + 0.18 * uv.x);

	float reserve_n = fbm(q * 0.85 + vec2(uv.y * 1.4, 2.8), 3);
	float reserve = smoothstep(0.58, 0.76, reserve_n) * smoothstep(0.25, 0.55, wash);
	float keep = wash * (1.0 - 0.82 * reserve);

	float front = exp(-pow((island - 0.50) * 7.5, 2.0));
	front *= 0.20 + 0.80 * smoothstep(0.30, 0.75, fbm(q * 4.8 + 2.0, 2));
	float front_deep = exp(-pow((island - 0.82) * 8.0, 2.0));
	front_deep *= 0.25 + 0.75 * smoothstep(0.28, 0.7, fbm(q * 5.1 + 9.2, 2));

	float grain = 0.55 + 0.9 * paper;
	grain = mix(1.0, grain, keep);

	vec3 YELLOW = vec3(0.86, 0.82, 0.36);
	vec3 OLIVE = vec3(0.42, 0.50, 0.16);
	vec3 GREEN = vec3(0.24, 0.40, 0.12);
	vec3 EARTH = vec3(0.40, 0.34, 0.14);
	vec3 INK = vec3(0.09, 0.13, 0.05);

	color = stain(color, YELLOW, (0.18 + 0.10 * fbm(uv * 2.2, 2)) * (0.75 + 0.4 * paper));
	color = stain(color, OLIVE, 0.05 * (0.75 + 0.4 * paper));

	if (u_stage >= 1) {
		color = stain(color, YELLOW, wash * 0.14 * grain);
		color = stain(color, OLIVE, wash * 0.10 * grain);
	}
	if (u_stage >= 2) {
		color = stain(color, OLIVE, wash_mid * 0.18 * grain);
		color = stain(color, YELLOW, (wash - wash_mid) * 0.08 * grain);
	}
	if (u_stage >= 3) {
		color = stain(color, GREEN, wash_deep * 0.24 * grain);
		color = stain(color, OLIVE, front * 0.40 * grain);
		color = stain(color, GREEN, front_deep * 0.22 * grain);
	}
	if (u_stage >= 4) {
		color = mix(color, vec3(0.975, 0.955, 0.860), reserve * 0.78);
		color = stain(color, OLIVE, keep * 0.08 * grain);
		color = stain(color, EARTH, keep * front_deep * 0.10 * grain);
	}
	if (u_stage >= 5) {
		float clump = fbm(q * 5.8 + 3.1, 3);
		color = stain(color, GREEN, smoothstep(0.58, 0.80, clump) * keep * 0.16 * grain);
		color = stain(color, OLIVE, front * keep * 0.14 * grain);
		float dark = smoothstep(0.74, 0.90, clump) * smoothstep(0.55, 0.9, keep) * step(0.50, paper);
		color = stain(color, INK, dark * 0.38);
		float blade_n = noise(px * vec2(0.07, 0.19) + clump * 1.8);
		color = stain(color, mix(GREEN, INK, 0.35), smoothstep(0.84, 0.95, blade_n) * keep * 0.16);
	}

	COLOR = vec4(clamp(color, 0.0, 1.0), 1.0);
}
