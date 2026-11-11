package fr.granulometrie.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import fr.granulometrie.core.AnalysisResult;
import fr.granulometrie.repo.AnalysisRepository;
import fr.granulometrie.repo.StoredSession;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Vue d'historique : sessions enregistrees dans la base locale H2. */
public final class HistoryView extends BorderPane {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AnalysisRepository repository;
    private final Consumer<AnalysisResult> onOpen;
    private final Consumer<String> status;

    private final ObservableList<StoredSession> sessions = FXCollections.observableArrayList();
    private final TableView<StoredSession> table = new TableView<>(sessions);
    private final ImageView preview = new ImageView();
    private final Label details = new Label("Sélectionnez une session pour afficher son aperçu.");
    private final Label countLabel = new Label("0 session");
    private long previewedId = -1;

    public HistoryView(AnalysisRepository repository, Consumer<AnalysisResult> onOpen,
                       Consumer<String> status) {
        this.repository = repository;
        this.onOpen = onOpen;
        this.status = status;
        getStyleClass().add("view");

        if (repository == null) {
            buildUnavailable();
            return;
        }
        buildTable();

        Button refresh = new Button("Rafraîchir");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(event -> refresh());

        Button open = new Button("Ouvrir la session");
        open.getStyleClass().add("primary-button");
        open.setOnAction(event -> openSelected());

        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteSelected());

        HBox toolbar = new HBox(12, refresh, open, delete, UiFactory.spacer(), countLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new Insets(10, 16, 10, 16));

        preview.setFitWidth(320);
        preview.setFitHeight(240);
        preview.setPreserveRatio(true);
        details.setWrapText(true);
        details.getStyleClass().add("hint");

        VBox previewPanel = new VBox(12, new Label("Aperçu"), preview, details);
        ((Label) previewPanel.getChildren().get(0)).getStyleClass().add("section-title");
        previewPanel.setPadding(new Insets(18));
        previewPanel.setPrefWidth(360);
        previewPanel.getStyleClass().add("side-panel");

        setTop(toolbar);
        setCenter(table);
        setRight(previewPanel);
        BorderPane.setMargin(table, new Insets(0, 0, 0, 16));
        table.setPlaceholder(new Label("Aucune session enregistrée pour le moment."));

        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> showPreview(selected));

        refresh();
    }

    private void buildUnavailable() {
        Label message = new Label("L'historique local n'est pas disponible.\n"
                + "Les analyses restent utilisables, mais l'enregistrement des sessions est désactivé.");
        message.getStyleClass().add("placeholder");
        setCenter(message);
    }

    private void buildTable() {
        TableColumn<StoredSession, Number> idColumn = new TableColumn<>("N°");
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        idColumn.setPrefWidth(60);

        TableColumn<StoredSession, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().createdAt().format(DATE_FORMAT)));
        dateColumn.setPrefWidth(140);

        TableColumn<StoredSession, String> imageColumn = new TableColumn<>("Image");
        imageColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().imageName()));
        imageColumn.setPrefWidth(240);

        TableColumn<StoredSession, Number> countColumn = new TableColumn<>("Particules");
        countColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().particleCount()));
        countColumn.setPrefWidth(90);

        TableColumn<StoredSession, Number> d50Column = new TableColumn<>("D50");
        d50Column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                Math.round(cell.getValue().d50() * 100.0) / 100.0));
        d50Column.setPrefWidth(90);

        TableColumn<StoredSession, String> unitColumn = new TableColumn<>("Unité");
        unitColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().parameters().unit()));
        unitColumn.setPrefWidth(70);

        TableColumn<StoredSession, String> commentColumn = new TableColumn<>("Commentaire");
        commentColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().comment()));
        commentColumn.setPrefWidth(260);

        table.getColumns().setAll(List.of(idColumn, dateColumn, imageColumn, countColumn,
                d50Column, unitColumn, commentColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /** Recharge la liste des sessions enregistrees. */
    public void refresh() {
        if (repository == null) {
            return;
        }
        try {
            List<StoredSession> stored = repository.listSessions();
            sessions.setAll(stored);
            countLabel.setText(stored.size() + (stored.size() > 1 ? " sessions" : " session"));
        } catch (RuntimeException e) {
            status.accept(e.getMessage());
        }
    }

    private void showPreview(StoredSession session) {
        if (session == null) {
            preview.setImage(null);
            details.setText("Sélectionnez une session pour afficher son aperçu.");
            previewedId = -1;
            return;
        }
        if (session.id() == previewedId) {
            return;
        }
        previewedId = session.id();
        try {
            StoredSession full = repository.loadSession(session.id());
            if (full == null) {
                return;
            }
            BufferedImage image = full.image();
            preview.setImage(image != null ? SwingFXUtils.toFXImage(image, null) : null);
            details.setText("Image : " + full.imageName()
                    + "\nDimensions : " + full.imageWidth() + " × " + full.imageHeight() + " px"
                    + "\nCalibration : " + full.parameters().getPixelSizeMicrons() + " µm/px"
                    + "\nSeuil : " + full.thresholdUsed()
                    + "\nParticules : " + full.particleCount()
                    + "\nD10 / D50 / D90 : " + round(full.d10()) + " / " + round(full.d50())
                    + " / " + round(full.d90()) + " " + full.parameters().unit()
                    + "\nCommentaire : " + (full.comment() == null || full.comment().isBlank()
                    ? "—" : full.comment()));
        } catch (RuntimeException e) {
            details.setText("Aperçu indisponible : " + e.getMessage());
        }
    }

    private String round(double value) {
        return String.format(java.util.Locale.FRANCE, "%.2f", value);
    }

    private void openSelected() {
        StoredSession selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.accept("Sélectionnez d'abord une session dans la liste.");
            return;
        }
        try {
            StoredSession full = repository.loadSession(selected.id());
            if (full == null || full.particles() == null) {
                refresh();
                return;
            }
            AnalysisResult result = AnalysisResult.fromStored(
                    new File(full.imageName() == null ? "session-" + full.id() : full.imageName()),
                    full.image(),
                    full.parameters(),
                    full.thresholdUsed(),
                    full.particles(),
                    full.imageWidth(),
                    full.imageHeight());
            onOpen.accept(result);
            status.accept("Session #" + full.id() + " chargée (" + full.particles().size()
                    + " particules).");
        } catch (RuntimeException e) {
            error("Chargement impossible", e.getMessage());
        }
    }

    private void deleteSelected() {
        StoredSession selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.accept("Sélectionnez d'abord une session dans la liste.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer la session");
        confirmation.setHeaderText("Supprimer définitivement la session #" + selected.id() + " ?");
        confirmation.setContentText("L'image et les particules enregistrées seront effacées.");
        confirmation.showAndWait().filter(button -> button == ButtonType.OK).ifPresent(button -> {
            try {
                repository.deleteSession(selected.id());
                previewedId = -1;
                preview.setImage(null);
                refresh();
                status.accept("Session #" + selected.id() + " supprimée.");
            } catch (RuntimeException e) {
                error("Suppression impossible", e.getMessage());
            }
        });
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Granulométrie Studio");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
