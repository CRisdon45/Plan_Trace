# Site image intake, registration and independent distance evidence

This is the first image-backed site setup in the actual design workspace. It is not the complete SITE-01 benchmark: editable house/property tracing, PDF intake and full site design still follow. App and source evidence stay separate from the visual Northstar target.

## Existing-yard source in the same project

View -> Site (or its equivalent compact-list command) opens the source controls. PNG/JPEG intake creates a bounded, app-owned normalized PNG rather than relying solely on a provider URI. JPEG orientation is handled before defining the image coordinates. Selected original bytes are retained locally by content hash; no original filename or location tags are added to project JSON. These local assets are not portable backup or a permission to publish client imagery.

The source is upright with explicit uniform image-to-world scale and translation. It has no inferred georeferencing, rotation, perspective correction or maps-provider integration. Ordinary design gestures cannot move the source. Deliberate source movement, hide/show, removal and restoration use the same command/Undo path, without resizing or moving existing proposed objects.

A two-point known-distance calibration holds its first world point fixed while changing image scale. Reference-distance presets avoid requiring keyboard entry; another value remains an optional fallback. Source scale is initially unset and is never presented as surveyed just because a known distance was entered. The owner's pen-first clarification remains authoritative: routine keyboard entry is not the intended design workflow.

The current bounds are 32 MiB of selected input, a supported PNG/JPEG header up to 100 megapixels with no dimension above 20,000, and a normalized image no larger than 4,096 on an edge or 4 million pixels. Reference points must be at least 8 normalized pixels apart, with a known distance between 1 inch and 1,000 feet. These are implementation budgets and validation limits, not recommended measurement practices or hardware benchmarks.

## Check another distance without changing the design

After calibration, the user can select a different image segment and provide its known distance. The app reports the distance implied by the current image scale, the supplied reference, and the signed discrepancy in feet and percent. The comparison is optional. It does not recalibrate, move the source, resize a pool, or automatically classify site geometry as field verified. No automatic agreement threshold or whole-site accuracy label is introduced.

A deliberate mismatch remains visible and is included in source-present output notices. Selecting the original calibration pair again, including in reverse order, is rejected rather than calling that a second check. This is only evidence from another user-selected distance, not proof that every part of an image is undistorted or that the input reference is independently measured.

Moving or hiding the source retains its check because those operations do not change scale. Recalibrating clears the old comparison so it cannot falsely validate a new scale. The check stores only authored points and reference distance; measured discrepancy is recomputed. It is one reversible project edit and survives verified save/reopen.

## Persistence, compatibility and output

Format 4 stores optional source registration, original calibration and second-distance evidence. It reads formats 1-3 without inventing sources or checks. Older builds cannot read later format-4 saves. The legacy Room drawings are not migrated. The single opt-in draft still has session-local Undo, serial atomic saves and verified readback, not multi-project management, a portable asset bundle or recovery of an edit killed before saving finishes.

Shared source/world-to-output coordinates drive the existing native renderer and PNG/PDF export. Exported source opacity matches the workspace. Source-present exports retain scale/confidence information even when automatic design dimensions are hidden. The north arrow is suppressed when an image is attached because true orientation has not been established. The existing auto-fit can still change framing with labels; fixed output frames are separate work.

A source-only draft can be exported. Missing or checksum-changed owned assets produce an explicit failure rather than a silently incomplete source-backed export. This does not yet provide relinking, controlled source replacement, asset garbage collection or export/share-chooser UI certification.

## Completed baseline verification

Application eb0b62f596b5eea98016fd3511fd57d0677c2404 completed [unit/build run 34542680701](https://github.com/CRisdon45/Plan_Trace/actions/runs/34542680701) with 155 tests, zero failures/errors/skips and successful debug assembly. [Android run 34542680695](https://github.com/CRisdon45/Plan_Trace/actions/runs/34542680695) completed seven separately invoked scenarios. Both archives were downloaded and checked, and the real source-backed workspace, PNG and rendered PDF page were opened.

Baseline unit artifact 10177973004 ZIP SHA-256: a61c501b66e45353a6453b6c5abc088266edabfd02033a44fb49aa38049c21fc. Baseline emulator artifact 10178047216: ba9a137fa422f1c648eec861dd7ee65e8e183f701705330a7acd048eb840e3d5. The 1,709-byte source-backed canonical file at revision 25 was identical across completed-save process restart: 8eccd6036c6f5e241f4124019529c4dc9cf0be44f54de7a36b1b4febdb42bace.

## Second-distance verification

Application 765ef687c91ec4482213f88c3fadc647055d978d completed [unit/build run 34544818262](https://github.com/CRisdon45/Plan_Trace/actions/runs/34544818262) with **165 tests, zero failures/errors/skips and successful debug assembly**. The ten new cases cover independent readings, mismatch retention, duplicate/invalid checks, invalidation after recalibration, Undo/Redo, JSON/atomic storage and output notices. Downloaded XML totals matched the job log.

Its [Android run 34544818274](https://github.com/CRisdon45/Plan_Trace/actions/runs/34544818274) reported all seven scenario tests successful, including the extended match/mismatch and saved-restart assertions. Its canonical 1,843-byte site-backed file at revision 29 was byte-identical across completed-save process restart, hash b1b0012570e520d17abe9d6eb314dea8ba46fa9e6d612bfe164bb9c7512958a5. But actual captures showed a Pixel Launcher ANR dialog over the application. **That run is not unobstructed visual evidence.** Its direct PNG and rendered PDF outputs were opened separately and showed the discrepancy notice and registered geometry correctly.

Unit ZIP 10178749964 verified against 4405caa628bf03e05965d4f207dfec7d3bbd7aaf10792519f30c453ad056c94b. Obstructed-emulator ZIP 10178805788 verified against 3be0075f29ad2fc83059e262124e026376905fb563d6589bf24b884c02516272.

QA-only revision 884f0ef0d23b04751dc9b9b84f99b08564f12503 adds an active-window package guard to every accepted in-app capture. It retains the raw screenshot and accessibility-window tree, then fails rather than silently dismissing an unrelated or application error dialog. Source/restart cases run before display reconfiguration; the harness keeps the test app foreground and allows a short settle interval while applying portrait/compact configurations. These are harness changes, not a cause-specific Pixel Launcher fix, and production source/unit tests are unchanged from 765ef687.

The final guarded replay at **884f0ef0d23b04751dc9b9b84f99b08564f12503** completed [unit/build run 34545769738](https://github.com/CRisdon45/Plan_Trace/actions/runs/34545769738) with **165 tests, zero failures/errors/skips and successful debug assembly**, and [Android run 34545769754](https://github.com/CRisdon45/Plan_Trace/actions/runs/34545769754) with **all seven scenarios passed**. Both final archives were downloaded; XML counts, scenario results and all 15 active-window capture records were checked. Each accepted capture record identifies the application package, not a system error dialog. The actual source-check, reopened-workspace and compact captures were opened and were unobstructed. Direct PNG and rendered PDF output were also opened. Manual visual review remains necessary; the guard is not a pixel-golden test or exhaustive overlay detector.

Final unit artifact 10179082510 ZIP SHA-256: 94a7719cead340166eae66734826ae421ef17593a372b742cacb275d67826eba. Final Android artifact 10179147953: d43ae7e17e6a7d2ec26aade862a0976ac3188e5ca3d942875e345958634e923f. Seven-day artifact retention is not permanent availability.

The final source-backed file is 1,843 bytes, format 4, revision 29, and byte-identical across force-stop/reopen after verified Saved: a3da97721716686a39ba967df476feaa38b71267a387eda633d8b6238cd79c00. The second check survives that restart. Its expected image distance is approximately 20 feet; an intentionally supplied 30-foot reference reports approximately -10 feet (-33.33%) without altering registration or proposed objects. Both matching and mismatching comparisons, Undo/Redo and save/reopen are exercised through the real app.

Final source-check screenshot SHA-256: 4311daa297b0c95912413c4c7168b5e1dca2f7470cee42f3fb83cd6b38691579. Reopened workspace: 87379df8cc339ec6879bb50569943a6d1c32d8f9d6254f5e688158b488a5e180. Registered PNG: 9e5b53e095a55afbb0e01e8060b29879d30f42ce8286934328f3d8b25198cf8f. Registered PDF: c51e3cc4eff3f67213be60cdc446e7e94052027648ae45df084ecdee32393783.

The virtual environment is API 35 x86_64, with landscape 1600x1000 at density 240, portrait 1000x1600 at 240, and compact 800x1400 at 400. The seven Android scenarios are separate from the 165-test unit count. The external document picker and share chooser are not automated; the import callback, UI reference gestures and direct real export paths are. The final crash buffer is empty, not proof of all-flow crash freedom. No physical stylus or tablet performance claim follows.

## Failures and honest evidence

The first image-intake implementation failed one of 153 unit tests because the old 85-percent underlay opacity changed output pixels. Its Android run passed the five earlier scenarios but exposed Cancel being intercepted as artwork input. The correction preserved the strict test assertions, matched opacity, moved artwork input handling off the controls' parent, and canceled transient source tools during selection/history/exit. The corrected baseline above passed; earlier failures were not relabeled as success.

The formerly reported radial documentation commit 786f644e1ec12f7f50bc28d738d13f36178d7e1c did not land. Its erroneous main/PR pointers are corrected by this handoff. Actual radial code/tests at 27ebdb2 remain real; RADIAL_IMPLEMENTATION.md records them without inventing a past documentation write.

Previously observed KSP/AWT background exceptions and inherited service/deprecation warnings remain open unless separately reproduced and fixed. A green build is not a warning-free log or physical pen certification. Captures are reviewed for system overlays, source/geometry alignment and legibility, not treated as Northstar approval. The synthetic scene deliberately contains overlapping test pools and a simple raster house. It is not a completed client layout or proof of collision-aware site design.

## Next bounded outcome and safety

Continue toward pen-created, editable existing-house and property outlines over a calibrated source, with deliberate closure, stable IDs, trace provenance, protection from incidental edits and save/reopen/output. Do not require repeated typed dimensions or expand the wheel for its own sake. The site source should support the project rather than become a disconnected tracing utility.

Still pending: system file-picker/provider-specific access, PDF source intake, source rotation, arbitrary site-outline authoring, registration rules for future traced objects, portable recovery, project alternatives, attached steps/shelves, shared surfaces, full landscape scope, physical S Pen/palm/barrel feel, hardware performance and final Northstar presentation. No construction approval or area takeoff is implied. No physical tablet, user app/data/signing key, visibility/permissions, private client assets, runtime AI, release or 3D work is changed by this milestone.
