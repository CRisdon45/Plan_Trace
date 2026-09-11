# Current state and next-session handoff

Updated 2026-09-11. Main contains this routing context, not the unfinished application. Independent development and GitHub virtual-device tests are authorized; preserve the interrupted physical tablet test, installed app, data and signing key. Verify live refs and concurrent work.

## Continue on the active branch

**feat/project-geometry-seam**, PR #4 into feat/off-tablet-integrity. PR #3 targets the 2D foundation and PR #2 targets main. No application PR was merged in this session.

Read the [active checkpoint](https://github.com/CRisdon45/Plan_Trace/blob/feat/project-geometry-seam/docs/CURRENT_STATE.md). Saved documentation commit: **0bf4fa0dab824ec92d65866355934dc9e4cb6950**. Tested application: **571a28c97c9ad8be7666f25eb5bbba5d83fc94a9**. The documentation report was read back successfully; it is not a new application build.

## Latest outcome

View -> Site -> House or Property now creates real existing-site vectors over the calibrated image. Mark corners with the pen, optionally align successive edges to the first segment, and deliberately close. No typed dimensions are required. Draft corners remain temporary; closure is one object and one Undo step.

The house/property objects start locked and marked TRACED. Site controls provide deliberate unlock/edit/relock. Their original image registration remains provenance; moving/rescaling/removing the raster does not silently move these vectors. Alignment-review notices preserve that distinction. Hiding the image leaves the house and property outlines in the actual renderer and export.

## Verified evidence

[Unit/build run 34616514170](https://github.com/CRisdon45/Plan_Trace/actions/runs/34616514170): **182 tests, zero failures/errors/skips, successful debug assembly**. [Android run 34616514212](https://github.com/CRisdon45/Plan_Trace/actions/runs/34616514212): real app/instrumentation builds and **nine scenarios passed**. Downloaded XML/results and all 20 active-window capture records were checked. The completed four-object, format-5 draft was byte-identical across saved force-stop/reopen. Actual reopened, source-hidden, warning and vector-export images were opened.

The first replay exposed source-drag cancellation caused by a preview-driven warning resizing the canvas. Committed-state notices and two layout regressions corrected it. The vector export still has a house notice touching a property line; label layout is not accepted presentation polish. Scope, failures and hashes are in the [site-outline report](https://github.com/CRisdon45/Plan_Trace/blob/0bf4fa0dab824ec92d65866355934dc9e4cb6950/docs/SITE_OUTLINE_IMPLEMENTATION.md).

## Direction and next outcome

Cody rarely wants to type dimensions. Direct pen manipulation, dependable snapping and context are primary, with typed values only a fallback. Next, use this editable site to author proposed pool/deck geometry where the pen indicates instead of always inserting predefined starters. Preserve coping, site protection, one-operation Undo and save/reopen through the same authority. Do not grow a disconnected tracing editor or another numeric form.

## Boundaries

Format 5 reads 1–4 without inventing site metadata; older builds cannot read newer saves. Legacy Room projects remain separate. One local draft and session-only Undo are not multi-project/portable recovery. Site outlines are straight, closed references, not automatic recognition, curved/open wall paths, survey boundaries or certified quantities. PDF intake, rotation, full landscape scope, attached features and Northstar quality remain unfinished.

Physical S Pen/palm/barrel feel, full draft creation in portrait/compact layouts, accessibility/handedness and hardware performance remain unverified. Existing command-layout tests are not complete drafting acceptance. No physical installation, user-data clearing, signing workaround, app merge, release/APK publication, private client/reference publication, permission change, runtime AI, framework/repository merger or 3D work occurred. Earlier tooling/service warnings remain open. Update this router only from actual successful writes and checked evidence.
