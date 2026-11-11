package fr.granulometrie.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import fr.granulometrie.core.AnalysisParameters;
import fr.granulometrie.core.AnalysisResult;
import fr.granulometrie.core.ImageAnalyzer;
import fr.granulometrie.io.OverlayRenderer;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Vue d'analyse : parametres a gauche, apercu et calque de detection a droite. */
public final class AnalyzeView extends BorderPane {

    private final Consumer<AnalysisResult> onResult;
    private final Consumer<String> status;
    private final Consumer<Boolean> busy;

    private final AnalysisParameters parameters = new AnalysisParameters();
    private final ImageView imageView = new ImageView();
    private final Label placeholder = new Label(
            "Importez une image ou glissez-déposez un fichier ici\n(png, jpg, bmp, tif…)");
    private final StackPane previewPane = new StackPane();
    private final Label imageInfo = new Label("Aucune image chargée");

    private final ComboBox<String> thresholdMode = new ComboBox<>();
    private final Slider thresholdSlider = new Slider(0, 255, 128);
    private final Label thresholdValue = new Label("128");
    private final Label autoThresholdLabel = new Label("Seuil automatique : —");
    private final CheckBox invertPolarity = new CheckBox("Inverser la polarité (particules claires)");
    private final Spinner<Integer> blurSpinner = new Spinner<>(0, 5, 0);
    private final TextField minDiameterField = new TextField("0");
    private final TextField maxDiameterField = new TextField("0");
    private final TextField minCircularityField = new TextField("0");
    private final CheckBox excludeBorder = new CheckBox("Exclure les particules coupées par le bord");
    private final TextField pixelSizeField = new TextField("1,0");
    private final Label unitHint = new Label("Analyse en pixels (1,0 µm/px).");
    private final CheckBox showOverlay = new CheckBox("Détections");
    private final CheckBox showNumbers = new CheckBox("Numéros");
    private final Button analyzeButton = new Button("Analyser l'image");

    private BufferedImage currentImage;
    private File currentFile;
    private AnalysisResult lastResult;

    public AnalyzeView(Consumer<AnalysisResult> onResult, Consumer<String> status, Consumer<Boolean> busy) {
        this.onResult = onResult;
        this.status = status;
        this.busy = busy;
        getStyleClass().add("view");

        setLeft(buildSidePanel());
        setCenter(buildPreviewArea());

        showOverlay.setSelected(true);
        showOverlay.selectedProperty().addListener((obs, old, value) -> refreshPreview());
        showNumbers.selectedProperty().addListener((obs, old, value) -> refreshPreview());
        analyzeButton.setOnAction(event -> analyze());
    }

    private ScrollPane buildSidePanel() {
        Button importButton = new Button("Importer une image…");
        importButton.getStyleClass().add("secondary-button");
        importButton.setMaxWidth(Double.MAX_VALUE);
        importButton.setOnAction(event -> importImage());

        Button sampleButton = new Button("Charger l'exemple fourni");
        sampleButton.getStyleClass().add("ghost-button");
        sampleButton.setMaxWidth(Double.MAX_VALUE);
        sampleButton.setOnAction(event -> loadSample());

        imageInfo.getStyleClass().add("hint");
        imageInfo.setWrapText(true);
        VBox imageSection = new VBox(8,
                UiFactory.sectionTitle("1 · Image"),
                importButton,
                sampleButton,
                imageInfo);

        thresholdMode.getItems().addAll("Automatique (Otsu)", "Manuel");
        thresholdMode.getSelectionModel().selectFirst();
        thresholdMode.setMaxWidth(Double.MAX_VALUE);
        thresholdMode.valueProperty().addListener((obs, old, value) -> updateThresholdControls());

        thresholdSlider.setDisable(true);
        thresholdSlider.setBlockIncrement(1);
        thresholdSlider.valueProperty().addListener((obs, old, value) ->
                thresholdValue.setText(Integer.toString(value.intValue())));
        thresholdValue.setDisable(true);
        thresholdValue.setMinWidth(34);
        HBox thresholdRow = new HBox(8, thresholdSlider, thresholdValue);
        HBox.setHgrow(thresholdSlider, Priority.ALWAYS);
        thresholdRow.setAlignment(Pos.CENTER_LEFT);
        autoThresholdLabel.getStyleClass().add("hint");

        VBox segmentationSection = new VBox(8,
                UiFactory.sectionTitle("2 · Segmentation"),
                UiFactory.caption("Seuil de binarisation"),
                thresholdMode,
                thresholdRow,
                autoThresholdLabel,
                invertPolarity,
                blurSpinnerRow(),
                UiFactory.caption("Le filtre moyenneur atténue le bruit avant seuillage."));

        GridPane filters = new GridPane();
        filters.setHgap(8);
        filters.setVgap(6);
        minDiameterField.setPrefWidth(90);
        maxDiameterField.setPrefWidth(90);
        minCircularityField.setPrefWidth(90);
        filters.add(UiFactory.caption("Diamètre min."), 0, 0);
        filters.add(minDiameterField, 1, 0);
        filters.add(UiFactory.caption("Diamètre max. (0 = ∞)"), 0, 1);
        filters.add(maxDiameterField, 1, 1);
        filters.add(UiFactory.caption("Circularité min. (0 → 1)"), 0, 2);
        filters.add(minCircularityField, 1, 2);
        GridPane.setHgrow(minDiameterField, Priority.ALWAYS);

        VBox filtersSection = new VBox(8,
                UiFactory.sectionTitle("3 · Filtres morphologiques"),
                filters,
                excludeBorder);

        pixelSizeField.setPrefWidth(90);
        pixelSizeField.textProperty().addListener((obs, old, value) -> {
            parameters.setPixelSizeMicrons(UiFactory.parseDouble(value, 1.0));
            unitHint.setText(parameters.isCalibrated()
                    ? "Calibration active : longueurs en µm."
                    : "Analyse en pixels (1,0 µm/px).");
        });
        VBox calibrationSection = new VBox(8,
                UiFactory.sectionTitle("4 · Calibration"),
                UiFactory.fieldRow("Taille d'un pixel (µm)", pixelSizeField),
                unitHint);

        Label shortcuts = new Label("Astuce : la molette de la souris ne modifie pas l'image "
                + "d'origine, le calque de détection est un rendu à part.");
        shortcuts.getStyleClass().add("hint");
        shortcuts.setWrapText(true);

        VBox panel = new VBox(18, imageSection, segmentationSection, filtersSection,
                calibrationSection, shortcuts);
        panel.getStyleClass().add("side-panel");
        panel.setPadding(new Insets(18));

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(380);
        scroll.setMinWidth(350);
        scroll.getStyleClass().add("side-scroll");
        return scroll;
    }

    private HBox blurSpinnerRow() {
        blurSpinner.setPrefWidth(80);
        HBox row = new HBox(10, UiFactory.caption("Lissage (rayon px)"), UiFactory.spacer(), blurSpinner);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildPreviewArea() {
        placeholder.getStyleClass().add("placeholder");
        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.fitWidthProperty().bind(previewPane.widthProperty().subtract(60));
        imageView.fitHeightProperty().bind(previewPane.heightProperty().subtract(60));

        previewPane.getChildren().addAll(placeholder, imageView);
        previewPane.getStyleClass().add("preview-pane");
        StackPane.setAlignment(placeholder, Pos.CENTER);
        StackPane.setMargin(placeholder, new Insets(40));
        previewPane.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
        });
        previewPane.setOnDragDropped(event -> {
            Dragboard board = event.getDragboard();
            boolean success = false;
            if (board.hasFiles() && !board.getFiles().isEmpty()) {
                loadFile(board.getFiles().get(0));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        analyzeButton.getStyleClass().add("primary-button");
        HBox toolbar = new HBox(14,
                UiFactory.caption("Calque :"), showOverlay, showNumbers,
                UiFactory.spacer(), analyzeButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new Insets(10, 16, 10, 16));

        VBox box = new VBox(toolbar, previewPane);
        VBox.setVgrow(previewPane, Priority.ALWAYS);
        return box;
    }

    private void updateThresholdControls() {
        boolean manual = thresholdMode.getSelectionModel().getSelectedIndex() == 1;
        thresholdSlider.setDisable(!manual);
        thresholdValue.setDisable(!manual);
    }

    private void importImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image à analyser");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tif", "*.tiff", "*.gif"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            loadFile(file);
        }
    }

    private void loadSample() {
        try (InputStream stream = getClass().getResourceAsStream("/samples/exemple-grains.jpg")) {
            if (stream == null) {
                error("Exemple indisponible", "L'image d'exemple n'est pas présente dans l'application.");
                return;
            }
            Path temp = Files.createTempFile("granulometrie-exemple-", ".jpg");
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            loadFile(temp.toFile());
        } catch (IOException e) {
            error("Exemple indisponible", e.getMessage());
        }
    }

    private void loadFile(File file) {
        try {
            BufferedImage image = ImageAnalyzer.load(file);
            currentImage = image;
            currentFile = file;
            lastResult = null;
            imageInfo.setText(file.getName() + " — " + image.getWidth() + " × " + image.getHeight() + " px");
            placeholder.setVisible(false);
            refreshPreview();
            status.accept("Image chargée : " + file.getName());
        } catch (IOException e) {
            error("Import impossible", e.getMessage());
        }
    }

    private void analyze() {
        if (currentImage == null) {
            error("Aucune image", "Importez une image avant de lancer l'analyse.");
            return;
        }
        syncParametersFromControls();
        final BufferedImage image = currentImage;
        final File file = currentFile;
        final AnalysisParameters snapshot = parameters.copy();

        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() {
                return ImageAnalyzer.analyze(image, file, snapshot);
            }
        };
        task.setOnRunning(event -> {
            busy.accept(true);
            analyzeButton.setDisable(true);
            status.accept("Analyse en cours…");
        });
        task.setOnSucceeded(event -> {
            busy.accept(false);
            analyzeButton.setDisable(false);
            AnalysisResult result = task.getValue();
            lastResult = result;
            autoThresholdLabel.setText("Seuil automatique (Otsu) : " + result.getAutoThreshold()
                    + " · seuil appliqué : " + result.getThresholdUsed());
            if (!snapshot.isManualThreshold()) {
                thresholdSlider.setValue(result.getThresholdUsed());
                thresholdValue.setText(Integer.toString(result.getThresholdUsed()));
            }
            refreshPreview();
            status.accept(result.getParticles().size() + " particules retenues sur "
                    + result.getAllParticles().size() + " détectées (seuil "
                    + result.getThresholdUsed() + ").");
            onResult.accept(result);
        });
        task.setOnFailed(event -> {
            busy.accept(false);
            analyzeButton.setDisable(false);
            Throwable exception = task.getException();
            error("Analyse impossible", exception == null ? "Erreur inconnue"
                    : exception.getClass().getSimpleName() + " : " + exception.getMessage());
        });

        Thread thread = new Thread(task, "analyse-image");
        thread.setDaemon(true);
        thread.start();
    }

    private void syncParametersFromControls() {
        parameters.setManualThreshold(thresholdMode.getSelectionModel().getSelectedIndex() == 1);
        parameters.setThreshold((int) Math.round(thresholdSlider.getValue()));
        parameters.setInvertPolarity(invertPolarity.isSelected());
        parameters.setBlurRadius(blurSpinner.getValue());
        parameters.setMinDiameter(UiFactory.parseDouble(minDiameterField.getText(), 0));
        parameters.setMaxDiameter(UiFactory.parseDouble(maxDiameterField.getText(), 0));
        parameters.setMinCircularity(UiFactory.parseDouble(minCircularityField.getText(), 0));
        parameters.setExcludeBorder(excludeBorder.isSelected());
        parameters.setPixelSizeMicrons(UiFactory.parseDouble(pixelSizeField.getText(), 1.0));
    }

    private void refreshPreview() {
        if (currentImage == null) {
            imageView.setImage(null);
            placeholder.setVisible(true);
            return;
        }
        BufferedImage displayed = currentImage;
        if (lastResult != null && showOverlay.isSelected()) {
            displayed = OverlayRenderer.render(currentImage, lastResult.getParticles(),
                    showNumbers.isSelected(), false);
        }
        imageView.setImage(SwingFXUtils.toFXImage(displayed, null));
        placeholder.setVisible(false);
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Granulométrie Studio");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
