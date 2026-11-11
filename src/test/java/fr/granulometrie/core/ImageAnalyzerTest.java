package fr.granulometrie.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ImageAnalyzerTest {

    @Test
    void detectsThreeParticlesWithExpectedSizes() {
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, new AnalysisParameters());

        assertEquals(3, result.getParticles().size());
        double[] diameters = result.getParticles().stream()
                .mapToDouble(Particle::equivalentDiameter)
                .sorted()
                .toArray();
        assertEquals(20.0, diameters[0], 1.0);
        assertEquals(40.0, diameters[1], 1.5);
        assertEquals(80.0, diameters[2], 2.5);
    }

    @Test
    void otsuThresholdSeparatesBackgroundFromParticles() {
        BufferedImage image = TestImages.circles();
        int[] gray = ImageAnalyzer.toGray(image);
        int threshold = Segmentation.otsu(Segmentation.histogram(gray));

        assertTrue(threshold > 5 && threshold < 250, "seuil inattendu : " + threshold);

        AnalysisResult result = ImageAnalyzer.analyze(image, null, new AnalysisParameters());
        assertEquals(threshold, result.getAutoThreshold());
        assertTrue(result.isForegroundDark(), "les particules noires doivent être le premier plan");
    }

    @Test
    void invertPolarityDetectsBackgroundInsteadOfParticles() {
        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setInvertPolarity(true);
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, parameters);

        // Le fond (clair) devient le premier plan : une seule grande composante,
        // en contact avec les bords de l'image.
        assertEquals(1, result.getParticles().size());
        assertTrue(result.getParticles().get(0).touchesBorder());
    }

    @Test
    void minDiameterFilterRemovesSmallParticles() {
        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setMinDiameter(30);

        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, parameters);
        assertEquals(2, result.getParticles().size());
    }

    @Test
    void maxDiameterFilterRemovesLargeParticles() {
        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setMaxDiameter(50);

        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, parameters);
        assertEquals(2, result.getParticles().size());
    }

    @Test
    void excludeBorderIgnoresTruncatedParticles() {
        BufferedImage image = TestImages.circles();
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillOval(-15, 120, 60, 60);
        g.dispose();

        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setExcludeBorder(true);
        AnalysisResult result = ImageAnalyzer.analyze(image, null, parameters);

        assertEquals(3, result.getParticles().size());
        assertTrue(result.getAllParticles().stream().anyMatch(Particle::touchesBorder));
    }

    @Test
    void circularityOfPixelDiscStaysCloseToOne() {
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, new AnalysisParameters());
        assertTrue(result.getStatistics().meanCircularity() > 0.7,
                "circularité moyenne trop faible : " + result.getStatistics().meanCircularity());
        assertTrue(result.getStatistics().meanCircularity() <= 1.2);
    }

    @Test
    void calibrationConvertsDiametersToMicrons() {
        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setPixelSizeMicrons(2.5);

        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, parameters);

        assertEquals("µm", result.getUnit());
        assertEquals(200.0, result.getStatistics().maxDiameter(), 8.0);
        assertEquals(3, result.getStatistics().count());
    }

    @Test
    void manualThresholdIsUsedWhenEnabled() {
        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setManualThreshold(true);
        parameters.setThreshold(10);

        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, parameters);
        assertEquals(10, result.getThresholdUsed());
        // Les pixels noirs (0) restent sous le seuil : les trois disques sont trouvés.
        assertEquals(3, result.getParticles().size());
    }

    @Test
    void statisticsAreConsistentWithParticles() {
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, new AnalysisParameters());
        SizeStatistics statistics = result.getStatistics();

        assertEquals(3, statistics.count());
        assertEquals(statistics.d50(), statistics.medianDiameter(), 1e-9);
        double[] sorted = result.getParticles().stream()
                .mapToDouble(Particle::equivalentDiameter)
                .sorted()
                .toArray();
        assertEquals(Arrays.stream(sorted).min().orElse(0), statistics.minDiameter(), 1e-6);
        assertEquals(Arrays.stream(sorted).max().orElse(0), statistics.maxDiameter(), 1e-6);
        assertTrue(statistics.areaCoverage() > 0 && statistics.areaCoverage() < 100);
    }
}
