# Godot Northstar baker

Presentation-only watercolor bake. Plan geometry, pen, files and Technical/Graphic
stay in Android. This project is a GPU wash that GitHub Actions (and a local
Xvfb session) can run. It is **not** yet an Android plugin inside the app.

## Why this exists

The Java grass painter is the live Android path. Close-up 9/10 needs a
continuous wet field at 2k+, which a fragment shader can do in one pass.
This folder is that baker. Embedding Godot as a Plan Trace viewport is a
later integration: same shader, different host.

`--headless` uses Godot's dummy renderer and writes empty images. Use Xvfb
(or a real display) with `--rendering-method gl_compatibility`.

## Bake

```sh
tools/godot-northstar/bake.sh build/godot-grass ground-study
```

Writes `grass.png`, stages `01`–`05`, and three 720px crops. Default size is
2048×1434. Typical llvmpipe bake is about 20 seconds here.

Godot 4.7.2 is downloaded on demand unless `GODOT` points at a binary.

## Not in this folder

- S Pen / radial / `ProjectDesign`
- Replacing `NorthstarGrassPaint.java` on device
- Water or travertine (same baker shape, later materials)
