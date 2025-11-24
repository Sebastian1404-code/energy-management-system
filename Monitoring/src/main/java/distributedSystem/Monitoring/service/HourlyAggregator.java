package distributedSystem.Monitoring.service;

import distributedSystem.Monitoring.repository.HourlyConsumptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// HourlyAggregator.java (rename class if you like to WindowAggregator)
@Service
public class HourlyAggregator {

    public record Key(String deviceId, Instant windowStartUtc) {}
    public static final class Bucket {
        volatile double kwh;
        volatile int samples;
        Bucket(double kwh, int samples) { this.kwh = kwh; this.samples = samples; }
        void add(double v) { this.kwh += v; this.samples += 1; }
    }

    private final HourlyConsumptionRepository repo;
    private final Map<Key, Bucket> buf = new ConcurrentHashMap<>();
    private final long flushSeconds;
    private final int windowMinutes;   // <<< configurable

    public HourlyAggregator(
            HourlyConsumptionRepository repo,
            @Value("${app.flush-seconds:5}") long flushSeconds,
            @Value("${app.aggregate-minutes:60}") int windowMinutes   // <<< add this property
    ) {
        this.repo = repo;
        this.flushSeconds = flushSeconds;
        this.windowMinutes = Math.max(1, windowMinutes);
    }

    private Instant windowStart(Instant ts) {
        long epochSec = ts.getEpochSecond();
        long sizeSec = windowMinutes * 60L;
        long start = (epochSec / sizeSec) * sizeSec;
        return Instant.ofEpochSecond(start);
    }

    public void add(String deviceId, Instant timestampUtc, double valueKwh) {
        Instant win = windowStart(timestampUtc);
        Key key = new Key(deviceId, win);
        buf.compute(key, (k, b) -> {
            if (b == null) return new Bucket(valueKwh, 1);
            b.add(valueKwh);
            return b;
        });
    }

    @Scheduled(fixedDelayString = "#{${app.flush-seconds:5} * 1000}")
    public void flush() {
        if (buf.isEmpty()) return;
        Map<Key, Bucket> snapshot = new ConcurrentHashMap<>(buf);
        buf.clear();

        Instant now = Instant.now();
        snapshot.forEach((k, b) ->
                // Reuse the same UPSERT method; the table/column names can stay "hourly"
                repo.upsertAdd(k.deviceId(), k.windowStartUtc(), b.kwh, b.samples, now)
        );
    }
}
