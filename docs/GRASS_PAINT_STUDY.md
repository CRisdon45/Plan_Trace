# Grass paint: visual revision, 2026-09-12

The owner rejected the previous `f52ea32` grass as **5/10**. The target is an
honest **9/10**, including the minute paint details in the supplied close-ups.
Passing regression tests does not establish that score. Water, decking and the
rest of Northstar are outside this revision.

## Steps 4 and 5 follow-up

The owner found `5bf421b` much better, but judged it to have reached only step 3.
The finishing stages now have substantive, separate jobs. The first three stages
remain pixel-identical in the local full-resolution study. Step 4 lifts selected
parts of existing glazes back toward the retained underpainting, then deposits
smaller, connected scalloped forms with their own drying fronts. Step 5 adds
middle-sized green marks, a stronger near-black range, fine curved strokes and
small lighter marks. Density follows the painted islands and varies spatially.
Final perimeter deposits follow the actual Android path, including concave
edges, rather than a rectangle baked into the texture.

The first follow-up attempt used a warped scalar field and produced stretched
ribbons. It was rejected on visual inspection. Related polygon masks create the
final lifting structure instead. Three local iterations adjusted connectivity
and final mark strength; two object seeds were inspected. The desktop adapter
now includes a full-frame 3/4/5 comparison. A renderer test captures those stages
and checks substantial lifting, new deposition and an increased dark range.
Those tests establish distinct operations, not a 9/10 quality score. Android CI
passed all 303 unit tests; the captured stage pixels exactly match the desktop
production study. Full-surface and concave Android renders were also inspected. All 20 emulator
scenarios passed; live, exported and reopened grass captures show the finished
paint. Exact run and artifact identities are recorded in CURRENT_STATE.

## What the reference requires

The supplied study depicts thin yellow-green underpainting; connected, rounded
and scalloped green wash fronts; local darker sediment just inside those fronts;
small pale reserves; overlapping glazes with different strengths; and varied,
clustered final deposits. Some edges are sharply dry while adjacent segments
soften slightly. Large quiet passages remain between the deposits. These are
observations of an AI-generated reference, not claims about an actual artist's
process. No reference pixels are embedded, traced or published.

Review both a whole surface at working size and its close detail:

| Criterion | Failure to look for |
| --- | --- |
| Layer construction | Separate stamped blobs, equally outlined everywhere |
| Drying fronts | Dark outlines unrelated to the wet shape; no interior-to-edge buildup |
| Translucency | Flat opaque green patches, muddy overlaps, yellow cast |
| Paper and small reserves | Final uniform noise, regular lattice holes, white paint pasted on top |
| Fine deposits | Equally sized confetti, aligned marks, uniform density |
| Working canvas | Detail only in a separate demonstration, blank exports, frozen drawing |

## Independently implemented method

`NorthstarGrassPaint.java` owns the production pixels. `NorthstarGroundMaterials`
adapts them to Android's exact clipping, opacity and prefiltered image cache.
`tools/qa/GrassPaintStudy.java` only writes those same pixels and intermediate
stages to PNG; it contains no alternative painting implementation.

The old grass path used a few displaced polygons and separate rim strokes. This
revision follows the inherited variance and related translucent polygon layers
in Tyler Hobbs's 2017
[A Guide to Simulating Watercolor Paint with Generative Art](https://www.tylerxhobbs.com/words/a-guide-to-simulating-watercolor-paint-with-generative-art).
Rounded lobes establish the broad front, recursive Gaussian displacement supplies
irregular detail, and repeated related contours integrate fractional coverage.
Major glazes use 32 passes; small deposits use fewer. Local variance is inherited
through subdivision. Paper reserves affect individual glazes, not the finished
image as a white overlay.

Curtis et al.'s 1997
[Computer-Generated Watercolor](https://grail.cs.washington.edu/projects/watercolor/paper_small.pdf)
informs edge accumulation, shared paper interaction and ordered Kubelka-Munk
compositing. A blurred wet mask identifies each glaze's evaporating front. A
bounded fraction of pigment moves from the interior into that front, with total
coverage mass preserved and no pigment placed on dry mask pixels. Spatially
varying front strength avoids a uniform border. Paper texture then modulates
deposition. Reflectance/transmittance lookup tables combine each RGB glaze with
the paint below it. Coefficients are artist-selected, not measured spectral
pigments. This is a finite drying approximation, **not the paper's full
shallow-water, adsorption and capillary-flow simulation**. No upstream code,
external art, new dependency or runtime service was imported.

Seven local revisions addressed excessive yellow, conspicuous broad outlines,
angular lobes, insufficient sediment, aligned flecks, lattice-shaped reserves,
overly flat underpainting, and then excessive grain in the initial wash. These
are iterations, not seven passed quality gates.
The current model retains visible stylization; owner acceptance is required before
recording the requested quality as achieved.

## Live behavior and verification boundary

Grass preparation on a hardware canvas uses one worker with at most three pending
washes. The initial flat underpainting remains visible while a cold wash is
prepared; completion invalidates the actual Compose drawing through observed
state. Stable cached paint follows movement and zoom. Software output waits for
a matching pending wash, or computes it directly, so exports never retain the
temporary underpainting. Resolution remains capped at a 1024-pixel longest side,
with prefiltered levels included in the existing 24 MiB ground cache.

The new paint includes its own underpainting, so the old turf base fill is skipped
in Northstar. Surface opacity is applied once to the finished grass. Paving and
water keep their previous rendering paths. Geometry, serialized project state,
appearance selection and signing are not modified.

Local Java compilation and production-pixel inspection run without an Android
SDK. Typical complete diagnostic generation took about 6.6 seconds on this host,
including five PNG stage captures; that is not a tablet benchmark. Cold generation
is appreciable, hence the worker. Actual Android build, clipping/opacity and
persistence results are recorded in CURRENT_STATE: app `f11d5fd` passed 303 unit
tests, nine policy checks and all 20 emulator scenarios. Its full grass surface,
concave clip, live workspace, reopened canvas and PNG export were inspected. Hardware frame
time, physical S Pen behavior and a 9/10 visual acceptance remain unverified.

To reproduce the isolated production study with a JDK:

```sh
javac -d build/grass-classes app/src/main/java/com/example/engine/NorthstarGrassPaint.java tools/qa/GrassPaintStudy.java
java -cp build/grass-classes GrassPaintStudy build/grass-study ground-study
```

The optional second argument changes the object identity seed. Inspect multiple
seeds, full frames and the actual Android captures; do not choose one flattering
crop as proof of the whole renderer.
