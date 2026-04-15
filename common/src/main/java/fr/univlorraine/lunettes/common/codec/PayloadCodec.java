package fr.univlorraine.lunettes.common.codec;

import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.OrderRequest;
import fr.univlorraine.lunettes.common.model.OrderStatus;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class PayloadCodec {

    private static final char ENTRY_SEPARATOR = ';';
    private static final char FIELD_SEPARATOR = '=';
    private static final char LIST_SEPARATOR = ',';
    private static final char ESCAPE = '\\';

    private PayloadCodec() {
    }

    public static String encodeOrder(OrderRequest request) {
        StringJoiner joiner = new StringJoiner(String.valueOf(ENTRY_SEPARATOR));
        request.quantities().forEach((type, quantity) ->
            joiner.add(escape(type.name()) + FIELD_SEPARATOR + quantity)
        );
        return joiner.toString();
    }

    public static OrderRequest decodeOrder(String orderId, String payload) {
        Map<GlassType, Integer> quantities = new EnumMap<>(GlassType.class);
        if (payload == null || payload.isBlank()) {
            return new OrderRequest(orderId, quantities);
        }
        for (String entry : split(payload, ENTRY_SEPARATOR)) {
            List<String> fields = split(entry, FIELD_SEPARATOR);
            if (fields.size() != 2) {
                throw new ProtocolException("Commande invalide: " + entry);
            }
            GlassType type = GlassType.fromWireName(unescape(fields.get(0)))
                .orElseThrow(() -> new ProtocolException("Type inconnu: " + fields.get(0)));
            try {
                quantities.put(type, Integer.parseInt(unescape(fields.get(1))));
            } catch (NumberFormatException exception) {
                throw new ProtocolException("Quantite invalide: " + fields.get(1));
            }
        }
        return new OrderRequest(orderId, quantities);
    }

    public static String encodeDelivery(List<ProducedGlass> producedGlasses) {
        StringJoiner joiner = new StringJoiner(String.valueOf(ENTRY_SEPARATOR));
        for (ProducedGlass producedGlass : producedGlasses) {
            joiner.add(escape(producedGlass.type().name()) + FIELD_SEPARATOR + escape(producedGlass.serial()));
        }
        return joiner.toString();
    }

    public static List<ProducedGlass> decodeDelivery(String payload) {
        List<ProducedGlass> producedGlasses = new ArrayList<>();
        if (payload == null || payload.isBlank()) {
            return producedGlasses;
        }
        for (String entry : split(payload, ENTRY_SEPARATOR)) {
            List<String> fields = split(entry, FIELD_SEPARATOR);
            if (fields.size() != 2) {
                throw new ProtocolException("Livraison invalide: " + entry);
            }
            GlassType type = GlassType.fromWireName(unescape(fields.get(0)))
                .orElseThrow(() -> new ProtocolException("Type de livraison inconnu: " + fields.get(0)));
            producedGlasses.add(new ProducedGlass(type, unescape(fields.get(1))));
        }
        return producedGlasses;
    }

    public static String encodeText(String payload) {
        return escape(payload == null ? "" : payload);
    }

    public static String decodeText(String payload) {
        return unescape(payload == null ? "" : payload);
    }

    public static String encodeStatus(OrderStatus status) {
        return status.wireValue();
    }

    public static OrderStatus decodeStatus(String payload) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.wireValue().equalsIgnoreCase(payload)) {
                return status;
            }
        }
        throw new ProtocolException("Statut inconnu: " + payload);
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (char character : value.toCharArray()) {
            if (character == ESCAPE
                || character == ENTRY_SEPARATOR
                || character == FIELD_SEPARATOR
                || character == LIST_SEPARATOR) {
                builder.append(ESCAPE);
            }
            builder.append(character);
        }
        return builder.toString();
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (char character : value.toCharArray()) {
            if (escaping) {
                builder.append(character);
                escaping = false;
            } else if (character == ESCAPE) {
                escaping = true;
            } else {
                builder.append(character);
            }
        }
        if (escaping) {
            throw new ProtocolException("Fin d'echappement invalide");
        }
        return builder.toString();
    }

    private static List<String> split(String value, char separator) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (char character : value.toCharArray()) {
            if (escaping) {
                current.append(character);
                escaping = false;
            } else if (character == ESCAPE) {
                escaping = true;
            } else if (character == separator) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (escaping) {
            throw new ProtocolException("Fin d'echappement invalide");
        }
        result.add(current.toString());
        return result;
    }
}
