package com.lhy.wcwt.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lhy.wcwt.helpers.WcwtToolkitHotbarState.Bar;
import org.junit.jupiter.api.Test;

class WcwtToolkitHotbarStateTest {
    @Test
    void leftPageMapsToToolkitCellsZeroToEight() {
        assertEquals(0, WcwtToolkitHotbarState.toolkitIndex(Bar.LEFT, 0));
        assertEquals(8, WcwtToolkitHotbarState.toolkitIndex(Bar.LEFT, 8));
    }

    @Test
    void rightPageMapsToToolkitCellsNineToSeventeen() {
        assertEquals(9, WcwtToolkitHotbarState.toolkitIndex(Bar.RIGHT, 0));
        assertEquals(17, WcwtToolkitHotbarState.toolkitIndex(Bar.RIGHT, 8));
    }

    @Test
    void vanillaPageHasNoToolkitCell() {
        assertEquals(-1, WcwtToolkitHotbarState.toolkitIndex(Bar.CENTER, 4));
    }

    @Test
    void slotOutsideAPageWrapsInsteadOfLeavingTheBar() {
        assertEquals(1, WcwtToolkitHotbarState.toolkitIndex(Bar.LEFT, 10));
        assertEquals(17, WcwtToolkitHotbarState.toolkitIndex(Bar.RIGHT, -1));
    }

    @Test
    void onlyExtraBarCellsAreValidIndices() {
        assertTrue(WcwtToolkitHotbarState.isValidToolkitIndex(0));
        assertTrue(WcwtToolkitHotbarState.isValidToolkitIndex(WcwtToolkitAccess.HOTBAR_SLOTS - 1));
        assertFalse(WcwtToolkitHotbarState.isValidToolkitIndex(-1));
        assertFalse(WcwtToolkitHotbarState.isValidToolkitIndex(WcwtToolkitAccess.HOTBAR_SLOTS));
    }
}
