package fr.granulometrie.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;

/** Resultat complet d'une analyse : image, parametres, particules et synthese. */
public final class AnalysisResult {

    private final File sourceFile;
    private final BufferedImage image;
    private final AnalysisParameters parameters;
    private final int thresholdUsed;
    private final int autoThreshold;
    private final boolean foregroundDark;
    private final List<Particle> allParticles;
    private final List<Particle> particles;
    private final SizeStatistics statistics;
    private final int width;
    private final int height;

    /**
     * @param sourceFile     fichier d'origine (peut etre null)
     * @param image          image de travail (peut etre null pour un resultat restaure)
     * @param parameters     parametres d'analyse
     * @param thresholdUsed  seuil applique
     * @param autoThreshold  seuil propose par Otsu
     * @param foregroundDark polarite retenue
     * @param allParticles   particules detectees avant filtres
     * @param particles      particules retenues
     */
    public AnalysisResult(File sourceFile, BufferedImage image, AnalysisParameters parameters,
                          int thresholdUsed, int autoThreshold, boolean foregroundDark,
                          List<Particle> allParticles, List<Particle> particles) {
        this(sourceFile, image, parameters, thresholdUsed, autoThreshold, foregroundDark,
                allParticles, particles,
                image != null ? image.getWidth() : 0,
                image != null ? image.getHeight() : 0);
    }

    /**
     * Variante permettant de fournir explicitement les dimensions de l'image
     * (necessaire lors d'une restauration depuis l'historique).
     */
    public AnalysisResult(File sourceFile, BufferedImage image, AnalysisParameters parameters,
                          int thresholdUsed, int autoThreshold, boolean foregroundDark,
                          List<Particle> allParticles, List<Particle> particles,
                          int width, int height) {
        this.sourceFile = sourceFile;
        this.image = image;
        this.parameters = parameters;
        this.thresholdUsed = thresholdUsed;
        this.autoThreshold = autoThreshold;
        this.foregroundDark = foregroundDark;
        this.allParticles = Collections.unmodifiableList(allParticles);
        this.particles = Collections.unmodifiableList(particles);
        this.width = width;
        this.height = height;
        this.statistics = SizeStatistics.from(particles, parameters.getPixelSizeMicrons(),
                (long) width * height);
    }

    /**
     * Reconstruit un resultat depuis les donnees stockees (historique).
     *
     * @param sourceFile  fichier d'origine (peut etre null)
     * @param image       image d'origine restauree depuis la base
     * @param parameters  parametres utilises lors de l'analyse
     * @param threshold   seuil applique
     * @param particles   particules enregistrees
     * @param width       largeur de l'image d'origine
     * @param height      hauteur de l'image d'origine
     * @return le resultat pret a etre affiche
     */
    public static AnalysisResult fromStored(File sourceFile, BufferedImage image,
                                            AnalysisParameters parameters, int threshold,
                                            List<Particle> particles, int width, int height) {
        return new AnalysisResult(sourceFile, image, parameters, threshold, threshold,
                true, particles, particles, width, height);
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getImageName() {
        return sourceFile != null ? sourceFile.getName() : "image";
    }

    public BufferedImage getImage() {
        return image;
    }

    public AnalysisParameters getParameters() {
        return parameters;
    }

    /** @return le seuil effectivement applique. */
    public int getThresholdUsed() {
        return thresholdUsed;
    }

    /** @return le seuil propose automatiquement par la methode d'Otsu. */
    public int getAutoThreshold() {
        return autoThreshold;
    }

    /** @return true si les particules sont plus sombres que le fond. */
    public boolean isForegroundDark() {
        return foregroundDark;
    }

    /** @return toutes les particules detectees (avant filtres). */
    public List<Particle> getAllParticles() {
        return allParticles;
    }

    /** @return les particules retenues apres filtres. */
    public List<Particle> getParticles() {
        return particles;
    }

    public SizeStatistics getStatistics() {
        return statistics;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getPixelSize() {
        return parameters.getPixelSizeMicrons();
    }

    public String getUnit() {
        return parameters.unit();
    }

    /** @return l'unite de surface ("µm²" ou "px²"). */
    public String getAreaUnit() {
        return parameters.isCalibrated() ? "µm²" : "px²";
    }
}
