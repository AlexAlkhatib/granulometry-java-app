package fr.granulometrie.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Images synthetiques partagees par les tests (3 disques de diametres connus). */
public final class TestImages {

    private TestImages() {
    }

    /** @return une image 400x300 blanche avec trois disques noirs (Ø 80, 40 et 20 px). */
    public static BufferedImage circles() {
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 400, 300);
        g.setColor(Color.BLACK);
        g.fillOval(40, 40, 80, 80);
        g.fillOval(200, 60, 40, 40);
        g.fillOval(300, 180, 20, 20);
        g.dispose();
        return image;
    }
}
