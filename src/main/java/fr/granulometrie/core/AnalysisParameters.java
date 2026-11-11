package fr.granulometrie.core;

/**
 * Parametres de traitement d'une image : segmentation, filtres morphologiques et
 * calibration spatiale.
 *
 * <p>Les diametres minimum et maximum ainsi que la calibration sont exprimes en
 * micrometres. Lorsque la calibration vaut 1.0, l'unite affichee est le pixel
 * (analyse non calibree).</p>
 */
public final class AnalysisParameters {

    /** Seuil calcule automatiquement (Otsu) ou saisi par l'operateur. */
    private boolean manualThreshold;
    /** Seuil manuel [0..255], utilise uniquement si {@link #manualThreshold}. */
    private int threshold = 128;
    /** Inverse la polarite detectee automatiquement (particules claires sur fond sombre). */
    private boolean invertPolarity;
    /** Rayon du filtre moyenneur applique avant le seuillage (0 = aucun). */
    private int blurRadius;
    /** Diametre equivalent minimum conserve (unites utilisateur). 0 = pas de limite. */
    private double minDiameter;
    /** Diametre equivalent maximum conserve (unites utilisateur). 0 = pas de limite. */
    private double maxDiameter;
    /** Circularite minimale conservee (0 = pas de filtre, 1 = disque parfait). */
    private double minCircularity;
    /** Ignore les particules touchant le bord de l'image (objets tronques). */
    private boolean excludeBorder;
    /** Taille d'un pixel en micrometres (calibration). 1.0 = analyse en pixels. */
    private double pixelSizeMicrons = 1.0;

    public AnalysisParameters() {
    }

    public AnalysisParameters copy() {
        AnalysisParameters c = new AnalysisParameters();
        c.manualThreshold = manualThreshold;
        c.threshold = threshold;
        c.invertPolarity = invertPolarity;
        c.blurRadius = blurRadius;
        c.minDiameter = minDiameter;
        c.maxDiameter = maxDiameter;
        c.minCircularity = minCircularity;
        c.excludeBorder = excludeBorder;
        c.pixelSizeMicrons = pixelSizeMicrons;
        return c;
    }

    /** @return true si une calibration physique a ete saisie. */
    public boolean isCalibrated() {
        return pixelSizeMicrons > 0 && Math.abs(pixelSizeMicrons - 1.0) > 1e-9;
    }

    /** @return l'unite d'affichage des longueurs ("px" ou "µm"). */
    public String unit() {
        return isCalibrated() ? "µm" : "px";
    }

    /** @return le diametre minimum exprime en pixels. */
    public double minDiameterPixels() {
        return pixelSizeMicrons > 0 ? minDiameter / pixelSizeMicrons : minDiameter;
    }

    /** @return le diametre maximum exprime en pixels (0 = pas de limite). */
    public double maxDiameterPixels() {
        return pixelSizeMicrons > 0 ? maxDiameter / pixelSizeMicrons : maxDiameter;
    }

    public boolean isManualThreshold() {
        return manualThreshold;
    }

    public void setManualThreshold(boolean manualThreshold) {
        this.manualThreshold = manualThreshold;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = Math.max(0, Math.min(255, threshold));
    }

    public boolean isInvertPolarity() {
        return invertPolarity;
    }

    public void setInvertPolarity(boolean invertPolarity) {
        this.invertPolarity = invertPolarity;
    }

    public int getBlurRadius() {
        return blurRadius;
    }

    public void setBlurRadius(int blurRadius) {
        this.blurRadius = Math.max(0, Math.min(5, blurRadius));
    }

    public double getMinDiameter() {
        return minDiameter;
    }

    public void setMinDiameter(double minDiameter) {
        this.minDiameter = Math.max(0, minDiameter);
    }

    public double getMaxDiameter() {
        return maxDiameter;
    }

    public void setMaxDiameter(double maxDiameter) {
        this.maxDiameter = Math.max(0, maxDiameter);
    }

    public double getMinCircularity() {
        return minCircularity;
    }

    public void setMinCircularity(double minCircularity) {
        this.minCircularity = Math.max(0, Math.min(2, minCircularity));
    }

    public boolean isExcludeBorder() {
        return excludeBorder;
    }

    public void setExcludeBorder(boolean excludeBorder) {
        this.excludeBorder = excludeBorder;
    }

    public double getPixelSizeMicrons() {
        return pixelSizeMicrons;
    }

    public void setPixelSizeMicrons(double pixelSizeMicrons) {
        this.pixelSizeMicrons = pixelSizeMicrons > 0 ? pixelSizeMicrons : 1.0;
    }

    @Override
    public String toString() {
        return "seuil=" + (manualThreshold ? threshold : "auto(Otsu)")
                + ", inversion=" + invertPolarity
                + ", flou=" + blurRadius
                + ", diametre=[" + minDiameter + " ; " + maxDiameter + "] " + unit()
                + ", circularite>=" + minCircularity
                + ", bords=" + (excludeBorder ? "exclus" : "inclus")
                + ", calibration=" + pixelSizeMicrons + " µm/px";
    }
}
