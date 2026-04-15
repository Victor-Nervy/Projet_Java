package fr.univlorraine.lunettes.frontend;

import javafx.application.Application;

public final class FrontendLauncher {

    private FrontendLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(FrontendApplication.class, args);
    }
}
