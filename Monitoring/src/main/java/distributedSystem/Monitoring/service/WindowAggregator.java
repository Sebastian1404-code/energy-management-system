// src/main/java/distributedSystem/Monitoring/service/WindowAggregator.java
package distributedSystem.Monitoring.service;

import distributedSystem.Monitoring.repository.WindowConsumptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WindowAggregator {

    public record Key(String deviceId, Instant windowStartUtc) {}
    public static final class Bucket {
        volatile double kwh;
        volatile int samples;
        Bucket(double kwh, int samples) { this.kwh = kwh; this.samples = samples; }
        void add(double v) { this.kwh += v; this.samples += 1; }
    }

    private final WindowConsumptionRepository repo;
    private final Map<Key, Bucket> buf = new ConcurrentHashMap<>();
    private final long flushSeconds;
    private final int windowMinutes;
    private final boolean useProcessingTime;

    public WindowAggregator(
            WindowConsumptionRepository repo,
            @Value("${app.flush-seconds:5}") long flushSeconds,
            @Value("${app.aggregate-minutes:60}") int windowMinutes,
            @Value("${app.window.use-processing-time:false}") boolean useProcessingTime
    ) {
        this.repo = repo;
        this.flushSeconds = flushSeconds;
        this.windowMinutes = Math.max(1, windowMinutes);
        this.useProcessingTime = useProcessingTime;
    }

    private Instant windowStart(Instant basis) {
        long epochSec = basis.getEpochSecond();
        long sizeSec = windowMinutes * 60L;
        long start = (epochSec / sizeSec) * sizeSec;
        return Instant.ofEpochSecond(start);
    }

    public void add(String deviceId, Instant eventTimestampUtc, double valueKwh) {
        Instant basis = useProcessingTime ? Instant.now() : eventTimestampUtc;
        Instant win = windowStart(basis);
        Key key = new Key(deviceId, win);
        buf.compute(key, (k, b) -> {
            if (b == null) {
                return new Bucket(valueKwh, 1);
            }
            b.add(valueKwh);
            return b;
        });

    }

    @Scheduled(fixedDelayString = "#{${app.flush-seconds:5} * 1000}")
    @Transactional               // ← add this (or on the repo method, but I like both)
    public void flush() {
        if (buf.isEmpty()) return;
        Map<Key, Bucket> snapshot = new ConcurrentHashMap<>(buf);
        buf.clear();
        Instant now = Instant.now();
        snapshot.forEach((k, b) ->
                repo.upsertAdd(k.deviceId(), k.windowStartUtc(), windowMinutes, b.kwh, b.samples, now)
        );
    }
}
