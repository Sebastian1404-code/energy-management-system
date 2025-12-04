// src/main/java/distributedSystem/Monitoring/service/LastSeenService.java
package distributedSystem.Monitoring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LastSeenService {
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private final boolean useProcessingTime;

    public LastSeenService(@Value("${app.last-seen.use-processing-time:true}") boolean useProcessingTime) {
        this.useProcessingTime = useProcessingTime;
    }

    /** Call this on every consumed reading. */
    public void mark(String deviceId, Instant eventTimestampUtc) {
        lastSeen.put(deviceId, useProcessingTime ? Instant.now() : eventTimestampUtc);
    }

    public Instant get(String deviceId) {
        return lastSeen.get(deviceId);
    }
}
