package fr.univlorraine.lunettes.usine;

import bernard_flou.Fabricateur;
import fr.univlorraine.lunettes.common.model.GlassType;
import fr.univlorraine.lunettes.common.model.ProducedGlass;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsineService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsineService.class);

    private final Fabricateur fabricateur;
    private final ExecutorService fabricationExecutor;
    private final Thread batchingThread;
    private final BlockingQueue<ProductionUnit> pendingUnits;
    private volatile boolean running;

    public UsineService(int capacity) {
        this(new Fabricateur(capacity));
    }

    public UsineService(Fabricateur fabricateur) {
        this.fabricateur = fabricateur;
        this.fabricationExecutor = Executors.newFixedThreadPool(fabricateur.getCapacity());
        this.pendingUnits = new LinkedBlockingQueue<>();
        this.running = true;
        this.batchingThread = Thread.ofPlatform().name("usine-batcher").start(this::batchLoop);
    }

    public int capacity() {
        return fabricateur.getCapacity();
    }

    public List<ProducedGlass> produire(final Map<GlassType, Integer> typesLunettes) {
        List<CompletableFuture<ProducedGlass>> futures = new ArrayList<>();
        typesLunettes.forEach((type, quantity) -> {
            for (int index = 0; index < quantity; index++) {
                CompletableFuture<ProducedGlass> future = new CompletableFuture<>();
                futures.add(future);
                pendingUnits.add(new ProductionUnit(type, future));
            }
        });

        List<ProducedGlass> result = new ArrayList<>();
        for (CompletableFuture<ProducedGlass> future : futures) {
            try {
                result.add(future.get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Production interrompue", exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Erreur de fabrication", cause);
            }
        }
        return result;
    }

    public GlassType verifierNumeroSerie(String serial) {
        Fabricateur.TypeLunette typeLunette = Fabricateur.validateSerial(serial);
        return typeLunette == null ? null : GlassType.valueOf(typeLunette.name());
    }

    private void batchLoop() {
        while (running || !pendingUnits.isEmpty()) {
            try {
                ProductionUnit first = pendingUnits.poll(200, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }

                List<ProductionUnit> batch = new ArrayList<>();
                batch.add(first);
                pendingUnits.drainTo(batch, Math.max(0, capacity() - 1));
                runBatch(batch);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException exception) {
                LOGGER.error("Erreur dans la boucle de production", exception);
            }
        }
    }

    private void runBatch(List<ProductionUnit> batch) {
        Fabricateur.TypeLunette[] plannedTypes = batch.stream()
            .map(unit -> Fabricateur.TypeLunette.valueOf(unit.type().name()))
            .toArray(Fabricateur.TypeLunette[]::new);

        fabricateur.configurer(plannedTypes);
        LOGGER.info("Demarrage d'un lot de {} lunettes", batch.size());

        List<CompletableFuture<Void>> jobs = batch.stream()
            .map(unit -> CompletableFuture.runAsync(() -> fabricateOne(unit), fabricationExecutor))
            .toList();

        CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new)).join();
    }

    private void fabricateOne(ProductionUnit unit) {
        try {
            Fabricateur.Lunette lunette = fabricateur.fabriquer(Fabricateur.TypeLunette.valueOf(unit.type().name()));
            unit.future().complete(new ProducedGlass(GlassType.valueOf(lunette.type.name()), lunette.serial));
        } catch (Exception exception) {
            unit.future().completeExceptionally(exception);
        }
    }

    @Override
    public void close() {
        running = false;
        batchingThread.interrupt();
        fabricationExecutor.shutdownNow();
    }

    private record ProductionUnit(GlassType type, CompletableFuture<ProducedGlass> future) {
    }
}
