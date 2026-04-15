package fr.univlorraine.lunettes.common.model;

public enum OrderStatus {
    PROCESSING("processing"),
    PROCESSED("processed");

    private final String wireValue;

    OrderStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
