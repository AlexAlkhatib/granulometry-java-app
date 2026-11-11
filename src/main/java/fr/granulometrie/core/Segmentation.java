package fr.granulometrie.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentation et etiquetage des particules.
 *
 * <p>Le pipeline est entierement implemente en Java standard (aucune dependance
 * externe) :</p>
 * <ol>
 *   <li>seuillage binaire (methode d'Otsu ou seuil fixe) ;</li>
 *   <li>etiquetage en composantes connexes 8-connexes par parcours en largeur ;</li>
 *   <li>mesure de chaque composante : aire, perimetre, barycentre, boite
 *       englobante, moments centraux d'ordre 2 (axes de l'ellipse equivalente).</li>
 * </ol>
 */
public final class Segmentation {

    private Segmentation() {
    }

    /** @return l'histogramme 256 classes des niveaux de gris. */
    public static int[] histogram(int[] gray) {
        int[] hist = new int[256];
        for (int value : gray) {
            hist[Math.max(0, Math.min(255, value))]++;
        }
        return hist;
    }

    /**
     * Calcule le seuil automatique par la methode d'Otsu (maximisation de la
     * variance inter-classes).
     *
     * <p>Sur une image franchement bimodale, plusieurs seuils produisent la meme
     * partition : on retourne le milieu du plateau de variance maximale, ce qui
     * evite un seuil artificiellement nul et reste robuste au bruit.</p>
     *
     * @param hist histogramme 256 classes
     * @return le seuil optimal [0..255]
     */
    public static int otsu(int[] hist) {
        long total = 0;
        double sum = 0;
        for (int i = 0; i < 256; i++) {
            total += hist[i];
            sum += (double) i * hist[i];
        }
        if (total == 0) {
            return 128;
        }
        double sumBackground = 0;
        long weightBackground = 0;
        double bestVariance = -1;
        int firstBest = 0;
        int lastBest = 0;
        for (int t = 0; t < 256; t++) {
            weightBackground += hist[t];
            if (weightBackground == 0) {
                continue;
            }
            long weightForeground = total - weightBackground;
            if (weightForeground == 0) {
                break;
            }
            sumBackground += (double) t * hist[t];
            double meanBackground = sumBackground / weightBackground;
            double meanForeground = (sum - sumBackground) / weightForeground;
            double between = (double) weightBackground * weightForeground
                    * (meanBackground - meanForeground) * (meanBackground - meanForeground);
            double tolerance = Math.max(1.0, bestVariance) * 1e-9;
            if (between > bestVariance + tolerance) {
                bestVariance = between;
                firstBest = t;
                lastBest = t;
            } else if (Math.abs(between - bestVariance) <= tolerance) {
                lastBest = t;
            }
        }
        return (firstBest + lastBest) / 2;
    }

    /**
     * Filtre moyenneur separable a fenetre glissante (bords traites par
     * replication).
     *
     * @param gray   image en niveaux de gris (largeur * hauteur)
     * @param width  largeur
     * @param height hauteur
     * @param radius rayon du noyau (0 = copie inchangee)
     * @return un nouveau tableau filtre
     */
    public static int[] boxBlur(int[] gray, int width, int height, int radius) {
        if (radius <= 0) {
            return gray.clone();
        }
        int[] horizontal = new int[gray.length];
        int window = 2 * radius + 1;
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int sum = 0;
                for (int k = -radius; k <= radius; k++) {
                    int xx = Math.max(0, Math.min(width - 1, x + k));
                    sum += gray[rowOffset + xx];
                }
                horizontal[rowOffset + x] = sum / window;
            }
        }
        int[] result = new int[gray.length];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int sum = 0;
                for (int k = -radius; k <= radius; k++) {
                    int yy = Math.max(0, Math.min(height - 1, y + k));
                    sum += horizontal[yy * width + x];
                }
                result[y * width + x] = sum / window;
            }
        }
        return result;
    }

    /**
     * Etiquette les composantes connexes de la forme binaire et calcule leurs
     * mesures geometriques.
     *
     * @param mask    forme binaire (true = particule)
     * @param width   largeur
     * @param height  hauteur
     * @param minArea aire minimale en pixels (composantes plus petites ignorees)
     * @return la liste des particules, numerotee dans l'ordre de detection
     */
    public static List<Particle> label(boolean[] mask, int width, int height, int minArea) {
        List<Particle> particles = new ArrayList<>();
        int[] labels = new int[mask.length];
        int[] queue = new int[mask.length];
        int currentLabel = 0;
        int id = 1;

        for (int start = 0; start < mask.length; start++) {
            if (!mask[start] || labels[start] != 0) {
                continue;
            }
            currentLabel++;
            int seedX = start % width;
            int seedY = start / width;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            labels[start] = currentLabel;
            long area = 0;
            long sumX = 0;
            long sumY = 0;
            double sumXX = 0;
            double sumYY = 0;
            double sumXY = 0;
            int minX = width;
            int minY = height;
            int maxX = 0;
            int maxY = 0;

            while (head < tail) {
                int index = queue[head++];
                int x = index % width;
                int y = index / width;
                area++;
                sumX += x;
                sumY += y;
                sumXX += (double) x * x;
                sumYY += (double) y * y;
                sumXY += (double) x * y;
                if (x < minX) {
                    minX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= height) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= width) {
                            continue;
                        }
                        int neighbour = ny * width + nx;
                        if (mask[neighbour] && labels[neighbour] == 0) {
                            labels[neighbour] = currentLabel;
                            queue[tail++] = neighbour;
                        }
                    }
                }
            }

            if (area < Math.max(1, minArea)) {
                continue;
            }
            double perimeter = perimeter(labels, width, height, currentLabel, seedX, seedY, area);
            particles.add(buildParticle(id, area, sumX, sumY, sumXX, sumYY, sumXY,
                    minX, minY, maxX, maxY, perimeter, width, height));
            id++;
        }
        return particles;
    }

    private static Particle buildParticle(int id, long area, long sumX, long sumY,
                                          double sumXX, double sumYY, double sumXY,
                                          int minX, int minY, int maxX, int maxY,
                                          double perimeter, int width, int height) {
        double cx = (double) sumX / area;
        double cy = (double) sumY / area;
        double mu20 = sumXX / area - cx * cx;
        double mu02 = sumYY / area - cy * cy;
        double mu11 = sumXY / area - cx * cy;

        double common = Math.sqrt(4 * mu11 * mu11 + (mu20 - mu02) * (mu20 - mu02));
        double lambda1 = Math.max(0, (mu20 + mu02 + common) / 2);
        double lambda2 = Math.max(0, (mu20 + mu02 - common) / 2);
        double majorAxis = 4 * Math.sqrt(lambda1);
        double minorAxis = 4 * Math.sqrt(lambda2);
        double orientation = 0.5 * Math.atan2(2 * mu11, mu20 - mu02);

        // Inegalite isoperimetrique : aucun contour ne peut etre plus court que
        // 2*sqrt(pi*A). On borne le perimetre estime par cette valeur, ce qui
        // garantit une circularite physiquement valide (0 <= C <= 1).
        double minimalPerimeter = 2 * Math.sqrt(Math.PI * area);
        if (perimeter < minimalPerimeter) {
            perimeter = minimalPerimeter;
        }
        double circularity = perimeter > 0
                ? Math.min(1.0, 4 * Math.PI * area / (perimeter * perimeter))
                : 0;
        double equivalentDiameter = 2 * Math.sqrt(area / Math.PI);
        boolean touchesBorder = minX <= 0 || minY <= 0 || maxX >= width - 1 || maxY >= height - 1;

        return new Particle(id, area, perimeter, cx, cy,
                minX, minY, maxX - minX + 1, maxY - minY + 1,
                majorAxis, minorAxis, Math.toDegrees(orientation),
                circularity, equivalentDiameter, touchesBorder);
    }

    /** Directions voisines dans l'ordre horaire, en commencant par l'est. */
    private static final int[] DIR_X = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DIR_Y = {0, 1, 1, 1, 0, -1, -1, -1};

    /**
     * Perimetre du contour exterieur, obtenu par suivi de frontiere de Moore et
     * sommation des pas unitaires (1 px) et diagonaux (√2 px).
     *
     * <p>Cette methode mesure le polygone passant par les centres des pixels de
     * bord ; contrairement au comptage des cotes exposees (perimetre de
     * Manhattan, surestime d'environ 27 % sur un disque), elle reste quasi
     * isotrope et exacte pour les formes alignees sur la grille.</p>
     */
    private static double perimeter(int[] labels, int width, int height, int label,
                                    int startX, int startY, long area) {
        if (area <= 1) {
            return 4.0;
        }
        int startIndex = startY * width + startX;
        int currentX = startX;
        int currentY = startY;
        int backX = startX - 1;
        int backY = startY;
        double length = 0;
        int guard = 8 * (width + height) + 64;
        boolean started = false;

        while (guard-- > 0) {
            int backDirection = directionIndex(backX - currentX, backY - currentY);
            int found = -1;
            for (int k = 1; k <= 8; k++) {
                int direction = (backDirection + k) % 8;
                int nx = currentX + DIR_X[direction];
                int ny = currentY + DIR_Y[direction];
                if (nx >= 0 && ny >= 0 && nx < width && ny < height
                        && labels[ny * width + nx] == label) {
                    found = direction;
                    break;
                }
            }
            if (found < 0) {
                break;
            }
            int nx = currentX + DIR_X[found];
            int ny = currentY + DIR_Y[found];
            length += (DIR_X[found] != 0 && DIR_Y[found] != 0) ? Math.sqrt(2.0) : 1.0;

            int previous = (found - 1 + 8) % 8;
            backX = currentX + DIR_X[previous];
            backY = currentY + DIR_Y[previous];
            currentX = nx;
            currentY = ny;

            if (started && currentY * width + currentX == startIndex) {
                break;
            }
            started = true;
        }
        return length > 0 ? length : 4.0;
    }

    private static int directionIndex(int dx, int dy) {
        for (int i = 0; i < DIR_X.length; i++) {
            if (DIR_X[i] == dx && DIR_Y[i] == dy) {
                return i;
            }
        }
        return 0;
    }

    private static boolean belongs(int[] labels, int width, int height, int x, int y, int label) {
        return x >= 0 && y >= 0 && x < width && y < height && labels[y * width + x] == label;
    }

    /**
     * Construit le masque binaire a partir des niveaux de gris et d'un seuil.
     *
     * @param gray           niveaux de gris
     * @param threshold      seuil
     * @param foregroundDark true si les particules sont plus sombres que le fond
     * @return le masque binaire
     */
    public static boolean[] binarize(int[] gray, int threshold, boolean foregroundDark) {
        boolean[] mask = new boolean[gray.length];
        for (int i = 0; i < gray.length; i++) {
            boolean dark = gray[i] <= threshold;
            mask[i] = foregroundDark == dark;
        }
        return mask;
    }

    /** @return le nombre de pixels du masque (aire totale detectee). */
    public static long maskArea(boolean[] mask) {
        long count = 0;
        for (boolean b : mask) {
            if (b) {
                count++;
            }
        }
        return count;
    }
}
