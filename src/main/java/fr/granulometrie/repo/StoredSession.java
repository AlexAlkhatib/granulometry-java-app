package fr.granulometrie.repo;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.List;

import fr.granulometrie.core.AnalysisParameters;
import fr.granulometrie.core.Particle;

/**
 * Une session d'analyse enregistree dans la base locale.
 *
 * @param id             identifiant de la session
 * @param createdAt      date et heure d'enregistrement
 * @param imageName      nom du fichier image
 * @param imageWidth     largeur de l'image
 * @param imageHeight    hauteur de l'image
 * @param parameters     parametres utilises
 * @param thresholdUsed  seuil applique
 * @param particleCount  nombre de particules retenues
 * @param d10            D10 en unite utilisateur
 * @param d50            D50 en unite utilisateur
 * @param d90            D90 en unite utilisateur
 * @param meanDiameter   diametre moyen
 * @param comment        commentaire de l'operateur
 * @param image          image d'origine restauree (null si non demandee)
 * @param particles      particules enregistrees (null si non demandees)
 */
public record StoredSession(
        long id,
        LocalDateTime createdAt,
        String imageName,
        int imageWidth,
        int imageHeight,
        AnalysisParameters parameters,
        int thresholdUsed,
        int particleCount,
        double d10,
        double d50,
        double d90,
        double meanDiameter,
        String comment,
        BufferedImage image,
        List<Particle> particles) {

    /** @return une representation courte pour les listes deroulantes. */
    public String shortLabel() {
        return "#" + id + " — " + imageName + " (" + particleCount + " particules)";
    }
}
