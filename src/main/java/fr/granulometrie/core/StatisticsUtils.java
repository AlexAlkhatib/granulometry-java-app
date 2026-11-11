package fr.granulometrie.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fonctions statistiques descriptives utilisees par l'analyse granulometrique. */
public final class StatisticsUtils {

    private StatisticsUtils() {
    }

    /** @return la moyenne, 0 si la liste est vide. */
    public static double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    /** @return l'ecart type (estimate non biaise), 0 si moins de deux valeurs. */
    public static double standardDeviation(List<Double> values) {
        if (values.size() < 2) {
            return 0;
        }
        double m = mean(values);
        double sum = 0;
        for (double v : values) {
            sum += (v - m) * (v - m);
        }
        return Math.sqrt(sum / (values.size() - 1));
    }

    /** @return la mediane (50e percentile). */
    public static double median(List<Double> values) {
        return percentile(values, 50);
    }

    /**
     * Percentile par interpolation lineaire sur une liste triee par ordre croissant.
     *
     * @param values     valeurs (non triees)
     * @param percentile pourcentage demande [0..100]
     * @return la valeur du percentile, 0 si la liste est vide
     */
    public static double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double p = Math.max(0, Math.min(100, percentile));
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        double rank = p / 100.0 * (sorted.size() - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        double fraction = rank - lower;
        return sorted.get(lower) + fraction * (sorted.get(upper) - sorted.get(lower));
    }

    /** @return le minimum, 0 si la liste est vide. */
    public static double min(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
    }

    /** @return le maximum, 0 si la liste est vide. */
    public static double max(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }
}
