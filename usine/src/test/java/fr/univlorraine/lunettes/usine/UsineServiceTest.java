package fr.univlorraine.lunettes.usine;

import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UsineServiceTest {

    @Test
    void shouldProduceRequestedAmount() {
        try (UsineService usineService = new UsineService(4)) {
            List<ProducedGlass> glasses = usineService.produire(Map.of(
                GlassType.BANANA, 2,
                GlassType.CLAUDE, 1
            ));

            assertEquals(3, glasses.size());
            glasses.forEach(glass -> assertNotNull(glass.serial()));
        }
    }
}
