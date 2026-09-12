shader_type canvas_item;

// GPU landscape-plan grass. Overlapping irregular washes, paper in the
// gaps, sediment on broken drying fronts. Not metaballs and not a stamp field.
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
	for (int i = 0; i < 5; i++) {
		if (i >= oct) {
			break;
		}
		v += a * noise(p);
		p = p * 2.07 + vec2(17.1, 9.7);
		a *= 0.5;
	}
	return v;
}

vec2 rotate(vec2 v, float a) {
	float s = sin(a);
	float c = cos(a);
	return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

vec3 stain(vec3 base, vec3 pigment, float thickness) {
	thickness = max(thickness, 0.0);
	float t = 1.0 - exp(-thickness * 2.45);
	return mix(base, pigment, t) * (1.0 - 0.14 * t);
}

void fragment() {
	vec2 uv = UV;
	vec2 px = uv * u_size;
	float aspect = u_size.x / u_size.y;
	vec2 p = vec2(uv.x * aspect, uv.y);

	float paper_fine = noise(px * 0.62);
	float paper_fiber = noise(px * vec2(0.16, 0.34) + 5.1);
	float paper = mix(paper_fine, paper_fiber, 0.42);
	vec3 paper_col = vec3(0.988, 0.976, 0.933) + (paper - 0.5) * vec3(0.018, 0.016, 0.012);
	vec3 color = paper_col;

	vec2 warp = vec2(
		fbm(p * 0.95 + 1.4, 3) - 0.5,
		fbm(p * 0.88 + 6.8, 3) - 0.5
	);
	vec2 q = p + 0.22 * warp;
	q += 0.07 * vec2(fbm(p * 2.5 + 3.1, 2) - 0.5, fbm(p * 2.2 + 8.4, 2) - 0.5);

	float field = 8.0;
	float field_b = 8.0;
	for (int i = 0; i < 6; i++) {
		vec2 rnd = hash22(vec2(float(i) * 2.7, u_seed * 11.0 + 1.8));
		vec2 center = vec2(
			(0.10 + 0.78 * rnd.x + 0.04 * float(i)) * aspect,
			0.14 + 0.70 * rnd.y
		);
		vec2 radii = vec2(0.22 + 0.20 * rnd.x, 0.16 + 0.18 * rnd.y);
		float ang = (rnd.x - 0.5) * 1.4;
		vec2 d = rotate(q - center, ang) / radii;
		float lobe = 0.07 * sin(atan(d.y, d.x) * (6.0 + 5.0 * rnd.y) + u_seed * 6.0);
		float nedge = 0.10 * (fbm(q * 3.4 + float(i), 3) - 0.5);
		float sd = length(d) - (1.0 + lobe + nedge);
		field = min(field, sd);
		if (i >= 2) {
			field_b = min(field_b, sd + 0.04);
		}
	}

	float wash = 1.0 - smoothstep(-0.08, 0.11, field);
	float wash_mid = 1.0 - smoothstep(-0.02, 0.09, field_b);
	float wash_deep = 1.0 - smoothstep(-0.12, 0.02, field - 0.06 + 0.10 * (1.0 - uv.x));
	float keep = wash;

	float reserve = smoothstep(0.58, 0.76, fbm(q * 1.05 + vec2(uv.y * 1.8, 3.3), 3));
	reserve *= smoothstep(0.12, 0.48, wash);
	keep *= 1.0 - 0.88 * reserve;

	float iso = field;
	float front = exp(-iso * iso * 90.0);
	float broken = smoothstep(0.32, 0.78, fbm(q * 5.4 + 2.2, 2));
	front *= mix(0.08, 1.0, broken);
	float front_deep = exp(-pow(field + 0.04, 2.0) * 70.0) * mix(0.1, 1.0, 1.0 - broken);

	float grain = mix(1.0, 0.48 + 1.05 * paper, keep);

	vec3 YELLOW = vec3(0.91, 0.86, 0.40);
	vec3 OLIVE = vec3(0.46, 0.54, 0.18);
	vec3 GREEN = vec3(0.27, 0.44, 0.13);
	vec3 SAP = vec3(0.34, 0.50, 0.16);
	vec3 EARTH = vec3(0.43, 0.37, 0.15);
	vec3 INK = vec3(0.11, 0.16, 0.06);

	color = stain(color, YELLOW, 0.07 * (0.7 + 0.5 * paper));

	if (u_stage >= 1) {
		color = stain(color, YELLOW, wash * 0.16 * grain);
		color = stain(color, SAP, wash * 0.07 * grain);
	}
	if (u_stage >= 2) {
		color = stain(color, OLIVE, wash_mid * 0.17 * grain);
		color = stain(color, YELLOW, max(wash - wash_mid, 0.0) * 0.10 * grain);
	}
	if (u_stage >= 3) {
		color = stain(color, GREEN, wash_deep * 0.22 * grain);
		color = stain(color, OLIVE, front * 0.55 * grain);
		color = stain(color, GREEN, front_deep * 0.28 * grain);
	}
	if (u_stage >= 4) {
		color = mix(color, paper_col, reserve * 0.82);
		color = stain(color, OLIVE, keep * 0.06 * grain);
		color = stain(color, EARTH, keep * front_deep * 0.12 * grain);
	}
	if (u_stage >= 5) {
		float clump = fbm(q * 4.6 + 4.0, 3);
		float in_mass = smoothstep(0.35, 0.75, keep);
		color = stain(color, GREEN, smoothstep(0.64, 0.84, clump) * in_mass * 0.12 * grain);
		color = stain(color, OLIVE, front * in_mass * 0.16 * grain);
		float dark = smoothstep(0.78, 0.93, clump) * smoothstep(0.55, 0.95, keep) * step(0.52, paper);
		color = stain(color, INK, dark * 0.32);
		float fleck = smoothstep(0.88, 0.97, noise(px * vec2(0.08, 0.17) + clump));
		color = stain(color, mix(GREEN, INK, 0.4), fleck * in_mass * 0.14);
	}

	COLOR = vec4(clamp(color, 0.0, 1.0), 1.0);
}
