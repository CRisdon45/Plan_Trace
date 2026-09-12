shader_type canvas_item;

// Plan study: pale lawn, water, paving, planting masses, ink on top.
// Matches the board process (line → wash → shadow → texture → ink).
// Lot polygons in UV. Not a stamp field.
uniform vec2 u_size = vec2(2048.0, 1434.0);
uniform float u_seed = 0.37;
uniform int u_stage = 5;
uniform vec2 u_site[16];
uniform int u_site_count = 0;
uniform vec2 u_house[8];
uniform int u_house_count = 0;
uniform vec2 u_lawn[8];
uniform int u_lawn_count = 0;
uniform vec2 u_pool[8];
uniform int u_pool_count = 0;

float hash21(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973) + u_seed);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
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
	float t = 1.0 - exp(-thickness * 2.2);
	return mix(base, pigment, t) * (1.0 - 0.10 * t);
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

float sd_site(vec2 p) {
	int n = u_site_count;
	if (n < 3) {
		return 1.0;
	}
	float d = 1e6;
	float s = 1.0;
	int j = n - 1;
	for (int i = 0; i < 16; i++) {
		if (i >= n) {
			break;
		}
		d = min(d, sd_ring(p, u_site[i], u_site[j], s));
		j = i;
	}
	return s * sqrt(d);
}

float sd_house(vec2 p) {
	int n = u_house_count;
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
		d = min(d, sd_ring(p, u_house[i], u_house[j], s));
		j = i;
	}
	return s * sqrt(d);
}

float sd_lawn(vec2 p) {
	int n = u_lawn_count;
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
		d = min(d, sd_ring(p, u_lawn[i], u_lawn[j], s));
		j = i;
	}
	return s * sqrt(d);
}

float sd_pool(vec2 p) {
	int n = u_pool_count;
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
		d = min(d, sd_ring(p, u_pool[i], u_pool[j], s));
		j = i;
	}
	return s * sqrt(d);
}

float cover(float sd) {
	return 1.0 - smoothstep(-0.0025, 0.005, sd);
}

float stroke(float sd, float w) {
	return 1.0 - smoothstep(0.0, w, abs(sd));
}

void fragment() {
	vec2 uv = UV;
	vec2 px = uv * u_size;
	float aspect = u_size.x / u_size.y;
	vec2 q = vec2(uv.x * aspect, uv.y);

	float paper_n = mix(noise(px * 0.62), noise(px * vec2(0.16, 0.34) + 5.1), 0.42);
	vec3 paper_col = vec3(0.988, 0.980, 0.952) + (paper_n - 0.5) * 0.016;
	vec3 color = paper_col;
	float grain = 0.78 + 0.36 * paper_n;

	float site_sd = sd_site(uv);
	site_sd += 0.006 * (fbm(uv * 8.0, 2) - 0.45);
	float house_sd = sd_house(uv);
	float lawn_sd = sd_lawn(uv);
	lawn_sd += 0.008 * (fbm(uv * 9.0 + 2.0, 2) - 0.4);
	float pool_sd = sd_pool(uv);

	float site = cover(site_sd);
	float house = cover(house_sd) * site;
	float lawn = cover(lawn_sd) * site * (1.0 - house);
	float pool = cover(pool_sd) * site * (1.0 - house);
	float paving = site * (1.0 - house) * (1.0 - lawn) * (1.0 - pool);

	float var = fbm(q * 2.6 + 1.2, 4);
	float var2 = fbm(q * 5.8 + 4.1, 3);
	float broken = mix(0.15, 1.0, smoothstep(0.3, 0.72, fbm(uv * 14.0, 2)));

	vec3 LAWN_W = vec3(0.78, 0.82, 0.50);
	vec3 LAWN_M = vec3(0.62, 0.70, 0.34);
	vec3 LAWN_S = vec3(0.50, 0.58, 0.26);
	vec3 WATER_W = vec3(0.72, 0.86, 0.88);
	vec3 WATER_M = vec3(0.38, 0.68, 0.78);
	vec3 WATER_D = vec3(0.13, 0.42, 0.56);
	vec3 STONE_W = vec3(0.86, 0.80, 0.68);
	vec3 STONE_M = vec3(0.76, 0.68, 0.54);
	vec3 PLANT_Y = vec3(0.78, 0.76, 0.32);
	vec3 PLANT_G = vec3(0.42, 0.56, 0.24);
	vec3 PLANT_D = vec3(0.22, 0.34, 0.14);
	vec3 INK = vec3(0.16, 0.15, 0.13);

	float plants = 0.0;
	float plant_sun = 0.0;
	for (int i = 0; i < 12; i++) {
		vec2 rnd = vec2(hash21(vec2(float(i), u_seed + 2.2)), hash21(vec2(float(i) + 4.1, u_seed)));
		vec2 c = vec2(0.28 + 0.58 * rnd.x, 0.12 + 0.08 * rnd.y);
		if (i >= 7) {
			c = vec2(0.80 + 0.08 * rnd.x, 0.20 + 0.55 * rnd.y);
		}
		vec2 radii = vec2(0.048 + 0.055 * rnd.x, 0.042 + 0.045 * rnd.y);
		vec2 d = rotate(q - vec2(c.x * aspect, c.y), (rnd.x - 0.5) * 0.8) / radii;
		float lobe = 0.16 * sin(atan(d.y, d.x) * (6.0 + 4.0 * rnd.y) + rnd.x * 5.0);
		float cov = 1.0 - smoothstep(0.62, 1.15, length(d) - lobe);
		cov *= 0.55 + 0.45 * fbm(q * 7.0 + float(i), 2);
		plants = max(plants, cov);
		plant_sun = max(plant_sun, cov * step(0.45, rnd.x));
	}
	plants *= site * (1.0 - house) * (1.0 - pool);
	plants *= 1.0 - lawn * 0.92;
	plant_sun *= plants;
	float rim = smoothstep(0.055, 0.0, -pool_sd);

	if (u_stage >= 1) {
		color = mix(color, INK, stroke(site_sd, 0.0018) * 0.55);
		color = mix(color, INK, stroke(house_sd, 0.0022) * 0.85);
		color = mix(color, INK, stroke(pool_sd, 0.0017) * 0.70);
		color = mix(color, INK, stroke(lawn_sd, 0.0012) * 0.25);
	}

	if (u_stage >= 2) {
		color = stain(color, LAWN_W, lawn * (0.20 + 0.05 * paper_n) * grain);
		color = stain(color, WATER_W, pool * 0.32 * grain);
		color = stain(color, STONE_W, paving * (0.16 + 0.05 * var) * grain);
		color = stain(color, mix(PLANT_G, PLANT_Y, plant_sun), plants * 0.22 * grain);
	}
	if (u_stage >= 3) {
		color = stain(color, LAWN_M, lawn * smoothstep(0.4, 0.75, var) * 0.12 * grain);
		float depth = smoothstep(0.0, 0.07, -pool_sd);
		color = stain(color, WATER_M, pool * depth * (0.18 + 0.12 * var) * grain);
		color = stain(color, STONE_M, paving * smoothstep(0.45, 0.8, var2) * 0.10 * grain);
		color = stain(color, PLANT_G, plants * (0.18 + 0.16 * var) * grain);
		color = stain(color, PLANT_Y, plant_sun * 0.12 * grain);
	}
	if (u_stage >= 4) {
		color = stain(color, LAWN_S, lawn * exp(-lawn_sd * lawn_sd * 700.0) * broken * 0.18);
		color = stain(color, WATER_D, pool * rim * 0.55 * grain);
		float spa = pool * smoothstep(0.36, 0.42, uv.y);
		color = stain(color, WATER_D, spa * 0.18 * grain);
		color = stain(color, STONE_M, paving * exp(-min(abs(pool_sd), abs(house_sd)) * 40.0) * 0.12);
		color = stain(color, PLANT_D, plants * smoothstep(0.5, 0.9, var2) * 0.28);
	}
	if (u_stage >= 5) {
		vec2 ruv = px * 0.011;
		ruv += vec2(fbm(ruv * 0.55 + 1.3, 3), fbm(ruv * 0.5 + 4.8, 3)) * 1.4;
		float caust = pow(0.5 + 0.5 * sin(ruv.x * 7.2) * sin(ruv.y * 5.6 + ruv.x * 1.4), 10.0);
		color = mix(color, vec3(0.94, 0.98, 0.99), pool * (1.0 - rim) * caust * 0.65);
		float trough = pow(0.5 + 0.5 * sin(ruv.x * 7.2 + 1.7) * sin(ruv.y * 5.6), 3.0);
		color = stain(color, WATER_M, pool * (1.0 - caust) * trough * 0.08);
		float speckle = smoothstep(0.91, 0.97, noise(px * vec2(0.09, 0.16)));
		color = stain(color, LAWN_S, speckle * lawn * 0.08);
		vec2 gv = fract(uv * vec2(14.0, 10.0) + 0.08 * vec2(var, var2));
		float joint = (1.0 - smoothstep(0.0, 0.045, min(gv.x, gv.y))) * paving * broken * 0.55;
		color = mix(color, INK, joint * 0.18);
		color = mix(color, INK, stroke(site_sd, 0.0016) * 0.75);
		color = mix(color, INK, stroke(house_sd, 0.0020) * 0.92);
		color = mix(color, INK, stroke(pool_sd, 0.0014) * 0.88);
		float canopy_edge = smoothstep(0.25, 0.55, plants) * (1.0 - smoothstep(0.55, 0.9, plants));
		color = mix(color, INK, canopy_edge * broken * 0.28);
	}

	COLOR = vec4(clamp(color, 0.0, 1.0), 1.0);
}
