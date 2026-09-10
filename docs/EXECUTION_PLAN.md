# Outcome roadmap

Revised 2026-09-10. This is the order of intended outcomes, not an instruction to ignore the current session's scope. [CURRENT_STATE.md](CURRENT_STATE.md) owns status, authorization and the immediate resume point. The superseded stage list/work log is linked in [HISTORY.md](HISTORY.md).

| Gate | Outcome | Required evidence |
| --- | --- | --- |
| Q0 | Trust the existing application checkpoint | Isolated development build, relevant automated checks, latest example on the physical tablet, accessible PNG measurement control, actual PNG/PDF review with measurements on/off, save/reopen and Undo checks. Preserve original app/data. |
| Q1 | Establish a coherent design seam | Small project-owned geometry/relationship contract, safe treatment of existing per-page data, synthetic straight and concave/convex cases, source/derived quantity distinction, and a documented recovery/migration approach before stored data changes. |
| Q2 | Own the starting site | SITE-01 from imported imagery/plan or measured outline through calibration, editable house/patio/walls, design, reopen and output, without a preliminary Pool Studio step. Verify radial access and physical input alongside this work. |
| Q3 | Revise both kinds of full-yard design | Incremental ORTHO-01 and ORGANIC-01 workflows through the real editing/rendering paths: connected pool/coping/features, shared surfaces, retained anchors, related quantities and a representative Northstar level while working. |
| Q4 | Complete the meeting and handoff | Reusable adjustable assemblies, coherent planting/material scope, alternatives, client presentation, portable project recovery and faithful output/handoff. Compare full workflow effort with the owner's existing process. |

Q1-Q3 are short end-to-end slices, not a requirement to finish an enormous model layer before touching the canvas. Curves, radial interaction, visible quality and recovery should influence early decisions. Do not complete a generic editor, then bolt on pools, then postpone all style and workflow proof.

## Evidence contract

A package demonstrates a user outcome using the application's real command and renderer paths. Record source revision, fixture/version, build identity, relevant tests, actual canvas/export evidence and remaining limitations. A future automated replay should exercise those same paths, not a parallel polished demo.

Use distinct evidence labels: automated, synthetic runtime, physical-device, visual review, owner-accepted, or not run. A passing build is not pen certification. A mockup is not a rendered feature. A missing compiler/device/reference is a limitation, not a pass. No test or performance number is invented to fill a report.

Close discovered integrity defects when they block the next outcome, including source relinking/recovery, trustworthy dimensions/perimeters and atomic edits. Trace inherited runtime AI/service dependencies and remove unused ones as an explicitly tested implementation change. Root metadata cleanup alone does not complete that audit.

## Stop expanding when the result is not proven

Before multiplying tools, demonstrate create -> edit -> undo -> save -> reopen -> export for representative cases. Keep both shape families in the feedback loop. Test boundaries such as a narrow concave offset or a split edge with attachments, not just happy-path rectangles.

Performance budgets and time-saving targets follow a measured tablet/workflow baseline. Do not add SaaS features, production-engineering automation, 3D or a framework restart to make a milestone look bigger. Revisit a technical proposal when evidence supports a simpler path while preserving the accepted product constraints.
