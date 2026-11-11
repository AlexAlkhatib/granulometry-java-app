package fr.granulometrie.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Synthese statistique d'une population de particules.
 *
 * @param count             nombre de particules retenues
 * @param minDiameter       diametre equivalent minimal
 * @param maxDiameter       diametre equivalent maximal
 * @param meanDiameter      diametre equivalent moyen
 * @param medianDiameter    diametre equivalent median (D50)
 * @param standardDeviation ecart type des diametres equivalents
 * @param d10               diametre sous lequel se trouvent 10 % des particules
 * @param d50               diametre sous lequel se trouvent 50 % des particules
 * @param d90               diametre sous lequel se trouvent 90 % des particules
 * @param meanCircularity   circularite moyenne (0..1)
 * @param meanAspectRatio   rapport d'aspect moyen (0..1)
 * @param meanArea          aire moyenne dans l'unite de surface courante
 * @param totalArea         aire cumulee dans l'unite de surface courante
 * @param areaCoverage      pourcentage de l'image couvert par les particules
 */
public record SizeStatistics(
        int count,
        double minDiameter,
        double maxDiameter,
        double meanDiameter,
        double medianDiameter,
        double standardDeviation,
        double d10,
        double d50,
        double d90,
        double meanCircularity,
        double meanAspectRatio,
        double meanArea,
        double totalArea,
        double areaCoverage) {

    /** Statistiques vides (aucune particule retenue). */
    public static final SizeStatistics EMPTY =
            new SizeStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    /**
     * Calcule la synthese d'une population.
     *
     * @param particles   particules retenues
     * @param pixelSize   taille du pixel en micrometres
     * @param imagePixels nombre total de pixels de l'image (pour le taux de couverture)
     * @return la synthese statistique
     */
    public static SizeStatistics from(List<Particle> particles, double pixelSize, long imagePixels) {
        if (particles == null || particles.isEmpty()) {
            return EMPTY;
        }
        List<Double> diameters = new ArrayList<>(particles.size());
        List<Double> circularities = new ArrayList<>(particles.size());
        List<Double> aspectRatios = new ArrayList<>(particles.size());
        double totalArea = 0;
        for (Particle p : particles) {
            diameters.add(p.equivalentDiameter(pixelSize));
            circularities.add(p.circularity());
            aspectRatios.add(p.aspectRatio());
            totalArea += p.area(pixelSize);
        }
        double totalPixels = 0;
        for (Particle p : particles) {
            totalPixels += p.area();
        }
        double coverage = imagePixels > 0 ? totalPixels * 100.0 / imagePixels : 0;
        return new SizeStatistics(
                particles.size(),
                StatisticsUtils.min(diameters),
                StatisticsUtils.max(diameters),
                StatisticsUtils.mean(diameters),
                StatisticsUtils.median(diameters),
                StatisticsUtils.standardDeviation(diameters),
                StatisticsUtils.percentile(diameters, 10),
                StatisticsUtils.percentile(diameters, 50),
                StatisticsUtils.percentile(diameters, 90),
                StatisticsUtils.mean(circularities),
                StatisticsUtils.mean(aspectRatios),
                totalArea / particles.size(),
                totalArea,
                coverage);
    }
}
