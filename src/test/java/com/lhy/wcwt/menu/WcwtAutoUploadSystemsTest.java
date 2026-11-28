package com.lhy.wcwt.menu;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.lhy.wcwt.menu.WcwtPatternUploadTargets.MatrixUploadState.AMBIGUOUS;
import static com.lhy.wcwt.menu.WcwtPatternUploadTargets.MatrixUploadState.DUPLICATE_RETURNED;
import static com.lhy.wcwt.menu.WcwtPatternUploadTargets.MatrixUploadState.FAILURE;
import static com.lhy.wcwt.menu.WcwtPatternUploadTargets.MatrixUploadState.UPLOADED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WcwtAutoUploadSystemsTest {
    @Test
    void noSystemDoesNotRunInsertionAndAllowsNamedProviderFallback() {
        var systems = new WcwtAutoUploadSystems();
        systems.add(null);
        var result = systems.uploadIfUnique(() -> {
            throw new AssertionError("No matrix target must not run insertion or duplicate handling");
        }, WcwtAutoUploadSystemsTest::unexpectedUpload, FAILURE, AMBIGUOUS);
        assertEquals(FAILURE, result);
        assertTrue(result.allowsProviderFallback());
    }

    @Test
    void oneSystemUploadsExactlyOnce() {
        var systems = new WcwtAutoUploadSystems();
        systems.add(new Object());
        AtomicInteger writes = new AtomicInteger();
        assertEquals(UPLOADED, systems.uploadIfUnique(() -> {
            writes.incrementAndGet();
            return UPLOADED;
        }, WcwtAutoUploadSystemsTest::unexpectedUpload, FAILURE, AMBIGUOUS));
        assertEquals(1, writes.get());
    }

    @Test
    void multiplePatternCoresOfOneClusterRemainOneSystem() {
        Object cluster = new Object();
        var firstCore = new PatternCore(cluster);
        var secondCore = new PatternCore(cluster);
        var superMatrixTerminal = new PatternCore(cluster);
        var systems = new WcwtAutoUploadSystems();
        systems.add(firstCore.backing());
        systems.add(secondCore.backing());
        systems.add(superMatrixTerminal.backing());
        AtomicInteger uploads = new AtomicInteger();
        assertEquals(UPLOADED, systems.uploadIfUnique(() -> {
            uploads.incrementAndGet();
            return UPLOADED;
        }, WcwtAutoUploadSystemsTest::unexpectedUpload, FAILURE, AMBIGUOUS));
        assertEquals(1, uploads.get());
    }

    @Test
    void ecoBusesShareTheirAggregatedStorageIdentity() {
        Object aggregatedStorage = new Object();
        var systems = new WcwtAutoUploadSystems();
        systems.addEco(aggregatedStorage);
        systems.addEco(aggregatedStorage);
        assertEquals(UPLOADED, systems.uploadIfUnique(WcwtAutoUploadSystemsTest::unexpectedUpload,
                () -> UPLOADED, FAILURE, AMBIGUOUS));
    }

    @Test
    void ecoSuccessAndDuplicateBothTakePriorityOverMeteoriteProviders() {
        var systems = new WcwtAutoUploadSystems();
        systems.addEco(new Object());
        systems.addMeteoriteProvider(new Object());
        assertEquals(UPLOADED, systems.uploadIfUnique(WcwtAutoUploadSystemsTest::unexpectedUpload,
                () -> UPLOADED, FAILURE, AMBIGUOUS));
        assertEquals(DUPLICATE_RETURNED, systems.uploadIfUnique(WcwtAutoUploadSystemsTest::unexpectedUpload,
                () -> DUPLICATE_RETURNED, FAILURE, AMBIGUOUS));
    }

    @Test
    void ecoRejectionCanFallBackToOneMeteoriteProvider() {
        var systems = new WcwtAutoUploadSystems();
        systems.addEco(new Object());
        systems.addMeteoriteProvider(new Object());
        assertEquals(UPLOADED, systems.uploadIfUnique(() -> UPLOADED, () -> FAILURE, FAILURE, AMBIGUOUS));
        systems.addMeteoriteProvider(new Object());
        assertEquals(AMBIGUOUS, systems.uploadIfUnique(WcwtAutoUploadSystemsTest::unexpectedUpload,
                () -> FAILURE, FAILURE, AMBIGUOUS));
    }

    @Test
    void twoSystemsCannotInsertReturnDuplicatesOrFallThroughToCraftingProvider() {
        var systems = new WcwtAutoUploadSystems();
        systems.add(new Object());
        systems.add(new Object());
        assertAmbiguousWithoutSideEffects(systems);
    }

    @Test
    void mixedEcoAndMatrixSystemsAreAmbiguousInEitherScanOrder() {
        Object ecoStorage = new Object();
        Object matrixCluster = new Object();
        var ecoFirst = new WcwtAutoUploadSystems();
        ecoFirst.addEco(ecoStorage);
        ecoFirst.add(matrixCluster);
        assertAmbiguousWithoutSideEffects(ecoFirst);

        var matrixFirst = new WcwtAutoUploadSystems();
        matrixFirst.add(matrixCluster);
        matrixFirst.addEco(ecoStorage);
        assertAmbiguousWithoutSideEffects(matrixFirst);
    }

    @Test
    void uncertainCandidateMakesSingleKnownSystemAmbiguous() {
        var systems = new WcwtAutoUploadSystems();
        systems.add(new Object());
        systems.markUncertain();
        assertAmbiguousWithoutSideEffects(systems);
    }

    @Test
    void distinctSystemsCannotMergeBecauseTheirNamesOrEqualityMatch() {
        var systems = new WcwtAutoUploadSystems();
        systems.add(new SameNameSystem("crafting"));
        systems.add(new SameNameSystem("crafting"));
        assertAmbiguousWithoutSideEffects(systems);
    }

    private static void assertAmbiguousWithoutSideEffects(WcwtAutoUploadSystems systems) {
        AtomicInteger insertsOrDuplicateReturns = new AtomicInteger();
        AtomicInteger namedProviderUploads = new AtomicInteger();
        var result = systems.uploadIfUnique(() -> {
            insertsOrDuplicateReturns.incrementAndGet();
            return UPLOADED;
        }, WcwtAutoUploadSystemsTest::unexpectedUpload, FAILURE, AMBIGUOUS);
        // This is the same result contract consumed by the menu before the named-provider branch.
        if (result.allowsProviderFallback()) {
            namedProviderUploads.incrementAndGet();
        }
        assertEquals(AMBIGUOUS, result);
        assertFalse(result.allowsProviderFallback());
        assertEquals(0, insertsOrDuplicateReturns.get());
        assertEquals(0, namedProviderUploads.get());
    }

    private static <T> T unexpectedUpload() {
        throw new AssertionError("Unexpected upload callback");
    }

    private record PatternCore(Object backing) {
    }

    private record SameNameSystem(String name) {
    }
}
