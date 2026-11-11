package fr.granulometrie.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Petites fabriques de composants partagees par les vues. */
public final class UiFactory {

    private UiFactory() {
    }

    /** @return un titre de section du panneau lateral. */
    public static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    /** @return un libelle de champ discret. */
    public static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-caption");
        return label;
    }

    /** @return une ligne "libelle : composant" etiree sur toute la largeur. */
    public static HBox fieldRow(String caption, Node control) {
        Label label = caption(caption);
        label.setMinWidth(150);
        HBox row = new HBox(8, label, control);
        row.setPadding(new Insets(2, 0, 2, 0));
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }

    /** @return une zone extensible servant d'espaceur. */
    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    /**
     * Cree une carte de synthese (valeur mise en avant + legende).
     *
     * @param caption legende de la carte
     * @return la carte, dont la valeur peut etre mise a jour
     */
    public static KpiCard kpi(String caption) {
        Label value = new Label("—");
        value.getStyleClass().add("kpi-value");
        Label legend = new Label(caption);
        legend.getStyleClass().add("kpi-caption");
        VBox box = new VBox(2, value, legend);
        box.getStyleClass().add("kpi-card");
        box.setMinWidth(140);
        return new KpiCard(box, value);
    }

    /** Carte de synthese : noeud affiche et libelle de valeur modifiable. */
    public record KpiCard(VBox node, Label valueLabel) {
    }

    /** Analyse un nombre decimal en acceptant la virgule francaise. */
    public static double parseDouble(String text, double fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Analyse un entier, avec valeur de repli. */
    public static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
