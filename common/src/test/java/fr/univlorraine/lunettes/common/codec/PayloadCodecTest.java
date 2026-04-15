package fr.univlorraine.lunettes.common.codec;

import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.OrderRequest;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadCodecTest {

    @Test
    void shouldRoundTripOrderPayload() {
        OrderRequest request = new OrderRequest("order-1", Map.of(
            GlassType.BANANA, 2,
            GlassType.CLAUDE, 1
        ));

        String payload = PayloadCodec.encodeOrder(request);
        OrderRequest decoded = PayloadCodec.decodeOrder("order-1", payload);

        assertEquals(request.quantities(), decoded.quantities());
    }

    @Test
    void shouldRoundTripDeliveryPayload() {
        List<ProducedGlass> producedGlasses = List.of(
            new ProducedGlass(GlassType.CHATGPT, "CH-AAAA-FFFF"),
            new ProducedGlass(GlassType.LE_CHAT, "LE-BBBB-EEEE")
        );

        String payload = PayloadCodec.encodeDelivery(producedGlasses);
        List<ProducedGlass> decoded = PayloadCodec.decodeDelivery(payload);

        assertEquals(producedGlasses, decoded);
    }
}
