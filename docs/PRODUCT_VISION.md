# Plan Trace — product vision

Version 2 · September 9, 2026 · Proposed north star

The output ambition now includes a deterministic Northstar-style presentation renderer: architectural line hierarchy, pale material washes, designed vegetation and consistent soft depth, driven by editable semantic objects. See the [Northstar visual specification](NORTHSTAR_VISUAL_SPEC.md) for concrete rules and the [ordered execution plan](EXECUTION_PLAN.md) for implementation status. Technical, Graphic and Northstar modes must preserve exactly the same plan geometry. Implementation is now authorized; 3D remains paused.

## The goal

**Make Plan Trace the tablet you reach for whenever a pool, patio or landscape idea needs to become a clear, accurately measured, beautifully presented plan.**

It should feel like drawing on tracing paper at a well-organized drafting desk, with the ability to revise every decision. The S Pen should stay in your hand from the first loose sketch through precise layout, alternative designs, quantities and a finished client sheet.

The defining promise: **Sketch naturally. Refine precisely. Revise fearlessly. Deliver faithfully.**

Success is a complete job: import a client's plan, verify scale, explore two alternatives, refine their geometry, explain the differences, save the decision and hand over a faithful drawing. This is a proposed destination, not a description of today's build. All current work remains focused on 2D; 3D is outside these release gates.

## A day using the finished app

You open a project at a client's table. The last sheet and view are already there. You import a survey, mark a known distance, and verify a second reference. The app clearly distinguishes a verified plan from an uncalibrated image.

You add a tracing layer and sketch a pool and patio loosely. A held stroke can become a straight line when you choose that assistance. You select the patio and enter 20 ft by 15 ft. Its dimensions and area update. A setback guide snaps the edge into position without changing your other work.

Hold the S Pen button, select the steps and shift them. Release it and continue drawing with the same pen, color and width. Nothing is accidentally erased. A mistake takes one Undo.

Duplicate the design as Option B. Widen the patio, move the spa and compare the alternatives over the same base plan. Each option has its own annotations and quantities. You can explain the additional paving area without counting or redrawing it.

Switch to Present. Tool clutter recedes; the plan fills the screen. Show the alternatives, enlarge a detail and add a temporary discussion mark. Promote that mark to a saved revision only when intended.

Prepare a sheet with your title block, scale, dimensions and material legend. The export preview is the actual output layout. Save the PDF and reopen it: the plan and marks align exactly. Close the app knowing the editable project, named version and backup are recoverable.

## What makes this app worth choosing

Plan Trace should excel at the connection between expressive sketching and measured outdoor-space planning. Its advantages should be the ease of changing a design, a tablet interface that respects the pen, useful pool/patio/landscape objects, and confidence in the saved and exported result.

Premium means fewer interruptions, clearer decisions and less repeated work. A beautiful toolbar is valuable when it helps that happen. A feature earns its place by helping someone create, refine, compare, communicate or recover a real design.

## Visual identity: a calm drafting desk

The drawing is the visual center. Use warm white paper, a quiet neutral surround, dark graphite controls and one restrained blueprint-blue interaction accent. Drawing colors belong to the work; the interface should not compete with them.

Proposed starting palette: paper `#FBFAF7`, workspace `#E9E7E2`, panels `#FFFFFF`, primary text `#252A31`, secondary text `#59616B`, accent `#2463B5`. Validate contrast in the actual design. Statuses use words and icons as well as color. An unverified scale is visibly labeled, not merely tinted.

Use clear Android-native sans-serif typography, tabular numbers for dimensions, consistent icon strokes and compact rectangular controls with gently rounded corners. Panels have a subtle border and minimal shadow. Avoid a screen full of oversized pills, glossy effects or permanent floating buttons over the work.

Default paper is clean. Optional grid, dots and a subtle paper texture are view preferences; decorative texture must not interfere with crisp lines or print by default. Dark interface chrome can surround a light sheet. Outdoor/high-contrast mode increases legibility without changing drawing colors in the file.

Motion explains a change: a panel slides from its dock, an option comparison fades between states, selection responds immediately. It never delays a pen stroke. Respect reduced-motion settings.

## Workspace and information architecture

**Project → sheet → design option → layers → objects.** A project can contain several source documents and sheets. Each sheet owns its calibration and view. An option contains its editable design layers over a shared, protected base. Finished exports and named versions belong to the project as well.

The default landscape workspace:

| Region | Purpose |
|---|---|
| Slim top bar | Project/sheet name, saved status, Undo/Redo, Present and Export. Project navigation stays reachable. |
| Tool rail | Select, Pen, Geometry, Dimension, Note, Erase. A compact favorites area holds frequently used styles. |
| Context strip | The active tool's few relevant choices: width/color, line constraint, dimension format or selection actions. |
| Right dock | Switch among Pages, Layers, Properties and Library. Open one primary panel at a time. |
| Small view controls | Fit Page, zoom/readout, reset rotation and calibration status. They never cover essential geometry. |
| Canvas | Continuous surrounding workspace with explicit sheet boundaries and export frames. |

In portrait, the inspector becomes a resizable bottom sheet and lower-frequency top-bar actions move into a labeled overflow menu. Essential tools remain reachable. Left-handed layout mirrors docks and positions temporary controls away from the pen hand. Preferences persist across rotation and restart.

Use a continuous workspace for exploration while keeping document pages and output frames explicit. Infinite space must not mean unclear page ownership or unpredictable export cropping.

## Interaction rules

- Select once to inspect. Tap an editable property to change it. Existing notes have an obvious Edit action and can also open by double-tap.
- Corner handles resize; an explicit handle rotates; dragging an object's body moves it. The opposite resize anchor stays fixed unless the user chooses another anchor.
- Numeric fields accept common feet/inches and decimal forms and show the interpreted value before committing. Units and rounding are explicit.
- Snaps identify what they attach to: endpoint, midpoint, alignment, perpendicular, grid or a chosen offset. Snapping can be temporarily suppressed. Precision assistance must be predictable.
- A floating selection toolbar never passes touches through to the canvas. Position it away from the hand and selection; use the inspector for longer forms.
- One completed user action is one Undo transaction. Layers, objects, option membership and related dimensions recover together.
- A visible Finish/Cancel pair accompanies unfinished paths. Changing tools cannot silently leave a ghost that looks saved.
- Locks protect all mutation routes. Tapping a locked object explains why it cannot change and offers a deliberate way to inspect its layer.
- No essential action exists only as a gesture. Contextual hints teach shortcuts without interrupting the first drawing.

## S Pen, barrel button and touch

### Default: drafting preset

| Input | Intended behavior |
|---|---|
| Pen contact | Use the current drawing/editing tool. |
| One finger, with no pen contact | Pan the canvas by default. Finger drawing is a clearly labeled alternative mode. |
| Two fingers | Pan and pinch zoom. Rotation is an explicit preference, initially locked for plan work; Reset Rotation is always available. |
| Hold barrel button, then tap an object | Temporarily select it without drawing. |
| Hold barrel button, then drag empty space | Lasso selection. The scope defaults to visible, unlocked layers and is adjustable. |
| Hold barrel button, drag an already selected object | Move that selection as one reversible action. |
| Release barrel button | Restore the previous tool and its settings. Keep the selection available in the inspector; the next normal pen stroke resumes the original tool. |
| Pen hover, where supported | Preview brush footprint or the target/handle that would receive contact. Hover alone never edits. |

Button handling must distinguish press, pen contact, release and cancellation. Releasing during contact finishes the current temporary gesture at pen-up; it must not turn the remainder into an accidental ink stroke. Pressing during an existing ink stroke does not convert or erase that stroke; the temporary behavior begins with the next contact. Leaving the app or losing focus resets temporary input state safely.

### Alternative: sketching preset

Users can map hold-button to temporary erasing instead. Provide distinct object-erase and stroke-segment-erase tools with understandable previews. Default erasing respects locks and never touches the base document. Releasing returns to the previous pen. All changes undo as a whole gesture.

Also offer configurable Select, Erase, Pan and Quick Tools assignments. A compact Quick Tools palette is optional, with a visible toolbar entry as a fallback. Do not overload the default with separate single-click, double-click and long-press meanings before proving the simpler interaction feels excellent.

Where the OS/device reports a usable hover-button action, an optional press can open Quick Tools near the pointer, offset away from the hand. Do not require Bluetooth remote gestures, assume every S Pen delivers the same events, or silently change system pen settings. Setup includes a small input check and explains unavailable shortcuts. Unsupported hardware retains the complete visible UI.

Precision ink uses consistent line weights by default. Expressive pens can use pressure and tilt, with adjustable curves and smoothing. A brush preset owns those settings. Keep navigation from jumping while the pen is drawing, distinguish intended fingers from palm contact, and correctly cancel marks rejected by the platform.

These are proposed interactions. Physical S Pen, palm and button behavior still require device testing. Android documents stylus pressure, tilt, hover and palm/cancellation handling; available behavior must be established on each supported target. [Android stylus guidance](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input)

## Capabilities of the finished 2D product

### Draw and refine

Editable vector strokes alongside expressive sketch tools; exact primitives; polylines, arcs and curves; vertex editing; width/height/radius/angle entry; rotate, mirror, offset and fillet; grouping, multiselect and alignment. Dimensions attach to geometry and update with it. Precision edits preserve the user's chosen anchor and intent.

### Organize and explore

Clear layers with names, thumbnails, visibility, lock, opacity and bounded action menus. Reassign objects, isolate a layer and duplicate a group. Named design options support side-by-side and overlay comparison. Shared base changes require an alignment preview and explicit treatment of each option's calibration.

### Understand the design

Closed regions report area and perimeter. A patio can carry material, unit and optional waste-factor information. A material schedule shows where each quantity came from and updates with edits. Symbols for pools, spas, steps, walls, planting, furniture and equipment are editable, dimensioned and reusable. Domain templates accelerate drawing without implying engineering or code approval.

### Annotate and present

Editable text, leaders, callouts, dimension styles, hatches and controlled fills. Presentation mode offers clean full-screen viewing, option comparison and a temporary pointer/markup layer. Sheet templates carry project/client details, date, revision, legend, north direction, logo and a mathematically correct scale bar. Print output preserves line-weight intent and text readability.

### Save and hand off

Local-first editing works without connectivity. Saved/saving/error states are truthful. Named versions, a recoverable trash and backup/restore protect editable work. PDF/PNG export offers explicit bounds and scale/resolution, faithful preview, Save to Files and Share. Portable editable project files come before optional cloud sync. Later editable interchange should be driven by actual handoff needs and document any conversion limitations.

## How the references shape the design

Concepts is a reference for editable drawing, configurable tool access, separation of finger and stylus roles, and precision properties close to the canvas. Its settings document configurable input and supported stylus shortcuts. Adopt those useful interaction principles and standard patterns, adapting their placement and defaults to plan work. Platform feature parity is not assumed. [Concepts settings](https://concepts.app/en/manual/settings) · [Concepts selection](https://concepts.app/en/manual/selection)

Morpholio Trace is a reference for treating tracing, drawing to scale and architectural communication as a connected design workflow. Use that sheet-and-overlay mentality to make Plan Trace feel at home with a real plan set. [Morpholio Trace user guide](https://morpholioapps.com/userguide/trace/)

Use familiar patterns directly when they fit: layer visibility/locks, lasso selection, resize handles, rulers, tool presets and temporary modifiers. Develop Plan Trace's own coherent layout, assets and domain workflow. A wheel or ruler should be included because it improves this task; resemblance alone is not a design requirement.

## Quality bar: proposed measurable targets

These are future acceptance targets, not current measured results. Establish representative fixtures and measure on the Tab S10 FE before committing performance claims.

| Area | Target |
|---|---|
| Trust | All 13 current QA findings resolved and their acceptance cases passing. No completed edit lost in defined save/restart/recovery tests. |
| Geometry | Exact dimensions remain exact within a documented numeric tolerance; canvas and outputs share one coordinate model. Export landmark error below one output pixel at the selected raster resolution. |
| Undo | Every supported gesture/command restores the complete prior state in one action, including valid layer and page references. |
| Pen feel | Aim for measured 95th-percentile ink latency below 30 ms on the target tablet. Record hardware/OS limits and a stable-frame baseline instead of claiming universal performance. |
| Responsiveness | Aim for 60 fps during ordinary pan/zoom on an agreed representative sheet; no blocking save or panel-opening pauses during drawing. Publish stress limits separately. |
| First use | A new user can import, calibrate, draw one exact-size shape and save a faithful PDF within 10 minutes with only in-app guidance. |
| Daily use | A practiced user can create, compare and export two simple patio options within 15 minutes from a supplied plan, without recovery workarounds. |
| Reachability | Essential controls work in both orientations and handedness layouts, with readable text, accessible names and at least 48 dp touch targets where applicable. |
| Portability | An exported editable project reimports with matching pages, geometry, calibration, layers, notes and options. |

## Path from the tested build

1. **Trustworthy foundation:** identify/reproduce the exact APK source; fix calibration input ownership, toolbar interception, lock enforcement, atomic history and layer deletion recovery. Preserve project integrity.
2. **Faithful documents:** unify canvas/import/export coordinates; correct printed scale; isolate PDF pages; make save/share reliable; add recovery and regression fixtures.
3. **Excellent editing:** exact geometry, real handles, note revision, clear path completion, responsive docks, fit/reset view and the physically validated pen/button contract.
4. **Complete professional workflow:** options, reusable symbols, linked quantities, presentation, sheet templates and portable backups.
5. **Refinement:** tune real-device performance, accessibility, outdoor use, onboarding and repeated-job shortcuts. Advanced integrations follow demonstrated demand.

The evidence and required regression scenarios remain in the [tablet QA reports and premium audit](https://github.com/CRisdon45/Plan_Trace/tree/qa/tablet-premium-audit-2026-09-09/docs/qa/2026-09-09). This vision complements those reports; it does not replace their repair priorities.

## Decision rule

When deciding what to build, ask: **Does this help the user turn an idea into a precise, revisable, understandable plan with less effort and more confidence?**

The finished app should make the user think: **“I can work out the idea here, change my mind here, and deliver it from here.”**
