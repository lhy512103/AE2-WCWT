package com.lhy.wcwt.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NeoEcoApiCompatTest {
    @Test
    void absentEcoDoesNotLinkItsApiOrBlockOtherUploadSystems() {
        assertFalse(NeoEcoApiCompat.isAvailable());
        assertFalse(NeoEcoApiCompat.isEcoPatternProvider(null));
        assertNull(NeoEcoApiCompat.patternStorageSystem(null));
        assertFalse(NeoEcoApiCompat.uploadPatternToEcoStorage(null, null));
    }
}
