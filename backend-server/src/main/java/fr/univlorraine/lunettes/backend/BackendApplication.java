package fr.univlorraine.lunettes.backend;

import fr.univlorraine.lunettes.usine.UsineService;
import java.nio.file.Path;

public final class BackendApplication {

    private BackendApplication() {
    }

    public static void main(String[] args) throws Exception {
        Path configPath = args.length > 0 ? Path.of(args[0]) : null;
        BackendConfig config = BackendConfig.load(configPath);

        try (UsineService usineService = new UsineService(config.factoryCapacity());
             BackendMqttServer server = new BackendMqttServer(config, usineService)) {
            server.start();
            Thread.currentThread().join();
        }
    }
}
