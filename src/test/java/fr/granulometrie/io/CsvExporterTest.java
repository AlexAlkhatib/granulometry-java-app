package fr.granulometrie.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.granulometrie.core.AnalysisParameters;
import fr.granulometrie.core.AnalysisResult;
import fr.granulometrie.core.ImageAnalyzer;
import fr.granulometrie.core.TestImages;

class CsvExporterTest {

    @Test
    void writesMetadataHeaderAndOneRowPerParticle(@TempDir Path directory) throws IOException {
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, new AnalysisParameters());
        Path file = directory.resolve("resultats.csv");

        CsvExporter.export(file, result);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(line -> line.replace("\uFEFF", ""))
                .collect(Collectors.toList());
        assertTrue(lines.get(0).contains("Granulometrie Studio"));
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("# Image;")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Diametre equivalent")));

        List<String> dataLines = lines.stream()
                .filter(line -> !line.startsWith("#") && !line.startsWith("N°"))
                .collect(Collectors.toList());
        assertEquals(result.getParticles().size(), dataLines.size());
        assertTrue(dataLines.get(0).startsWith("1;"));
    }

    @Test
    void exportsCalibratedUnits(@TempDir Path directory) throws IOException {
        AnalysisParameters parameters = new AnalysisParameters();
        parameters.setPixelSizeMicrons(0.5);
        AnalysisResult result = ImageAnalyzer.analyze(TestImages.circles(), null, parameters);
        Path file = directory.resolve("calibre.csv");

        CsvExporter.export(file, result);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertTrue(lines.stream().anyMatch(line -> line.contains("um/px")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Diametre equivalent (µm)")));
    }
}
