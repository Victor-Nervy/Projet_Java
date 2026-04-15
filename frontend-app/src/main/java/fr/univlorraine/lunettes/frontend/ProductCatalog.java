package fr.univlorraine.lunettes.frontend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class ProductCatalog {

    private ProductCatalog() {
    }

    public static List<ProductDefinition> load() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = ProductCatalog.class.getResourceAsStream("/assets/products.json")) {
            if (inputStream == null) {
                throw new IllegalStateException("Catalog assets/products.json introuvable");
            }
            return mapper.readValue(inputStream, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de lire le catalogue de produits", exception);
        }
    }
}
