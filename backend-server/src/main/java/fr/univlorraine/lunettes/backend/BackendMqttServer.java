package fr.univlorraine.lunettes.backend;

import fr.univlorraine.lunettes.common.codec.PayloadCodec;
import fr.univlorraine.lunettes.common.codec.ProtocolException;
import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.OrderRequest;
import fr.univlorraine.lunettes.common.model.OrderStatus;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import fr.univlorraine.lunettes.common.mqtt.TopicNames;
import fr.univlorraine.lunettes.usine.UsineService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendMqttServer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendMqttServer.class);

    private final UsineService usineService;
    private final MqttClient client;
    private final ExecutorService orderExecutor;

    public BackendMqttServer(BackendConfig config, UsineService usineService) throws MqttException {
        this.usineService = usineService;
        this.client = new MqttClient(config.brokerUrl(), config.clientId());
        this.orderExecutor = Executors.newCachedThreadPool();
    }

    public void start() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        client.setCallback(new BackendCallback());
        client.connect(options);
        client.subscribe(TopicNames.ordersWildcard(), 1);
        client.subscribe(TopicNames.serialCheckWildcard(), 1);
        LOGGER.info("Backend connecte au broker");
    }

    private void handleOrder(String topic, String payload) {
        String orderId = topic.substring("orders/".length());
        try {
            OrderRequest request = PayloadCodec.decodeOrder(orderId, payload);
            OrderValidator.validate(request).ifPresentOrElse(
                error -> publish(TopicNames.orderCancelled(orderId), PayloadCodec.encodeText(error)),
                () -> {
                    publish(TopicNames.orderValidated(orderId), "");
                    orderExecutor.submit(() -> processOrder(request));
                }
            );
        } catch (ProtocolException exception) {
            publish(TopicNames.orderCancelled(orderId), PayloadCodec.encodeText(exception.getMessage()));
        }
    }

    private void processOrder(OrderRequest request) {
        publish(TopicNames.orderStatus(request.orderId()), PayloadCodec.encodeStatus(OrderStatus.PROCESSING));
        try {
            List<ProducedGlass> delivery = usineService.produire(request.quantities());
            publish(TopicNames.orderStatus(request.orderId()), PayloadCodec.encodeStatus(OrderStatus.PROCESSED));
            publish(TopicNames.orderDelivery(request.orderId()), PayloadCodec.encodeDelivery(delivery));
        } catch (RuntimeException exception) {
            LOGGER.error("Erreur pendant la fabrication de {}", request.orderId(), exception);
            publish(TopicNames.orderError(request.orderId()), PayloadCodec.encodeText(exception.getMessage()));
        }
    }

    private void handleSerialCheck(String topic) {
        String serial = topic.substring("serials/".length(), topic.length() - "/check".length());
        GlassType type = usineService.verifierNumeroSerie(serial);
        publish(TopicNames.serialResult(serial), type == null ? "invalid" : type.name());
    }

    private void publish(String topic, String payload) {
        try {
            client.publish(topic, new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (MqttException exception) {
            throw new IllegalStateException("Impossible de publier sur " + topic, exception);
        }
    }

    @Override
    public void close() throws Exception {
        orderExecutor.shutdownNow();
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
    }

    private final class BackendCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            LOGGER.warn("Connexion MQTT perdue", cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            if (topic.startsWith("orders/") && topic.chars().filter(ch -> ch == '/').count() == 1) {
                handleOrder(topic, payload);
            } else if (topic.startsWith("serials/") && topic.endsWith("/check")) {
                handleSerialCheck(topic);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
}
