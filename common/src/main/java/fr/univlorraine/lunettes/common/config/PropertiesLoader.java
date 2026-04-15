package fr.univlorraine.lunettes.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesLoader {

    private PropertiesLoader() {
    }

    public static Properties load(String classpathLocation, Path externalPath) {
        Properties properties = new Properties();
        try (InputStream inputStream = PropertiesLoader.class.getResourceAsStream(classpathLocation)) {
            if (inputStream == null) {
                throw new IllegalStateException("Configuration introuvable: " + classpathLocation);
            }
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de lire la configuration embarquee", exception);
        }

        if (externalPath != null && Files.exists(externalPath)) {
            try (InputStream inputStream = Files.newInputStream(externalPath)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                throw new IllegalStateException("Impossible de lire la configuration externe: " + externalPath, exception);
            }
        }
        return properties;
    }
}
