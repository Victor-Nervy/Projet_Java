package fr.univlorraine.lunettes.backend;

import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.OrderRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderValidatorTest {

    @Test
    void shouldRejectEmptyOrder() {
        OrderRequest request = new OrderRequest("order-1", Map.of());
        assertTrue(OrderValidator.validate(request).isPresent());
    }

    @Test
    void shouldAcceptValidOrder() {
        OrderRequest request = new OrderRequest("order-2", Map.of(GlassType.BANANA, 2));
        assertTrue(OrderValidator.validate(request).isEmpty());
    }
}
