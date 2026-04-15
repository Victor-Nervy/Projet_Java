package fr.univlorraine.lunettes.common.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum GlassType {
    BANANA("banana", "Bananaaaa"),
    CHATGPT("chatgpt", "BlaBlaBla"),
    LE_CHAT("le_chat", "Miaousse"),
    CLAUDE("claude", "Claude");

    private final String productId;
    private final String displayName;

    GlassType(String productId, String displayName) {
        this.productId = productId;
        this.displayName = displayName;
    }

    public String productId() {
        return productId;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<GlassType> fromProductId(String productId) {
        return Arrays.stream(values())
            .filter(type -> type.productId.equalsIgnoreCase(productId))
            .findFirst();
    }

    public static Optional<GlassType> fromWireName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(type -> type.name().equals(normalized))
            .findFirst();
    }
}
