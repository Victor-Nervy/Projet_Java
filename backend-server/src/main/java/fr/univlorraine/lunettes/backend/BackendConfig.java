package fr.univlorraine.lunettes.backend;

import fr.univlorraine.lunettes.common.config.PropertiesLoader;
import java.nio.file.Path;
import java.util.Properties;

public record BackendConfig(String brokerUrl, String clientId, int factoryCapacity) {

    public static BackendConfig load(Path overridePath) {
        Properties properties = PropertiesLoader.load("/backend.properties", overridePath);
        return new BackendConfig(
            properties.getProperty("broker.url", "tcp://localhost:1883"),
            properties.getProperty("backend.clientId", "lunettes-backend"),
            Integer.parseInt(properties.getProperty("backend.factoryCapacity", "4"))
        );
    }
}
