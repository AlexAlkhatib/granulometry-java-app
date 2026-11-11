package fr.granulometrie.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import javax.imageio.ImageIO;

import fr.granulometrie.core.AnalysisResult;
import fr.granulometrie.core.Particle;
import fr.granulometrie.core.SizeClassifier;
import fr.granulometrie.core.SizeClassifier.SizeClass;
import fr.granulometrie.core.SizeStatistics;
import fr.granulometrie.io.CsvExporter;
import fr.granulometrie.repo.AnalysisRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Vue de resultats : indicateurs, distributions, tableau des particules et export. */
public final class ResultView extends BorderPane {

    private static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.FRANCE);
    private static final NumberFormat INTEGER = NumberFormat.getIntegerInstance(Locale.FRANCE);

    static {
        NUMBER.setMaximumFractionDigits(2);
        NUMBER.setMinimumFractionDigits(1);
    }

    private final AnalysisRepository repository;
    private final java.util.function.Consumer<String> status;

    private AnalysisResult result;
    private final Label sourceLabel = new Label("Aucun résultat");
    private final Button saveButton = new Button("Enregistrer dans l'historique");

    private final UiFactory.KpiCard kpiCount = UiFactory.kpi("Particules retenues");
    private final UiFactory.KpiCard kpiD10 = UiFactory.kpi("D10");
    private final UiFactory.KpiCard kpiD50 = UiFactory.kpi("D50");
    private final UiFactory.KpiCard kpiD90 = UiFactory.kpi("D90");
    private final UiFactory.KpiCard kpiCircularity = UiFactory.kpi("Circularité moyenne");
    private final UiFactory.KpiCard kpiCoverage = UiFactory.kpi("Surface occupée");

    private final BarChart<String, Number> sizeChart =
            new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final LineChart<Number, Number> cumulativeChart =
            new LineChart<>(new NumberAxis(), new NumberAxis());
    private final BarChart<String, Number> surfaceChart =
            new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final TextField binWidthField = new TextField("5");
    private final TextField surfaceBinField = new TextField("1");

    private final TableView<ParticleRow> table = new TableView<>();
    private final GridPane statsGrid = new GridPane();
    private final TextArea parametersArea = new TextArea();
    private final TabPane tabs = new TabPane();

    public ResultView(AnalysisRepository repository, java.util.function.Consumer<String> status, Runnable onBack) {
        this.repository = repository;
        this.status = status;
        getStyleClass().add("view");

        Button back = new Button("← Analyse");
        back.getStyleClass().add("ghost-button");
        back.setOnAction(event -> onBack.run());

        Button exportCsv = new Button("Exporter CSV");
        exportCsv.getStyleClass().add("secondary-button");
        exportCsv.setOnAction(event -> exportCsv());

        Button exportCharts = new Button("Exporter les graphiques");
        exportCharts.getStyleClass().add("secondary-button");
        exportCharts.setOnAction(event -> exportCharts());

        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(event -> saveToHistory());
        saveButton.setDisable(repository == null);

        HBox toolbar = new HBox(12, back, sourceLabel, UiFactory.spacer(), exportCsv, exportCharts, saveButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new Insets(10, 16, 10, 16));

        HBox kpis = new HBox(12, kpiCount.node(), kpiD10.node(), kpiD50.node(),
                kpiD90.node(), kpiCircularity.node(), kpiCoverage.node());
        kpis.setPadding(new Insets(4, 16, 12, 16));

        buildCharts();
        buildTable();
        statsGrid.setPadding(new Insets(18));
        statsGrid.setHgap(26);
        statsGrid.setVgap(9);
        parametersArea.setEditable(false);
        parametersArea.setWrapText(true);

        for (Tab tab : List.of(
                tab("Distribution", buildDistributionTab()),
                tab("Particules", buildParticlesTab()),
                tab("Statistiques", buildStatisticsTab()),
                tab("Paramètres", buildParametersTab()))) {
            tabs.getTabs().add(tab);
        }

        setTop(new VBox(toolbar, kpis));
        setCenter(tabs);
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private void buildCharts() {
        sizeChart.setAnimated(false);
        cumulativeChart.setAnimated(false);
        surfaceChart.setAnimated(false);
        sizeChart.setLegendVisible(false);
        cumulativeChart.setLegendVisible(false);
        surfaceChart.setLegendVisible(false);
        sizeChart.setTitle("Nombre de particules par classe de diamètre");
        cumulativeChart.setTitle("Courbe cumulative des passants");
        surfaceChart.setTitle("Répartition de la surface occupée");
        ((CategoryAxis) sizeChart.getXAxis()).setLabel("Diamètre");
        ((NumberAxis) sizeChart.getYAxis()).setLabel("Nombre de particules");
        cumulativeChart.getXAxis().setLabel("Diamètre");
        cumulativeChart.getYAxis().setLabel("Passants cumulés (%)");
        ((CategoryAxis) surfaceChart.getXAxis()).setLabel("Part de l'image par particule (%)");
        ((NumberAxis) surfaceChart.getYAxis()).setLabel("Nombre de particules");
    }

    private javafx.scene.Node buildDistributionTab() {
        Button apply = new Button("Recalculer");
        apply.getStyleClass().add("secondary-button");
        apply.setOnAction(event -> refreshCharts());
        binWidthField.setPrefWidth(80);
        surfaceBinField.setPrefWidth(80);

        HBox controls = new HBox(10,
                UiFactory.caption("Largeur de classe (diamètre) :"), binWidthField,
                UiFactory.caption("Largeur de classe (surface, %) :"), surfaceBinField,
                apply);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(12, 16, 0, 16));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 16, 16, 16));
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        half.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(half, half);
        RowConstraints top = new RowConstraints();
        top.setPercentHeight(48);
        top.setVgrow(Priority.ALWAYS);
        RowConstraints bottom = new RowConstraints();
        bottom.setPercentHeight(48);
        bottom.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(top, bottom);

        grid.add(sizeChart, 0, 0);
        grid.add(cumulativeChart, 1, 0);
        grid.add(surfaceChart, 0, 1, 2, 1);
        GridPane.setVgrow(sizeChart, Priority.ALWAYS);
        GridPane.setVgrow(cumulativeChart, Priority.ALWAYS);
        GridPane.setVgrow(surfaceChart, Priority.ALWAYS);

        VBox box = new VBox(controls, grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
        return box;
    }

    private void buildTable() {
        table.setPlaceholder(new Label("Aucune particule : lancez une analyse."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private javafx.scene.Node buildParticlesTab() {
        VBox box = new VBox(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(12, 16, 16, 16));
        return box;
    }

    private javafx.scene.Node buildStatisticsTab() {
        VBox box = new VBox(statsGrid);
        box.setPadding(new Insets(8));
        return box;
    }

    private javafx.scene.Node buildParametersTab() {
        VBox box = new VBox(parametersArea);
        VBox.setVgrow(parametersArea, Priority.ALWAYS);
        box.setPadding(new Insets(16));
        return box;
    }

    /** Met a jour toute la vue avec un nouveau resultat. */
    public void setResult(AnalysisResult analysisResult) {
        this.result = analysisResult;
        SizeStatistics stats = analysisResult.getStatistics();
        String unit = analysisResult.getUnit();

        sourceLabel.setText(analysisResult.getImageName() + " — " + analysisResult.getWidth()
                + " × " + analysisResult.getHeight() + " px");
        sourceLabel.getStyleClass().add("source-label");

        kpiCount.valueLabel().setText(INTEGER.format(stats.count()));
        kpiD10.valueLabel().setText(NUMBER.format(stats.d10()) + " " + unit);
        kpiD50.valueLabel().setText(NUMBER.format(stats.d50()) + " " + unit);
        kpiD90.valueLabel().setText(NUMBER.format(stats.d90()) + " " + unit);
        kpiCircularity.valueLabel().setText(NUMBER.format(stats.meanCircularity()));
        kpiCoverage.valueLabel().setText(NUMBER.format(stats.areaCoverage()) + " %");

        refreshCharts();
        refreshTable();
        refreshStatistics();
        parametersArea.setText(buildParametersText());
        saveButton.setDisable(repository == null);
    }

    private void refreshCharts() {
        if (result == null) {
            return;
        }
        double pixelSize = result.getPixelSize();
        String unit = result.getUnit();

        double binWidth = Math.max(0.01, UiFactory.parseDouble(binWidthField.getText(), 5));
        List<SizeClass> sizeClasses = SizeClassifier.histogramOfDiameters(
                result.getParticles(), binWidth, pixelSize);

        sizeChart.getData().clear();
        XYChart.Series<String, Number> sizeSeries = new XYChart.Series<>();
        for (SizeClass sizeClass : sizeClasses) {
            sizeSeries.getData().add(new XYChart.Data<>(categoryLabel(sizeClass, unit), sizeClass.count()));
        }
        sizeChart.getData().add(sizeSeries);
        ((CategoryAxis) sizeChart.getXAxis()).setLabel("Diamètre (" + unit + ")");

        cumulativeChart.getData().clear();
        XYChart.Series<Number, Number> cumulativeSeries = new XYChart.Series<>();
        for (double[] point : SizeClassifier.cumulativeCurve(sizeClasses)) {
            cumulativeSeries.getData().add(new XYChart.Data<>(point[0], point[1]));
        }
        cumulativeChart.getData().add(cumulativeSeries);
        cumulativeChart.getXAxis().setLabel("Diamètre (" + unit + ")");

        double surfaceBin = Math.max(0.01, UiFactory.parseDouble(surfaceBinField.getText(), 1));
        long imagePixels = (long) result.getWidth() * result.getHeight();
        List<SizeClass> surfaceClasses = SizeClassifier.histogramOfSurfaceShare(
                result.getParticles(), surfaceBin, imagePixels);
        surfaceChart.getData().clear();
        XYChart.Series<String, Number> surfaceSeries = new XYChart.Series<>();
        for (SizeClass sizeClass : surfaceClasses) {
            surfaceSeries.getData().add(new XYChart.Data<>(
                    String.format(Locale.FRANCE, "%.1f – %.1f", sizeClass.lowerBound(), sizeClass.upperBound()),
                    sizeClass.count()));
        }
        surfaceChart.getData().add(surfaceSeries);
    }

    private String categoryLabel(SizeClass sizeClass, String unit) {
        return String.format(Locale.FRANCE, "%.1f–%.1f", sizeClass.lowerBound(), sizeClass.upperBound());
    }

    private void refreshTable() {
        if (result == null) {
            return;
        }
        String unit = result.getUnit();
        double scale = result.getPixelSize();
        List<ParticleRow> rows = new ArrayList<>();
        for (Particle particle : result.getParticles()) {
            rows.add(new ParticleRow(
                    particle.id(),
                    particle.equivalentDiameter(scale),
                    particle.area(scale),
                    particle.majorAxis() * scale,
                    particle.minorAxis() * scale,
                    particle.circularity(),
                    particle.aspectRatio(),
                    particle.centroidX() * scale,
                    particle.centroidY() * scale,
                    particle.bboxWidth(),
                    particle.bboxHeight(),
                    particle.touchesBorder() ? "oui" : "non"));
        }
        table.getColumns().setAll(
                column("N°", ParticleRow::index, 50),
                column("Ø équiv. (" + unit + ")", ParticleRow::diameter, 110),
                column("Aire (" + result.getAreaUnit() + ")", ParticleRow::area, 110),
                column("Grand axe (" + unit + ")", ParticleRow::major, 115),
                column("Petit axe (" + unit + ")", ParticleRow::minor, 110),
                column("Circularité", ParticleRow::circularity, 95),
                column("Aspect", ParticleRow::aspectRatio, 80),
                column("X (" + unit + ")", ParticleRow::x, 95),
                column("Y (" + unit + ")", ParticleRow::y, 95),
                column("L bbox (px)", ParticleRow::bboxWidth, 90),
                column("H bbox (px)", ParticleRow::bboxHeight, 90),
                column("Bord", ParticleRow::touchesBorder, 70));
        table.setItems(FXCollections.observableArrayList(rows));
    }

    private <T> TableColumn<ParticleRow, T> column(String title, Function<ParticleRow, T> extractor,
                                                   double width) {
        TableColumn<ParticleRow, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(extractor.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setSortable(true);
        return column;
    }

    private void refreshStatistics() {
        SizeStatistics stats = result.getStatistics();
        String unit = result.getUnit();
        statsGrid.getChildren().clear();
        int row = 0;
        row = addStat(row, "Particules détectées (avant filtres)",
                INTEGER.format(result.getAllParticles().size()));
        row = addStat(row, "Particules retenues", INTEGER.format(stats.count()));
        row = addStat(row, "Diamètre minimal", NUMBER.format(stats.minDiameter()) + " " + unit);
        row = addStat(row, "Diamètre maximal", NUMBER.format(stats.maxDiameter()) + " " + unit);
        row = addStat(row, "Diamètre moyen", NUMBER.format(stats.meanDiameter()) + " " + unit);
        row = addStat(row, "Diamètre médian", NUMBER.format(stats.medianDiameter()) + " " + unit);
        row = addStat(row, "Écart type", NUMBER.format(stats.standardDeviation()) + " " + unit);
        row = addStat(row, "D10 / D50 / D90", NUMBER.format(stats.d10()) + " / "
                + NUMBER.format(stats.d50()) + " / " + NUMBER.format(stats.d90()) + " " + unit);
        row = addStat(row, "Circularité moyenne (4πA/P²)", NUMBER.format(stats.meanCircularity()));
        row = addStat(row, "Rapport d'aspect moyen", NUMBER.format(stats.meanAspectRatio()));
        row = addStat(row, "Aire moyenne", NUMBER.format(stats.meanArea()) + " " + result.getAreaUnit());
        row = addStat(row, "Aire cumulée", NUMBER.format(stats.totalArea()) + " " + result.getAreaUnit());
        row = addStat(row, "Surface de l'image occupée", NUMBER.format(stats.areaCoverage()) + " %");
        addStat(row, "Seuil appliqué (Otsu)", result.getThresholdUsed() + " (" + result.getAutoThreshold() + ")");
    }

    private int addStat(int row, String caption, String value) {
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-caption");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        statsGrid.add(captionLabel, 0, row);
        statsGrid.add(valueLabel, 1, row);
        return row + 1;
    }

    private String buildParametersText() {
        StringBuilder builder = new StringBuilder();
        builder.append("Image          : ").append(result.getImageName()).append('\n');
        builder.append("Dimensions     : ").append(result.getWidth()).append(" × ")
                .append(result.getHeight()).append(" px\n");
        builder.append("Calibration    : ").append(result.getPixelSize()).append(" µm/px (")
                .append(result.getUnit()).append(")\n");
        builder.append("Seuil          : ").append(result.getThresholdUsed())
                .append(" (Otsu automatique : ").append(result.getAutoThreshold()).append(")\n");
        builder.append("Polarité       : ").append(result.isForegroundDark()
                ? "particules sombres sur fond clair" : "particules claires sur fond sombre").append('\n');
        builder.append("Paramètres     : ").append(result.getParameters()).append('\n');
        builder.append("Méthode        : seuillage global + composantes connexes 8-connexes\n");
        builder.append("Mesures        : aire, périmètre, barycentre, moments d'ordre 2\n");
        builder.append("Diamètre       : équivalent disque de même aire (2·√(A/π))\n");
        return builder.toString();
    }

    private void exportCsv() {
        if (result == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les résultats en CSV");
        chooser.setInitialFileName(defaultFileName() + "-granulometrie.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) {
            return;
        }
        try {
            CsvExporter.export(file.toPath(), result);
            status.accept("Export CSV réussi : " + file.getAbsolutePath());
        } catch (IOException e) {
            error("Export impossible", e.getMessage());
        }
    }

    private void exportCharts() {
        if (result == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les graphiques en PNG");
        chooser.setInitialFileName(defaultFileName() + "-graphiques.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) {
            return;
        }
        try {
            WritableImage leftImage = sizeChart.snapshot(new SnapshotParameters(), null);
            WritableImage rightImage = cumulativeChart.snapshot(new SnapshotParameters(), null);
            WritableImage lowerImage = surfaceChart.snapshot(new SnapshotParameters(), null);
            BufferedImage left = SwingFXUtils.fromFXImage(leftImage, null);
            BufferedImage right = SwingFXUtils.fromFXImage(rightImage, null);
            BufferedImage lower = SwingFXUtils.fromFXImage(lowerImage, null);

            int width = left.getWidth() + right.getWidth();
            int height = Math.max(left.getHeight(), right.getHeight()) + lower.getHeight();
            BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = combined.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(left, 0, 0, null);
            g.drawImage(right, left.getWidth(), 0, null);
            g.drawImage(lower, 0, Math.max(left.getHeight(), right.getHeight()), null);
            g.dispose();
            ImageIO.write(combined, "png", file);
            status.accept("Graphiques exportés : " + file.getAbsolutePath());
        } catch (IOException e) {
            error("Export impossible", e.getMessage());
        }
    }

    private void saveToHistory() {
        if (result == null || repository == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Enregistrer la session");
        dialog.setHeaderText("Commentaire associé à cette analyse (facultatif)");
        dialog.setContentText("Commentaire :");
        Optional<String> comment = dialog.showAndWait();
        if (comment.isEmpty()) {
            return;
        }
        try {
            long id = repository.save(result, comment.get());
            status.accept("Session #" + id + " enregistrée dans l'historique local.");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Historique");
            alert.setHeaderText("Session enregistrée");
            alert.setContentText("La session #" + id + " et ses " + result.getParticles().size()
                    + " particules sont disponibles dans l'onglet Historique.");
            alert.showAndWait();
        } catch (RuntimeException e) {
            error("Enregistrement impossible", e.getMessage());
        }
    }

    private String defaultFileName() {
        String name = result.getImageName();
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name).replaceAll("[^\\w\\-]", "_");
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Granulométrie Studio");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Ligne du tableau des particules (valeurs deja converties). */
    public record ParticleRow(int index, double diameter, double area, double major, double minor,
                              double circularity, double aspectRatio, double x, double y,
                              int bboxWidth, int bboxHeight, String touchesBorder) {
    }
}
