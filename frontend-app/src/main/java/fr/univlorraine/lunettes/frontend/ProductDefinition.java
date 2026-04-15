package fr.univlorraine.lunettes.frontend;

public record ProductDefinition(
    String id,
    String name,
    double price,
    String badge,
    String description
) {
}
