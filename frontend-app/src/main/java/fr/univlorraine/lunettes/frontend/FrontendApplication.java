package fr.univlorraine.lunettes.frontend;

import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.OrderStatus;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import java.nio.file.Path;
import java.util.Comparator;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class FrontendApplication extends Application {

    private Stage stage;
    private MqttOrderClient mqttOrderClient;
    private List<ProductDefinition> products;
    private final Map<String, Integer> selectedQuantities = new HashMap<>();
    private final Label orderStateLabel = new Label("Aucune commande en cours");
    private final VBox deliveryBox = new VBox(8);
    private final Label serialCheckResultLabel = new Label("Saisis un numero de serie puis verifie-le.");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        Path configPath = getParameters().getUnnamed().isEmpty() ? null : Path.of(getParameters().getUnnamed().getFirst());
        this.products = ProductCatalog.load();
        this.mqttOrderClient = new MqttOrderClient(FrontendConfig.load(configPath));

        stage.setTitle("Lunettes connectees");
        showHome();
        stage.show();
    }

    private void showHome() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Lunettes connectees");
        title.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 34));

        Label subtitle = new Label("Commande, suis la fabrication puis verifie les numeros de serie.");
        subtitle.setWrapText(true);

        Button orderButton = new Button("Commander");
        orderButton.setOnAction(event -> showCatalog());

        Button checkButton = new Button("Verifier un numero de serie");
        checkButton.setOnAction(event -> showSerialCheck());

        content.getChildren().addAll(title, subtitle, orderButton, checkButton);
        stage.setScene(new Scene(wrapWithNav(content), 1100, 760));
    }

    private void showCatalog() {
        stage.setScene(new Scene(wrapWithNav(buildCatalogView()), 1100, 760));
    }

    private BorderPane wrapWithNav(Node center) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        HBox nav = new HBox(12);
        nav.setAlignment(Pos.CENTER_LEFT);

        Button home = new Button("Accueil");
        home.setOnAction(event -> showHome());
        Button catalog = new Button("Catalogue");
        catalog.setOnAction(event -> showCatalog());
        Button tracking = new Button("Suivi");
        tracking.setOnAction(event -> showTracking());
        Button serials = new Button("Verification");
        serials.setOnAction(event -> showSerialCheck());

        nav.getChildren().addAll(home, catalog, tracking, serials);
        root.setTop(nav);
        BorderPane.setMargin(nav, new Insets(0, 0, 16, 0));
        root.setCenter(center);
        return root;
    }

    private Node buildCatalogView() {
        VBox root = new VBox(16);

        Label title = new Label("Choisis tes lunettes");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 28));

        FlowPane cards = new FlowPane();
        cards.setHgap(18);
        cards.setVgap(18);

        products.stream()
            .sorted(Comparator.comparing(ProductDefinition::name))
            .map(this::createCard)
            .forEach(cards.getChildren()::add);

        Button orderButton = new Button("Envoyer la commande");
        orderButton.setOnAction(event -> placeOrder());

        root.getChildren().addAll(title, cards, orderButton);
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private VBox createCard(ProductDefinition product) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setPrefWidth(240);
        card.setStyle("-fx-background-color: linear-gradient(to bottom, #fff8e8, #f6f7fb); -fx-background-radius: 18;");

        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/assets/" + product.id() + ".png")));
        imageView.setFitWidth(210);
        imageView.setFitHeight(170);
        imageView.setPreserveRatio(true);

        Label name = new Label(product.name());
        name.setFont(Font.font("Verdana", FontWeight.BOLD, 18));

        Label badge = new Label(product.badge().isBlank() ? "Collection" : product.badge());
        badge.setStyle("-fx-background-color: #202938; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 999;");

        Label description = new Label(product.description());
        description.setWrapText(true);

        Label price = new Label(String.format("%.2f EUR", product.price()));
        Spinner<Integer> spinner = new Spinner<>(0, 9, selectedQuantities.getOrDefault(product.id(), 0));
        spinner.valueProperty().addListener((observable, oldValue, newValue) -> selectedQuantities.put(product.id(), newValue));

        card.getChildren().addAll(imageView, name, badge, description, price, new Label("Quantite"), spinner);
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
            orderStateLabel.setText("Choisis au moins une paire avant d'envoyer la commande.");
            showTracking();
            return;
        }

        deliveryBox.getChildren().clear();
        orderStateLabel.setText("Commande envoyee au backend...");
        showTracking();

        String orderId = mqttOrderClient.submitOrder(quantities, new MqttOrderClient.OrderListener() {
            @Override
            public void onValidated() {
                Platform.runLater(() -> {
                    orderStateLabel.setText("Commande validee");
                    showTracking();
                });
            }

            @Override
            public void onCancelled(String reason) {
                Platform.runLater(() -> {
                    orderStateLabel.setText("Commande annulee : " + reason);
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
                    orderStateLabel.setText(status == OrderStatus.PROCESSING ? "Fabrication en cours..." : "Fabrication terminee");
                    showTracking();
                });
            }

            @Override
            public void onDelivered(List<ProducedGlass> delivery) {
                Platform.runLater(() -> {
                    orderStateLabel.setText("Livraison recue");
                    displayDelivery(delivery);
                    showTracking();
                });
            }
        });

        scheduler.schedule(() -> Platform.runLater(() -> {
            if (orderStateLabel.getText().equals("Commande envoyee au backend...")) {
                orderStateLabel.setText("Aucune reponse du backend pour la commande " + orderId + ". Verifie que le backend est bien lance.");
                showTracking();
            }
        }), 10, TimeUnit.SECONDS);
    }

    private void displayDelivery(List<ProducedGlass> delivery) {
        deliveryBox.getChildren().clear();
        for (ProducedGlass producedGlass : delivery) {
            deliveryBox.getChildren().add(new Label(producedGlass.type().displayName() + " -> " + producedGlass.serial()));
        }
    }

    private void showTracking() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(24));
        Label title = new Label("Suivi de commande");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 28));
        deliveryBox.setPadding(new Insets(12));
        deliveryBox.setStyle("-fx-background-color: #f5f6fa; -fx-background-radius: 16;");
        content.getChildren().addAll(title, orderStateLabel, deliveryBox);
        stage.setScene(new Scene(wrapWithNav(content), 1100, 760));
    }

    private void showSerialCheck() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));

        Label title = new Label("Verifier un numero de serie");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 28));

        TextField serialField = new TextField();
        serialField.setPromptText("Exemple : CH-ABCD-1234");
        HBox.setHgrow(serialField, Priority.ALWAYS);

        Button verifyButton = new Button("Verifier");
        verifyButton.setOnAction(event -> mqttOrderClient.checkSerial(serialField.getText().trim())
            .thenAccept(result -> Platform.runLater(() ->
                serialCheckResultLabel.setText("Resultat : " + ("invalid".equalsIgnoreCase(result) ? "invalide" : result))
            )));

        HBox actions = new HBox(10, serialField, verifyButton);
        content.getChildren().addAll(title, actions, serialCheckResultLabel);
        stage.setScene(new Scene(wrapWithNav(content), 1100, 760));
    }

    @Override
    public void stop() throws Exception {
        scheduler.shutdownNow();
        if (mqttOrderClient != null) {
            mqttOrderClient.close();
        }
    }
}
