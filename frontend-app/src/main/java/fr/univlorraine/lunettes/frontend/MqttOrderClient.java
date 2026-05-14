package fr.univlorraine.lunettes.frontend;

import fr.univlorraine.lunettes.common.codec.PayloadCodec;
import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.OrderRequest;
import fr.univlorraine.lunettes.common.model.OrderStatus;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import fr.univlorraine.lunettes.common.mqtt.TopicNames;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttOrderClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttOrderClient.class);

    private final MqttClient client;
    private final Map<String, Consumer<String>> handlers;

    public MqttOrderClient(FrontendConfig config) throws MqttException {
        this.client = new MqttClient(config.brokerUrl(), config.clientPrefix() + "-" + UUID.randomUUID());
        this.handlers = new ConcurrentHashMap<>();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.setCallback(new UiMqttCallback());
        try {
            client.connect(options);
            LOGGER.info("Frontend connecte au broker {}", config.brokerUrl());
        } catch (MqttException exception) {
            LOGGER.warn("MQTT indisponible au demarrage: {}", exception.getMessage());
        }
    }

    public String submitOrder(Map<GlassType, Integer> quantities, OrderListener listener) {
        String orderId = UUID.randomUUID().toString();
        handlers.put(TopicNames.orderValidated(orderId), payload -> listener.onValidated());
        handlers.put(TopicNames.orderCancelled(orderId), payload -> listener.onCancelled(PayloadCodec.decodeText(payload)));
        handlers.put(TopicNames.orderError(orderId), payload -> listener.onError(PayloadCodec.decodeText(payload)));
        handlers.put(TopicNames.orderStatus(orderId), payload -> listener.onStatusChanged(PayloadCodec.decodeStatus(payload)));
        handlers.put(TopicNames.orderDelivery(orderId), payload -> listener.onDelivered(PayloadCodec.decodeDelivery(payload)));

        try {
            client.subscribe(TopicNames.orderSubscription(orderId), 1);
            OrderRequest request = new OrderRequest(orderId, quantities);
            client.publish(TopicNames.order(orderId), new MqttMessage(PayloadCodec.encodeOrder(request).getBytes(StandardCharsets.UTF_8)));
            LOGGER.info("Commande {} envoyee ({} types de lunettes)", orderId, quantities.size());
            return orderId;
        } catch (MqttException exception) {
            throw new IllegalStateException("Impossible d'envoyer la commande", exception);
        }
    }

    public CompletableFuture<String> checkSerial(String serial) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String replyTopic = TopicNames.serialResult(serial);
        handlers.put(replyTopic, payload -> {
            future.complete(payload);
            handlers.remove(replyTopic);
            try {
                client.unsubscribe(replyTopic);
            } catch (MqttException ignored) {
            }
        });
        try {
            client.subscribe(replyTopic, 1);
            client.publish(TopicNames.serialCheck(serial), new MqttMessage(new byte[0]));
            LOGGER.info("Verification serie {} demandee", serial);
        } catch (MqttException exception) {
            future.completeExceptionally(exception);
        }
        return future;
    }

    @Override
    public void close() throws Exception {
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
    }

    private final class UiMqttCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            LOGGER.warn("Connexion MQTT frontend perdue", cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            LOGGER.debug("Message frontend recu sur {}", topic);
            Consumer<String> handler = handlers.get(topic);
            if (handler != null) {
                handler.accept(new String(message.getPayload(), StandardCharsets.UTF_8));
            } else {
                LOGGER.warn("Aucun handler frontend pour le topic {}", topic);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    public interface OrderListener {
        void onValidated();
        void onCancelled(String reason);
        void onError(String reason);
        void onStatusChanged(OrderStatus status);
        void onDelivered(List<ProducedGlass> delivery);
    }
}
