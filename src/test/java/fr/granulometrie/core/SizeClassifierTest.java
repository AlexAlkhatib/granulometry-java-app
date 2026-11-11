package fr.granulometrie.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.granulometrie.core.SizeClassifier.SizeClass;

class SizeClassifierTest {

    @Test
    void histogramGroupsValuesByClassWidth() {
        List<SizeClass> classes = SizeClassifier.histogram(List.of(1.0, 2.5, 3.0, 7.0), 2.0);

        assertEquals(3, classes.size());
        assertEquals(1, classes.get(0).count());
        assertEquals(0.0, classes.get(0).lowerBound(), 1e-9);
        assertEquals(2.0, classes.get(0).upperBound(), 1e-9);
        assertEquals(2, classes.get(1).count());
        assertEquals(1, classes.get(2).count());
        assertEquals(6.0, classes.get(2).lowerBound(), 1e-9);
    }

    @Test
    void frequenciesSumToHundred() {
        List<SizeClass> classes = SizeClassifier.histogram(List.of(1.0, 2.0, 3.0, 4.0, 5.0), 2.0);
        double total = classes.stream().mapToDouble(SizeClass::frequencyPercent).sum();
        assertEquals(100.0, total, 1e-9);
    }

    @Test
    void cumulativeCurveEndsAtHundredPercent() {
        List<SizeClass> classes = SizeClassifier.histogram(List.of(1.0, 2.0, 3.0, 8.0), 2.0);
        List<double[]> curve = SizeClassifier.cumulativeCurve(classes);

        assertEquals(classes.size(), curve.size());
        assertEquals(100.0, curve.get(curve.size() - 1)[1], 1e-9);
    }

    @Test
    void diametersOfParticlesUseRequestedUnit() {
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, new AnalysisParameters());

        List<SizeClass> pixels = SizeClassifier.histogramOfDiameters(result.getParticles(), 20, 1.0);
        List<SizeClass> microns = SizeClassifier.histogramOfDiameters(result.getParticles(), 20, 2.0);

        assertTrue(microns.get(microns.size() - 1).upperBound() > pixels.get(pixels.size() - 1).upperBound());
    }

    @Test
    void emptyHistogramStaysEmpty() {
        assertTrue(SizeClassifier.histogram(List.of(), 5).isEmpty());
        assertTrue(SizeClassifier.histogramOfDiameters(List.of(), 5, 1.0).isEmpty());
    }
}
