package bernard_flou;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Fabricateur {

    private static final Map<String, TypeLunette> SERIALS = new ConcurrentHashMap<>();

    private final int capacity;

    public Fabricateur(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacite doit etre positive");
        }
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public void configurer(TypeLunette[] types) {
        if (types.length > capacity) {
            throw new IllegalArgumentException("Lot trop grand pour la capacite de l'usine");
        }
    }

    public Lunette fabriquer(TypeLunette type) {
        String serial = type.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SERIALS.put(serial, type);
        return new Lunette(type, serial);
    }

    public static TypeLunette validateSerial(String serial) {
        return SERIALS.get(serial);
    }

    public enum TypeLunette {
        BANANA,
        CHATGPT,
        LE_CHAT,
        CLAUDE
    }

    public static class Lunette {
        public final TypeLunette type;
        public final String serial;

        public Lunette(TypeLunette type, String serial) {
            this.type = type;
            this.serial = serial;
        }
    }
}
