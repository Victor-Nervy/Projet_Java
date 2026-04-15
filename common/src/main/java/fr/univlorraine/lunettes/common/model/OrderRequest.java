package fr.univlorraine.lunettes.common.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record OrderRequest(String orderId, Map<GlassType, Integer> quantities) {

    public OrderRequest {
        Map<GlassType, Integer> safeCopy = new EnumMap<>(GlassType.class);
        if (quantities != null) {
            safeCopy.putAll(quantities);
        }
        quantities = Collections.unmodifiableMap(safeCopy);
    }

    public int totalQuantity() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }
}
