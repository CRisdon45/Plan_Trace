# Decisions and their reasons

Recorded 2026-09-10 from the owner's clarification and delegated direction review. **Accepted product direction is not proof of implementation.** Technical proposals remain revisable through evidence. This is a compact decision record, not a second task list.

| ID | Status | Decision and reason |
| --- | --- | --- |
| D01 | Owner requirement | Design complete pool/landscape projects with the client on a tablet. Tracing is an entry route. The owner already completes substantial designs in first meetings, so optimize expert fluency rather than beginner onboarding or generic drafting completeness. |
| D02 | Owner requirement | Personal professional tool, not a commercial product. Do not add accounts, subscriptions, marketplace or team-product overhead without a demonstrated need. Personal use does not lower data-integrity standards. |
| D03 | Owner requirement | GPT-6 Astra is the requested development AI. No other AI service/agent is introduced for this project. The shipped app itself has no runtime AI and remains deterministic and useful offline. |
| D04 | Owner requirement | A near-pen radial command menu is primary, inspired by Concepts and Morpholio Trace. The old default temporary-Select contract is an implementation baseline, not the final product decision. Validate the hardware and retain visible equivalents. |
| D05 | Owner requirement | Northstar references govern appearance. Construction plans inform scope/geometry; finished photos inform physical relationships. Neither replaces the visual Northstar. Keep the working design attractive throughout editing. |
| D06 | Adopted direction | Model the physical project once, independently of source pages and output sheets. Duplicate editable yards across pool/landscape/presentation sheets would create avoidable inconsistency. Exact storage migration is not settled. |
| D07 | Adopted direction | Straight and freeform projects influence the foundation together. Early tests must include tangency, concave offsets, related surfaces and edge-attached features; rectangles alone are not a sufficient proof. |
| D08 | Owner requirement | 3D stays paused. Preserve useful height/depth meaning in 2D. Do not reopen a framework contest or resume old perspective branches by default. |
| D09 | Owner workflow constraint | No mandatory planting-bed model. Granite is eligible for planting unless explicitly excluded. Support no-plantings/plantings-only intent without adding meaningless surface boundaries. |
| D10 | Owner requirement | Keep the repo public for now; the owner cites compute availability as the reason. Do not change visibility, billing, sharing or repository permissions. Keep client/company originals, identifying derivatives and unapproved art out of public Git, issues, PRs, logs and artifacts. |
| D11 | Provisional technical boundary | Supply versioned objects/quantities to the Estimator rather than embedding a second pricing catalog in the renderer. Exact interchange and the relationship to other repositories remain open. No merger or framework migration is authorized by this boundary. |
| D12 | Working method | Maintain a small current context package and authentic end-to-end evidence. Completed prose, mockups, test counts and synthetic pen input are not substitutes for working features, visual acceptance or physical tablet trials. |
| D13 | Initial technical choice, revisable | The first project-geometry seam uses double-precision metres/y-up, stable edge IDs and exact line/circular-arc segments with versioned JSON. Existing page drawings are not migrated. Output still defaults to imperial labels. This is a bounded implementation seam, not an owner requirement to use metric UI, a finished persistence architecture or a commitment to exclude future spline types. See PROJECT_GEOMETRY_IMPLEMENTATION.md. |

## How decisions change

Ordinary implementation details are flexible within the product requirements. Challenge a direction when code, design or user evidence shows a better outcome. Explain the tradeoff and affected acceptance cases; update the relevant decision and contract in the same change rather than silently drifting. Reversing an explicit owner constraint requires the owner's approval. Do not invent approval or convert a recommendation into a historical user quote.

The old product hierarchy, optional radial palette, late freeform sequencing, and execution-stage priorities are superseded where they conflict with these decisions. Their historical evidence remains available through [HISTORY.md](HISTORY.md). The latest explicit owner request controls session scope, with [CURRENT_STATE.md](CURRENT_STATE.md) recording the current checkpoint.
