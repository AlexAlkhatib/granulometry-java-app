package fr.granulometrie.io;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

import fr.granulometrie.core.Particle;

/**
 * Dessine le contour (ellipse d'inertie equivalente) et l'index des particules
 * detectees sur une copie de l'image, sans modifier l'original.
 */
public final class OverlayRenderer {

    private static final Color CONTOUR = new Color(255, 140, 0);
    private static final Color LABEL_BACKGROUND = new Color(0, 0, 0, 150);
    private static final Color LABEL_TEXT = Color.WHITE;

    private OverlayRenderer() {
    }

    /**
     * @param source  image d'origine
     * @param particles particules a dessiner
     * @param numbers true pour afficher l'index de chaque particule
     * @param boxes   true pour afficher la boite englobante
     * @return une copie de l'image avec le calque de detection
     */
    public static BufferedImage render(BufferedImage source, List<Particle> particles,
                                       boolean numbers, boolean boxes) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(CONTOUR);

        Font font = new Font("SansSerif", Font.BOLD, Math.max(9, source.getWidth() / 160));
        g.setFont(font);

        for (Particle particle : particles) {
            double major = Math.max(2, particle.majorAxis());
            double minor = Math.max(2, particle.minorAxis());
            AffineTransform previous = g.getTransform();
            AffineTransform transform = new AffineTransform();
            transform.translate(particle.centroidX(), particle.centroidY());
            transform.rotate(Math.toRadians(particle.orientationDegrees()));
            g.transform(transform);
            g.draw(new Ellipse2D.Double(-major / 2, -minor / 2, major, minor));
            g.setTransform(previous);

            if (boxes) {
                g.setColor(new Color(0, 200, 255, 120));
                g.drawRect(particle.bboxX(), particle.bboxY(), particle.bboxWidth(), particle.bboxHeight());
                g.setColor(CONTOUR);
            }
            if (numbers) {
                String text = Integer.toString(particle.id());
                int textWidth = g.getFontMetrics().stringWidth(text);
                int x = (int) Math.round(particle.bboxX() + particle.bboxWidth() / 2.0 - textWidth / 2.0);
                int y = (int) Math.round(particle.bboxY() - 2);
                g.setColor(LABEL_BACKGROUND);
                g.fillRect(x - 2, y - g.getFontMetrics().getAscent(), textWidth + 4,
                        g.getFontMetrics().getHeight());
                g.setColor(LABEL_TEXT);
                g.drawString(text, x, y);
                g.setColor(CONTOUR);
            }
        }
        g.dispose();
        return copy;
    }
}
