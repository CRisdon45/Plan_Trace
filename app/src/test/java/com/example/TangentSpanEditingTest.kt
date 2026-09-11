package com.example

import org.junit.Test

class TangentSpanEditingTest {
    @Test fun seededBiarcs() { TangentProofCases.seededBiarcs() }
    @Test fun specialCases() { TangentProofCases.specialCases() }
    @Test fun closedFamilies() { TangentProofCases.families() }
    @Test fun localAnchor() { TangentProofCases.localAnchor() }
    @Test fun wraparoundAnchor() { TangentProofCases.wraparoundAnchor() }
    @Test fun noInitialJump() { TangentProofCases.noInitialJump() }
    @Test fun localRadius() { TangentProofCases.radius() }
    @Test fun rejectedRequests() { TangentProofCases.rejection() }
    @Test fun translatedCoordinates() { TangentProofCases.translation() }
    @Test fun reversedWinding() { TangentProofCases.reversed() }
    @Test fun independentlySampledArea() { TangentProofCases.independentArea() }
}
