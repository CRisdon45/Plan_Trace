shader_type canvas_item;

// GPU landscape-plan grass. Lot polygon + holes are the clip. Washes live
// inside that silhouette with an inset drying front so a later hard clip
// still reads as paint. Seeded, object-space. Not a stamp field.
uniform vec2 u_size = vec2(2048.0, 1434.0);
uniform float u_seed = 0.37;
uniform int u_stage = 5;
uniform vec2 u_lot[16];
uniform int u_lot_count = 0;
uniform vec2 u_hole_a[8];
uniform int u_hole_a_count = 0;
uniform vec2 u_hole_b[8];
uniform int u_hole_b_count = 0;

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

float sd_ring(vec2 p, vec2 a, vec2 b, inout float s) {
	vec2 e = b - a;
	vec2 w = p - a;
	vec2 proj = w - e * clamp(dot(w, e) / max(dot(e, e), 1e-6), 0.0, 1.0);
	bvec3 c = bvec3(p.y >= a.y, p.y < b.y, e.x * w.y > e.y * w.x);
	if (all(c) || all(not(c))) {
		s *= -1.0;
	}
	return dot(proj, proj);
}

float sd_lot(vec2 p) {
	int n = u_lot_count;
	if (n < 3) {
		return -1.0;
	}
	float d = 1e6;
	float s = 1.0;
	int j = n - 1;
	for (int i = 0; i < 16; i++) {
		if (i >= n) {
			break;
		}
		d = min(d, sd_ring(p, u_lot[i], u_lot[j], s));
		j = i;
	}
	return s * sqrt(d);
}

float sd_hole_a(vec2 p) {
	int n = u_hole_a_count;
	if (n < 3) {
		return 1.0;
	}
	float d = 1e6;
	float s = 1.0;
	int j = n - 1;
	for (int i = 0; i < 8; i++) {
		if (i >= n) {
			break;
		}
		d = min(d, sd_ring(p, u_hole_a[i], u_hole_a[j], s));
		j = i;
	}
	return s * sqrt(d);
}

float sd_hole_b(vec2 p) {
	int n = u_hole_b_count;
	if (n < 3) {
		return 1.0;
	}
	float d = 1e6;
	float s = 1.0;
	int j = n - 1;
	for (int i = 0; i < 8; i++) {
		if (i >= n) {
			break;
		}
		d = min(d, sd_ring(p, u_hole_b[i], u_hole_b[j], s));
		j = i;
	}
	return s * sqrt(d);
}

void fragment() {
	vec2 uv = UV;
	vec2 px = uv * u_size;
	float aspect = u_size.x / u_size.y;
	vec2 p = vec2(uv.x * aspect, uv.y);

	float paper_fine = noise(px * 0.62);
	float paper_fiber = noise(px * vec2(0.16, 0.34) + 5.1);
	float paper = mix(paper_fine, paper_fiber, 0.42);
	vec3 paper_col = vec3(0.992, 0.986, 0.958) + (paper - 0.5) * vec3(0.014, 0.012, 0.010);
	vec3 color = paper_col;

	float lot_sd = sd_lot(uv);
	lot_sd += 0.011 * (fbm(uv * 9.0 + 1.2, 3) - 0.38);
	float hole = min(sd_hole_a(uv), sd_hole_b(uv));
	float inside = (1.0 - smoothstep(-0.003, 0.006, lot_sd)) * smoothstep(-0.004, 0.006, hole);

	vec2 q = uv * vec2(aspect, 1.0);
	float var = fbm(q * 2.4 + 1.1, 4);
	float var2 = fbm(q * 5.5 + 4.8, 3);
	float even = inside;
	float mid = smoothstep(0.38, 0.72, var) * inside;
	float shade = smoothstep(0.58, 0.86, var) * (0.35 + 0.65 * var2) * inside;
	float lift = smoothstep(0.62, 0.82, var2) * mid;
	float broken = mix(0.12, 1.0, smoothstep(0.28, 0.7, fbm(uv * 13.0, 2)));
	float lot_front = exp(-lot_sd * lot_sd * 900.0) * inside * broken;
	float hole_front = exp(-hole * hole * 1100.0) * inside * mix(0.1, 1.0, 1.0 - broken);
	float grain = 0.72 + 0.45 * paper;

	vec3 WASH = vec3(0.78, 0.82, 0.48);
	vec3 MID = vec3(0.62, 0.70, 0.32);
	vec3 SHADE = vec3(0.50, 0.58, 0.24);
	vec3 EDGE = vec3(0.42, 0.50, 0.18);

	if (u_stage >= 1) {
		color = stain(color, WASH, even * (0.22 + 0.06 * paper) * grain);
	}
	if (u_stage >= 2) {
		color = stain(color, MID, mid * 0.14 * grain);
		color = stain(color, WASH, (even - mid) * 0.04);
	}
	if (u_stage >= 3) {
		color = stain(color, SHADE, shade * 0.16 * grain);
		color = stain(color, EDGE, lot_front * 0.28 * grain);
	}
	if (u_stage >= 4) {
		color = mix(color, paper_col, lift * 0.45);
		color = stain(color, WASH, even * 0.04 * grain);
		color = stain(color, EDGE, hole_front * 0.18 * grain);
	}
	if (u_stage >= 5) {
		float speckle = smoothstep(0.90, 0.97, noise(px * vec2(0.09, 0.16) + var));
		color = stain(color, EDGE, speckle * even * 0.10);
		color = stain(color, SHADE, lot_front * 0.12 * grain);
	}

	COLOR = vec4(clamp(color, 0.0, 1.0), 1.0);
}
