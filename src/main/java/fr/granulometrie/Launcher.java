package fr.granulometrie;

/**
 * Point d'entree neutre (ne derive pas de {@code Application}).
 *
 * <p>Cette classe permet de lancer l'application depuis un classpath classique
 * (jar + dossier lib) sans le message "JavaFX runtime components are
 * missing" emis par le lanceur Java lorsque la classe principale herite de
 * {@code javafx.application.Application}.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        GranulometryApp.main(args);
    }
}
