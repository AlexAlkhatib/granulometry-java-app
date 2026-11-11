package fr.granulometrie.ui;

import java.util.List;

import fr.granulometrie.GranulometryApp;
import fr.granulometrie.core.AnalysisResult;
import fr.granulometrie.repo.AnalysisRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;

/** Fenetre principale : en-tete, navigation entre les vues et barre d'etat. */
public final class MainWindow extends BorderPane {

    private final StackPane content = new StackPane();
    private final Label statusLabel = new Label("Prêt");
    private final ProgressBar progressBar = new ProgressBar();
    private final ToggleGroup navigation = new ToggleGroup();
    private final List<String> viewNames = List.of("Analyse", "Résultats", "Historique");

    private final AnalyzeView analyzeView;
    private final ResultView resultView;
    private final HistoryView historyView;
    private final AnalysisRepository repository;

    public MainWindow(Stage stage) {
        getStyleClass().add("app-root");
        this.repository = createRepository();

        HBox header = buildHeader();
        HBox footer = buildFooter();

        analyzeView = new AnalyzeView(this::onAnalysisFinished, this::setStatus, this::setBusy);
        resultView = new ResultView(repository, this::setStatus, () -> select(0));
        historyView = new HistoryView(repository, this::onHistoryLoaded, this::setStatus);

        content.getChildren().addAll(analyzeView, resultView, historyView);
        setTop(header);
        setCenter(content);
        setBottom(footer);
        select(0);

        stage.setOnCloseRequest(event -> {
            if (repository != null) {
                repository.close();
            }
        });
    }

    private AnalysisRepository createRepository() {
        try {
            return new AnalysisRepository();
        } catch (RuntimeException e) {
            statusLabel.setText("Historique indisponible : " + e.getMessage());
            return null;
        }
    }

    private HBox buildHeader() {
        Label title = new Label(GranulometryApp.APP_NAME);
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Analyse granulométrique d'images scientifiques");
        subtitle.getStyleClass().add("app-subtitle");
        VBox titles = new VBox(1, title, subtitle);

        HBox navigationBox = new HBox(6);
        navigationBox.setAlignment(Pos.CENTER_RIGHT);
        for (int i = 0; i < viewNames.size(); i++) {
            ToggleButton button = new ToggleButton(viewNames.get(i));
            button.setToggleGroup(navigation);
            button.getStyleClass().add("nav-button");
            final int index = i;
            button.setOnAction(event -> select(index));
            navigationBox.getChildren().add(button);
        }

        HBox header = new HBox(20, titles, UiFactory.spacer(), navigationBox);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        return header;
    }

    private HBox buildFooter() {
        progressBar.setPrefWidth(160);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        Label hint = new Label("Données conservées localement (base H2) — aucun envoi réseau.");
        hint.getStyleClass().add("footer-hint");
        statusLabel.getStyleClass().add("status-text");

        HBox footer = new HBox(14, statusLabel, progressBar, UiFactory.spacer(), hint);
        footer.getStyleClass().add("status-bar");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 20, 8, 20));
        return footer;
    }

    private void select(int index) {
        List<Node> views = List.of(analyzeView, resultView, historyView);
        for (int i = 0; i < views.size(); i++) {
            Node view = views.get(i);
            view.setVisible(i == index);
            view.setManaged(i == index);
        }
        if (index == 2) {
            historyView.refresh();
        }
        var toggles = navigation.getToggles();
        if (index < toggles.size()) {
            toggles.get(index).setSelected(true);
        }
    }

    private void onAnalysisFinished(AnalysisResult result) {
        resultView.setResult(result);
        select(1);
        setStatus("Analyse terminée : " + result.getParticles().size() + " particules retenues sur "
                + result.getAllParticles().size() + " détectées.");
    }

    private void onHistoryLoaded(AnalysisResult result) {
        resultView.setResult(result);
        select(1);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void setBusy(boolean busy) {
        progressBar.setVisible(busy);
        progressBar.setManaged(busy);
    }
}
