# Godot Northstar baker

Presentation-only watercolor bake. Plan geometry, pen, files and Technical/Graphic
stay in Android. This project is a GPU wash that GitHub Actions (and a local
Xvfb session) can run. It is **not** yet an Android plugin inside the app.

## Contract

The live Android painter bakes a rectangle and `clipPath`s it. That is a cookie
cutter. This baker takes a **lot polygon and holes in UV** (`lots/study-lawn.json`)
and paints an inset, scalloped drying front *inside* that silhouette so a later
hard clip still reads as paint. Same inputs a Godot viewport plugin would get:
lot, holes, seed, material.

## Bake

```sh
tools/godot-northstar/bake.sh build/godot-grass ground-study
```

Writes `grass.png`, stages `01`–`05`, three 720px crops, and a five-cell
progression sheet. Default size is 2048×1434. Typical llvmpipe bake is about
20 seconds here. The shader is overlapping irregular washes with paper gaps,
not a metaball noise field.

`--headless` uses Godot's dummy renderer and writes empty images. Use Xvfb
(or a real display) with `--rendering-method gl_compatibility`.

Godot 4.7.2 is downloaded on demand unless `GODOT` points at a binary.

## Not in this folder

- S Pen / radial / `ProjectDesign`
- Replacing `NorthstarGrassPaint.java` on device
- Water or travertine (same baker shape, later materials)
