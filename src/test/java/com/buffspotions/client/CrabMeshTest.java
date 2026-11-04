package com.buffspotions.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CrabMeshTest {
    @Test
    void babyLegsKeepTheOutsideTextureOnAReversedCube() {
        assertTrue(CrabMesh.babyLegsHaveInnerFaces());
    }
}
