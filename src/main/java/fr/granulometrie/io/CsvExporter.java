package fr.granulometrie.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import fr.granulometrie.core.AnalysisResult;
import fr.granulometrie.core.Particle;
import fr.granulometrie.core.SizeStatistics;

/**
 * Export des resultats d'analyse au format CSV (separateur point-virgule,
 * encodage UTF-8 avec BOM pour une ouverture directe dans Excel).
 *
 * <p>Le fichier commence par un bloc de metadonnees commente (#) puis une ligne
 * d'entete et une ligne par particule.</p>
 */
public final class CsvExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private CsvExporter() {
    }

    /**
     * Ecrit le fichier CSV.
     *
     * @param target fichier de destination
     * @param result resultat a exporter
     * @throws IOException en cas d'erreur d'ecriture
     */
    public static void export(Path target, AnalysisResult result) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            String unit = result.getUnit();
            String areaUnit = result.getAreaUnit();
            SizeStatistics stats = result.getStatistics();

            writer.write("# Granulometrie Studio - export du " + LocalDateTime.now().format(DATE_FORMAT) + "\n");
            writer.write("# Image;" + escape(result.getImageName()) + "\n");
            writer.write("# Dimensions;" + result.getWidth() + " x " + result.getHeight() + " px\n");
            writer.write("# Calibration;" + format(result.getPixelSize()) + " um/px\n");
            writer.write("# Seuil applique;" + result.getThresholdUsed()
                    + " (Otsu automatique : " + result.getAutoThreshold() + ")\n");
            writer.write("# Parametres;" + escape(result.getParameters().toString()) + "\n");
            writer.write("# Particules retenues;" + stats.count()
                    + " / detectees : " + result.getAllParticles().size() + "\n");
            writer.write("# Diametre moyen (" + unit + ");" + format(stats.meanDiameter()) + "\n");
            writer.write("# D10 / D50 / D90 (" + unit + ");" + format(stats.d10()) + " ; "
                    + format(stats.d50()) + " ; " + format(stats.d90()) + "\n");
            writer.write("#\n");

            writer.write("N°;Diametre equivalent (" + unit + ");Aire (" + areaUnit
                    + ");Perimetre (px);Grand axe (" + unit + ");Petit axe (" + unit
                    + ");Rapport aspect;Circularite;X (" + unit + ");Y (" + unit
                    + ");Largeur bbox (px);Hauteur bbox (px);Touche le bord\n");

            double scale = result.getPixelSize();
            for (Particle particle : result.getParticles()) {
                writer.write(particle.id() + ";");
                writer.write(format(particle.equivalentDiameter(scale)) + ";");
                writer.write(format(particle.area(scale)) + ";");
                writer.write(format(particle.perimeter()) + ";");
                writer.write(format(particle.majorAxis() * scale) + ";");
                writer.write(format(particle.minorAxis() * scale) + ";");
                writer.write(format(particle.aspectRatio()) + ";");
                writer.write(format(particle.circularity()) + ";");
                writer.write(format(particle.centroidX() * scale) + ";");
                writer.write(format(particle.centroidY() * scale) + ";");
                writer.write(particle.bboxWidth() + ";");
                writer.write(particle.bboxHeight() + ";");
                writer.write(particle.touchesBorder() ? "oui" : "non");
                writer.write("\n");
            }
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }
}
