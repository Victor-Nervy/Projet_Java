package fr.univlorraine.lunettes.frontend;

import fr.univlorraine.lunettes.common.config.PropertiesLoader;
import java.nio.file.Path;
import java.util.Properties;

public record FrontendConfig(String brokerUrl, String clientPrefix) {

    public static FrontendConfig load(Path overridePath) {
        Properties properties = PropertiesLoader.load("/frontend.properties", overridePath);
        return new FrontendConfig(
            properties.getProperty("broker.url", "tcp://localhost:1883"),
            properties.getProperty("frontend.clientPrefix", "lunettes-frontend")
        );
    }
}
