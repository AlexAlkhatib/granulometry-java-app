package fr.granulometrie.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Chaine d'analyse complete : lecture de l'image, pretraitement, seuillage,
 * etiquetage, mesures et filtrage.
 *
 * <p>Cette classe ne depend pas de JavaFX : elle est utilisable en batch, en test
 * unitaire ou depuis une autre interface.</p>
 */
public final class ImageAnalyzer {

    /** Aire minimale (en pixels) en dessous de laquelle une composante est du bruit. */
    private static final int NOISE_FLOOR_PIXELS = 4;

    private ImageAnalyzer() {
    }

    /**
     * Lit une image depuis le disque et la convertit en RGB (format de travail).
     *
     * @param file fichier image
     * @return l'image chargee
     * @throws IOException si le fichier n'est pas une image lisible
     */
    public static BufferedImage load(File file) throws IOException {
        BufferedImage source = ImageIO.read(file);
        if (source == null) {
            throw new IOException("Format d'image non reconnu : " + file.getName());
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(source, 0, 0, null);
        return rgb;
    }

    /** @return les niveaux de gris (luminance Rec.601) sous forme de tableau plat. */
    public static int[] toGray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = image.getRGB(0, 0, width, height, null, 0, width);
        int[] gray = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int value = rgb[i];
            int r = (value >> 16) & 0xFF;
            int g = (value >> 8) & 0xFF;
            int b = value & 0xFF;
            gray[i] = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
        }
        return gray;
    }

    /**
     * Analyse une image deja chargee.
     *
     * @param image      image de travail
     * @param sourceFile fichier d'origine (peut etre null)
     * @param parameters parametres de traitement
     * @return le resultat complet de l'analyse
     */
    public static AnalysisResult analyze(BufferedImage image, File sourceFile, AnalysisParameters parameters) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] gray = toGray(image);
        int[] processed = Segmentation.boxBlur(gray, width, height, parameters.getBlurRadius());

        int autoThreshold = Segmentation.otsu(Segmentation.histogram(processed));
        int threshold = parameters.isManualThreshold() ? parameters.getThreshold() : autoThreshold;

        long dark = 0;
        for (int value : processed) {
            if (value <= threshold) {
                dark++;
            }
        }
        long light = (long) width * height - dark;
        boolean foregroundDark = dark <= light;
        if (parameters.isInvertPolarity()) {
            foregroundDark = !foregroundDark;
        }

        boolean[] mask = Segmentation.binarize(processed, threshold, foregroundDark);
        double minDiameterPixels = parameters.minDiameterPixels();
        int minArea = (int) Math.max(NOISE_FLOOR_PIXELS,
                Math.round(Math.PI * minDiameterPixels * minDiameterPixels / 4.0));

        List<Particle> all = Segmentation.label(mask, width, height, minArea);
        List<Particle> kept = filter(all, parameters);
        return new AnalysisResult(sourceFile, image, parameters, threshold, autoThreshold,
                foregroundDark, all, kept);
    }

    /**
     * Applique les filtres morphologiques aux particules detectees.
     *
     * @param particles  particules detectees
     * @param parameters parametres portant les bornes de diametre et de circularite
     * @return les particules retenues
     */
    public static List<Particle> filter(List<Particle> particles, AnalysisParameters parameters) {
        List<Particle> kept = new ArrayList<>(particles.size());
        double minPixels = parameters.minDiameterPixels();
        double maxPixels = parameters.maxDiameterPixels();
        for (Particle particle : particles) {
            if (parameters.isExcludeBorder() && particle.touchesBorder()) {
                continue;
            }
            if (minPixels > 0 && particle.equivalentDiameter() < minPixels) {
                continue;
            }
            if (maxPixels > 0 && particle.equivalentDiameter() > maxPixels) {
                continue;
            }
            if (particle.circularity() < parameters.getMinCircularity()) {
                continue;
            }
            kept.add(particle);
        }
        return kept;
    }
}
