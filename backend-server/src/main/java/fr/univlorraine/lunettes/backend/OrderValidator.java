package fr.univlorraine.lunettes.backend;

import fr.univlorraine.lunettes.common.model.OrderRequest;
import java.util.Optional;

public final class OrderValidator {

    private OrderValidator() {
    }

    public static Optional<String> validate(OrderRequest request) {
        if (request.totalQuantity() <= 0) {
            return Optional.of("La quantite totale doit etre strictement positive");
        }

        return request.quantities().entrySet().stream()
            .filter(entry -> entry.getValue() < 0 || entry.getValue() >= 10)
            .findFirst()
            .map(entry -> "La quantite pour " + entry.getKey().name() + " doit etre comprise entre 0 et 9");
    }
}
