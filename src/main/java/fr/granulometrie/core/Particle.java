package fr.granulometrie.core;

/**
 * Une particule detectee, geometrie exprimee en pixels.
 *
 * <p>Les axes majeur et mineur proviennent des moments centraux d'ordre 2 : ils
 * correspondent aux axes de l'ellipse d'inertie equivalente (convention de
 * l'analyseur de particules d'ImageJ : grand axe = 4 * sqrt(lambda1)).</p>
 *
 * @param id                numero d'ordre de detection (1..n)
 * @param area              aire en pixels carres
 * @param perimeter         perimetre estime par comptage des cotes exposees (pixels)
 * @param centroidX         abscisse du barycentre (pixels)
 * @param centroidY         ordonnee du barycentre (pixels)
 * @param bboxX             abscisse du coin superieur gauche de la boite englobante
 * @param bboxY             ordonnee du coin superieur gauche de la boite englobante
 * @param bboxWidth         largeur de la boite englobante (pixels)
 * @param bboxHeight        hauteur de la boite englobante (pixels)
 * @param majorAxis         longueur du grand axe de l'ellipse equivalente (pixels)
 * @param minorAxis         longueur du petit axe de l'ellipse equivalente (pixels)
 * @param orientationDegrees orientation du grand axe par rapport a l'horizontale (degres)
 * @param circularity       circularite 4 * pi * A / P^2 (1 = disque parfait)
 * @param equivalentDiameter diametre du disque de meme aire 2 * sqrt(A / pi) (pixels)
 * @param touchesBorder     true si la particule est en contact avec le bord de l'image
 */
public record Particle(
        int id,
        double area,
        double perimeter,
        double centroidX,
        double centroidY,
        int bboxX,
        int bboxY,
        int bboxWidth,
        int bboxHeight,
        double majorAxis,
        double minorAxis,
        double orientationDegrees,
        double circularity,
        double equivalentDiameter,
        boolean touchesBorder) {

    /** @return le rapport d'aspect (petit axe / grand axe), entre 0 et 1. */
    public double aspectRatio() {
        return majorAxis <= 0 ? 0 : Math.min(1.0, minorAxis / majorAxis);
    }

    /** @return le diametre equivalent converti dans l'unite demandee. */
    public double equivalentDiameter(double pixelSize) {
        return equivalentDiameter * pixelSize;
    }

    /** @return l'aire convertie en µm carres (ou px carres si non calibree). */
    public double area(double pixelSize) {
        return area * pixelSize * pixelSize;
    }
}
