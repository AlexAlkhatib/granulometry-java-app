package fr.granulometrie.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Construction des classes granulometriques (histogrammes et courbe cumulative). */
public final class SizeClassifier {

    /**
     * Une classe de taille.
     *
     * @param lowerBound       borne inferieure (exclue)
     * @param upperBound       borne superieure (incluse)
     * @param count            effectif de la classe
     * @param frequencyPercent frequence relative en pourcentage
     */
    public record SizeClass(double lowerBound, double upperBound, int count, double frequencyPercent) {
        /** @return un libelle "min – max" formate. */
        public String label() {
            return String.format(Locale.FRANCE, "%.2f – %.2f", lowerBound, upperBound);
        }

        /** @return le centre de la classe. */
        public double midPoint() {
            return (lowerBound + upperBound) / 2.0;
        }
    }

    private SizeClassifier() {
    }

    /**
     * Histogramme des diametres equivalents.
     *
     * @param particles particules retenues
     * @param binWidth  largeur de classe dans l'unite courante
     * @param pixelSize taille du pixel en micrometres
     * @return les classes non vides, ordonnees
     */
    public static List<SizeClass> histogramOfDiameters(List<Particle> particles, double binWidth, double pixelSize) {
        List<Double> diameters = new ArrayList<>(particles.size());
        for (Particle particle : particles) {
            diameters.add(particle.equivalentDiameter(pixelSize));
        }
        return histogram(diameters, binWidth);
    }

    /**
     * Histogramme de la part de surface occupee par chaque particule.
     *
     * @param particles   particules retenues
     * @param binWidth    largeur de classe en pourcentage de l'image
     * @param imagePixels nombre total de pixels de l'image
     * @return les classes non vides, ordonnees
     */
    public static List<SizeClass> histogramOfSurfaceShare(List<Particle> particles, double binWidth, long imagePixels) {
        List<Double> shares = new ArrayList<>(particles.size());
        for (Particle particle : particles) {
            shares.add(imagePixels > 0 ? particle.area() * 100.0 / imagePixels : 0);
        }
        return histogram(shares, binWidth);
    }

    /**
     * Construit un histogramme generique.
     *
     * @param values   valeurs a classer
     * @param binWidth largeur de classe (> 0)
     * @return les classes non vides
     */
    public static List<SizeClass> histogram(List<Double> values, double binWidth) {
        List<SizeClass> classes = new ArrayList<>();
        if (values.isEmpty()) {
            return classes;
        }
        double width = binWidth > 0 ? binWidth : 1.0;
        double max = StatisticsUtils.max(values);
        int binCount = (int) Math.floor(max / width) + 1;
        binCount = Math.max(1, Math.min(binCount, 200));
        int[] counts = new int[binCount];
        for (double value : values) {
            int index = (int) Math.floor(value / width);
            index = Math.max(0, Math.min(binCount - 1, index));
            counts[index]++;
        }
        int total = values.size();
        for (int i = 0; i < binCount; i++) {
            if (counts[i] == 0) {
                continue;
            }
            classes.add(new SizeClass(i * width, (i + 1) * width, counts[i],
                    counts[i] * 100.0 / total));
        }
        return classes;
    }

    /**
     * Courbe cumulative des passants (pourcentage de particules de diametre
     * inferieur ou egal a la borne superieure de la classe).
     *
     * @param classes classes triees par ordre croissant
     * @return tableau de paires {borne superieure, pourcentage cumule}
     */
    public static List<double[]> cumulativeCurve(List<SizeClass> classes) {
        List<double[]> points = new ArrayList<>(classes.size());
        double cumulative = 0;
        for (SizeClass sizeClass : classes) {
            cumulative += sizeClass.frequencyPercent();
            points.add(new double[]{sizeClass.upperBound(), Math.min(100, cumulative)});
        }
        return points;
    }

    /** @return la classe modale (effectif maximal) ou null si l'histogramme est vide. */
    public static SizeClass mode(List<SizeClass> classes) {
        SizeClass best = null;
        for (SizeClass sizeClass : classes) {
            if (best == null || sizeClass.count() > best.count()) {
                best = sizeClass;
            }
        }
        return best;
    }
}
