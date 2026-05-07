package fr.univlorraine.lunettes.frontend;

import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.OrderStatus;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class FrontendApplication extends Application {

    private static final String APP_NAME = "Optique Premium";
    private static final String PURPLE_BACKGROUND = "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2);";
    private static final String DARK = "#2f3437";
    private static final String TEXT = "#39424e";
    private static final String PRIMARY = "#5f7fdd";
    private static final String CARD = "-fx-background-color: white; -fx-background-radius: 8;";

    private Stage stage;
    private MqttOrderClient mqttOrderClient;
    private List<ProductDefinition> products;
    private final Map<String, Integer> selectedQuantities = new HashMap<>();
    private final Label orderStateLabel = new Label("Aucune commande en cours");
    private final VBox deliveryBox = new VBox(8);
    private final Label serialCheckResultLabel = new Label("Saisissez un numéro de série puis vérifiez-le.");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        Path configPath = getParameters().getUnnamed().isEmpty() ? null : Path.of(getParameters().getUnnamed().getFirst());
        this.products = ProductCatalog.load();
        this.mqttOrderClient = new MqttOrderClient(FrontendConfig.load(configPath));

        stage.setTitle(APP_NAME);
        showHome();
        stage.show();
    }

    private void showHome() {
        VBox hero = new VBox(20);
        hero.setAlignment(Pos.CENTER);
        hero.setMaxWidth(560);
        hero.setPadding(new Insets(52, 44, 56, 44));
        hero.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-background-radius: 8;");

        Label icon = new Label("∞");
        icon.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 110));
        icon.setStyle("-fx-text-fill: white;");

        Label title = new Label("Vision Parfaite");
        title.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 40));
        title.setStyle("-fx-text-fill: white;");

        Label subtitle = new Label("Découvrez notre collection exclusive de lunettes premium");
        subtitle.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
        subtitle.setStyle("-fx-text-fill: white;");
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        Button orderButton = pillButton("🛒 Commander", true);
        orderButton.setOnAction(event -> showCatalog());

        hero.getChildren().addAll(icon, title, subtitle, orderButton);

        StackPane content = new StackPane(hero);
        content.setPadding(new Insets(55, 24, 75, 24));
        content.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(wrapStorePage(content, true), 1100, 760));
    }

    private void showCatalog() {
        stage.setScene(new Scene(wrapStorePage(buildCatalogView(), true), 1100, 760));
    }

    private BorderPane wrapStorePage(Node center, boolean darkHeader) {
        BorderPane root = new BorderPane();
        root.setStyle(PURPLE_BACKGROUND);

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 34, 14, 34));
        header.setStyle("-fx-background-color: " + (darkHeader ? DARK : "white") + ";");

        Label brand = new Label("∞ " + APP_NAME);
        brand.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 22));
        brand.setStyle("-fx-text-fill: " + (darkHeader ? "white" : TEXT) + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button home = navButton("Accueil", darkHeader);
        home.setOnAction(event -> showHome());
        Button catalog = navButton("Commander", darkHeader);
        catalog.setOnAction(event -> showCatalog());
        Button tracking = navButton("Suivi", darkHeader);
        tracking.setOnAction(event -> showTracking());
        Button serials = navButton("Authenticité", darkHeader);
        serials.setOnAction(event -> showSerialCheck());

        header.getChildren().addAll(brand, spacer, home, catalog, tracking, serials);

        VBox footer = new VBox(8);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(18, 24, 18, 24));
        footer.setStyle("-fx-background-color: " + DARK + ";");
        Label copyright = new Label("© 2024 Optique Premium - Tous droits réservés");
        Label links = new Label("Mentions Légales    CGV    Contact");
        copyright.setStyle("-fx-text-fill: white;");
        links.setStyle("-fx-text-fill: white;");
        footer.getChildren().addAll(copyright, links);

        root.setTop(header);
        root.setCenter(center);
        root.setBottom(footer);
        return root;
    }

    private Node buildCatalogView() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(30, 45, 45, 45));
        root.setAlignment(Pos.TOP_CENTER);

        FlowPane cards = new FlowPane();
        cards.setAlignment(Pos.CENTER);
        cards.setHgap(18);
        cards.setVgap(18);
        cards.setPrefWrapLength(1010);

        products.stream()
            .map(this::createCard)
            .forEach(cards.getChildren()::add);

        Button orderButton = pillButton("🛒 Passer commande", false);
        orderButton.setMaxWidth(Double.MAX_VALUE);
        orderButton.setOnAction(event -> placeOrder());

        root.getChildren().addAll(cards, orderButton);
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private VBox createCard(ProductDefinition product) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(14));
        card.setPrefWidth(230);
        card.setMinHeight(390);
        card.setStyle(CARD + " -fx-border-color: rgba(0,0,0,0.07); -fx-border-radius: 8;");

        StackPane imageArea = new StackPane();
        imageArea.setPrefSize(202, 165);
        imageArea.setStyle("-fx-background-color: #f8fbff; -fx-background-radius: 8;");

        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/assets/" + product.id() + ".png")));
        imageView.setFitWidth(190);
        imageView.setFitHeight(145);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageArea.getChildren().add(imageView);

        if (!product.badge().isBlank()) {
            Label badge = new Label(product.badge());
            badge.setStyle("-fx-background-color: #df6464; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 10;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(8));
            imageArea.getChildren().add(badge);
        }

        Label name = new Label(product.name());
        name.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 18));
        name.setStyle("-fx-text-fill: " + TEXT + ";");

        Label description = new Label(product.description());
        description.setWrapText(true);
        description.setMinHeight(58);
        description.setStyle("-fx-text-fill: #667085;");

        Label price = new Label(String.format("%.2f €", product.price()).replace('.', ','));
        price.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 22));
        price.setStyle("-fx-text-fill: " + PRIMARY + ";");
        price.setMaxWidth(Double.MAX_VALUE);
        price.setAlignment(Pos.CENTER_RIGHT);

        Label quantity = new Label("Quantité :");
        quantity.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        quantity.setStyle("-fx-text-fill: " + TEXT + ";");
        Spinner<Integer> spinner = new Spinner<>(0, 9, selectedQuantities.getOrDefault(product.id(), 0));
        spinner.setPrefWidth(92);
        spinner.valueProperty().addListener((observable, oldValue, newValue) -> selectedQuantities.put(product.id(), newValue));
        HBox quantityLine = new HBox(10, quantity, spinner);
        quantityLine.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(imageArea, name, description, price, quantityLine);
        return card;
    }

    private void placeOrder() {
        Map<GlassType, Integer> quantities = new EnumMap<>(GlassType.class);
        selectedQuantities.forEach((productId, quantity) -> GlassType.fromProductId(productId)
            .ifPresent(type -> {
                if (quantity != null && quantity > 0) {
                    quantities.put(type, quantity);
                }
            }));

        if (quantities.isEmpty()) {
            orderStateLabel.setText("Choisissez au moins une paire avant d'envoyer la commande.");
            deliveryBox.getChildren().clear();
            showTracking();
            return;
        }

        deliveryBox.getChildren().clear();
        orderStateLabel.setText("Envoi de la commande...");
        showTracking();

        String orderId;
        try {
            orderId = mqttOrderClient.submitOrder(quantities, new MqttOrderClient.OrderListener() {
                @Override
                public void onValidated() {
                    Platform.runLater(() -> {
                        orderStateLabel.setText("Commande validée, fabrication planifiée.");
                        showTracking();
                    });
                }

                @Override
                public void onCancelled(String reason) {
                    Platform.runLater(() -> {
                        orderStateLabel.setText("Commande annulée : " + reason);
                        showTracking();
                    });
                }

                @Override
                public void onError(String reason) {
                    Platform.runLater(() -> {
                        orderStateLabel.setText("Erreur : " + reason);
                        showTracking();
                    });
                }

                @Override
                public void onStatusChanged(OrderStatus status) {
                    Platform.runLater(() -> {
                        orderStateLabel.setText(status == OrderStatus.PROCESSING ? "Fabrication en cours..." : "Fabrication terminée");
                        showTracking();
                    });
                }

                @Override
                public void onDelivered(List<ProducedGlass> delivery) {
                    Platform.runLater(() -> {
                        orderStateLabel.setText("Livraison reçue");
                        displayDelivery(delivery);
                        showOrderSuccess(delivery);
                    });
                }
            });
        } catch (IllegalStateException exception) {
            orderStateLabel.setText("Impossible d'envoyer la commande : " + rootMessage(exception));
            showTracking();
            return;
        }

        orderStateLabel.setText("Commande n° " + shortOrderId(orderId) + " envoyée au backend...");
        showTracking();

        scheduler.schedule(() -> Platform.runLater(() -> {
            if (orderStateLabel.getText().contains("envoyée au backend")) {
                orderStateLabel.setText("Aucune réponse du backend pour la commande " + shortOrderId(orderId) + ". Vérifiez que le backend est lancé.");
                showTracking();
            }
        }), 10, TimeUnit.SECONDS);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private void displayDelivery(List<ProducedGlass> delivery) {
        deliveryBox.getChildren().clear();
        for (ProducedGlass producedGlass : delivery) {
            deliveryBox.getChildren().add(new Label(producedGlass.type().displayName() + "  •  " + producedGlass.serial()));
        }
    }

    private void showTracking() {
        VBox panel = new VBox(18);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(620);
        panel.setPadding(new Insets(36));
        panel.setStyle(CARD + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 22, 0, 0, 8);");

        Label spinnerIcon = new Label("⌛");
        spinnerIcon.setFont(Font.font("Verdana", FontWeight.BOLD, 44));
        spinnerIcon.setStyle("-fx-text-fill: " + PRIMARY + ";");

        Label title = new Label("Suivi de commande");
        title.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 30));
        title.setStyle("-fx-text-fill: " + TEXT + ";");

        orderStateLabel.setWrapText(true);
        orderStateLabel.setAlignment(Pos.CENTER);
        orderStateLabel.setStyle("-fx-text-fill: #667085; -fx-font-weight: bold;");

        deliveryBox.setPadding(new Insets(16));
        deliveryBox.setMaxWidth(520);
        deliveryBox.setStyle("-fx-background-color: #f5f7fb; -fx-background-radius: 8;");
        panel.getChildren().addAll(spinnerIcon, title, orderStateLabel, deliveryBox);

        StackPane content = new StackPane(panel);
        content.setPadding(new Insets(45, 24, 60, 24));
        stage.setScene(new Scene(wrapStorePage(content, true), 1100, 760));
    }

    private void showOrderSuccess(List<ProducedGlass> delivery) {
        VBox panel = new VBox(18);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(430);
        panel.setPadding(new Insets(42, 36, 42, 36));
        panel.setStyle("-fx-background-color: linear-gradient(to bottom, #5fe56b, #46d95f); -fx-background-radius: 2;");

        Label check = new Label("✓");
        check.setAlignment(Pos.CENTER);
        check.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 38));
        check.setStyle("-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 999; -fx-min-width: 70; -fx-min-height: 70;");

        Label title = new Label("Commande reçue !");
        title.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 30));
        title.setStyle("-fx-text-fill: white;");

        Label subtitle = new Label("Merci pour votre achat.");
        subtitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        VBox serials = new VBox(7);
        serials.setAlignment(Pos.CENTER);
        serials.setPadding(new Insets(13, 18, 13, 18));
        serials.setStyle("-fx-background-color: rgba(255,255,255,0.16); -fx-background-radius: 999;");
        Label serialTitle = new Label("Commande N° " + delivery.size() + " paire(s)");
        serialTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        serials.getChildren().add(serialTitle);
        delivery.stream()
            .map(glass -> {
                Label label = new Label(glass.serial());
                label.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                return label;
            })
            .forEach(serials.getChildren()::add);

        panel.getChildren().addAll(check, title, subtitle, serials);

        Button home = pillButton("🏠 Retour à l'accueil", true);
        home.setOnAction(event -> showHome());

        VBox contentBox = new VBox(26, panel, home);
        contentBox.setAlignment(Pos.CENTER);
        StackPane content = new StackPane(contentBox);
        content.setPadding(new Insets(42, 24, 58, 24));
        stage.setScene(new Scene(wrapStorePage(content, true), 1100, 760));
    }

    private void showSerialCheck() {
        serialCheckResultLabel.setText("Saisissez un numéro de série puis vérifiez-le.");
        serialCheckResultLabel.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-text-fill: #667085;");

        VBox page = new VBox(24);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(28, 24, 55, 24));
        page.setStyle("-fx-background-color: #f8fbff;");

        HBox reassurance = new HBox(120);
        reassurance.setAlignment(Pos.CENTER);
        reassurance.setPadding(new Insets(6, 0, 16, 0));
        reassurance.getChildren().addAll(
            mutedLabel("Livraison offerte sur les commandes de plus de 50€"),
            mutedLabel("Protection complète sur tous nos produits"),
            mutedLabel("Satisfait ou remboursé sans condition")
        );

        Label title = new Label("Vérification d'Authenticité");
        title.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 32));
        title.setStyle("-fx-text-fill: " + TEXT + ";");

        Label subtitle = mutedLabel("Assurez-vous de l'authenticité de vos lunettes avec notre système de vérification");

        VBox form = new VBox(16);
        form.setMaxWidth(470);
        form.setPadding(new Insets(32));
        form.setStyle("-fx-background-color: #e8f4f8; -fx-background-radius: 8;");

        Label scan = new Label("▦");
        scan.setAlignment(Pos.CENTER);
        scan.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 30));
        scan.setStyle("-fx-text-fill: white; -fx-background-color: #5c7186; -fx-background-radius: 999; -fx-min-width: 60; -fx-min-height: 60;");

        Label serialLabel = new Label("#  Numéro de Série");
        serialLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        serialLabel.setStyle("-fx-text-fill: " + TEXT + ";");

        TextField serialField = new TextField();
        serialField.setPromptText("Ex: BA-9RK7M3-B325B383");
        serialField.setStyle("-fx-background-radius: 6; -fx-border-color: transparent; -fx-padding: 12;");

        Label format = mutedLabel("Format: XX-XXXX-XXXXXXXX (Ex: LX-2024-AB123456)");

        Button verifyButton = pillButton("🔍 Vérifier l'Authenticité", false);
        verifyButton.setMaxWidth(Double.MAX_VALUE);
        verifyButton.setOnAction(event -> verifySerial(serialField.getText().trim()));

        serialCheckResultLabel.setWrapText(true);
        serialCheckResultLabel.setMaxWidth(Double.MAX_VALUE);
        serialCheckResultLabel.setPadding(new Insets(14));

        VBox help = new VBox(4);
        help.setPadding(new Insets(14));
        help.setStyle("-fx-background-color: rgba(255,255,255,0.45); -fx-background-radius: 6; -fx-border-color: #6ea8ff; -fx-border-width: 0 0 0 4;");
        Label helpTitle = new Label("ⓘ Où trouver votre numéro de série ?");
        helpTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        helpTitle.setStyle("-fx-text-fill: " + TEXT + ";");
        Label helpText = mutedLabel("Le numéro de série se trouve sur la branche intérieure droite de vos lunettes ou sur le certificat fourni lors de l'achat.");
        helpText.setWrapText(true);
        help.getChildren().addAll(helpTitle, helpText);

        form.getChildren().addAll(scan, serialLabel, serialField, format, verifyButton, serialCheckResultLabel, help);
        form.setAlignment(Pos.CENTER);

        page.getChildren().addAll(reassurance, title, subtitle, form);
        stage.setScene(new Scene(wrapStorePage(page, false), 1100, 760));
    }

    private void verifySerial(String serial) {
        if (serial.isBlank()) {
            showSerialResult("⚠ Numéro invalide\nVeuillez saisir un numéro de série.", false);
            return;
        }
        serialCheckResultLabel.setText("Vérification en cours...");
        serialCheckResultLabel.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-text-fill: #667085;");
        mqttOrderClient.checkSerial(serial)
            .thenAccept(result -> Platform.runLater(() -> showSerialResult(
                "invalid".equalsIgnoreCase(result)
                    ? "⚠ Numéro invalide\nLe numéro de série " + serial + " n'est pas valide."
                    : "✓ Produit Authentique\nLe numéro de série " + serial + " est valide. Type : " + result,
                !"invalid".equalsIgnoreCase(result)
            )))
            .exceptionally(error -> {
                Platform.runLater(() -> showSerialResult("⚠ Erreur de vérification\n" + rootMessage(error), false));
                return null;
            });
    }

    private void showSerialResult(String message, boolean valid) {
        serialCheckResultLabel.setText(message);
        serialCheckResultLabel.setStyle("-fx-background-color: " + (valid ? "#9df4e9" : "#ffe0da") + "; -fx-background-radius: 6; -fx-text-fill: " + TEXT + "; -fx-font-weight: bold;");
    }

    private Button navButton(String text, boolean darkHeader) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: " + (darkHeader ? "white" : TEXT) + "; -fx-font-weight: bold; -fx-cursor: hand;");
        return button;
    }

    private Button pillButton(String text, boolean light) {
        Button button = new Button(text);
        button.setPadding(new Insets(12, 28, 12, 28));
        button.setStyle("-fx-background-color: " + (light ? "white" : "#75a9f8") + "; -fx-text-fill: " + (light ? PRIMARY : "white") + "; -fx-font-weight: bold; -fx-background-radius: 999; -fx-cursor: hand;");
        return button;
    }

    private Label mutedLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #667085;");
        return label;
    }

    private String shortOrderId(String orderId) {
        return orderId.length() <= 8 ? orderId : orderId.substring(0, 8);
    }

    @Override
    public void stop() throws Exception {
        scheduler.shutdownNow();
        if (mqttOrderClient != null) {
            mqttOrderClient.close();
        }
    }
}
