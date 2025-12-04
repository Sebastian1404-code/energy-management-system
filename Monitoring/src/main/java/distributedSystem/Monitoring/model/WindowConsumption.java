// src/main/java/distributedSystem/Monitoring/model/WindowConsumption.java
package distributedSystem.Monitoring.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "window_consumption",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "window_start_utc", "window_minutes"})
)
@Data @AllArgsConstructor @NoArgsConstructor
public class WindowConsumption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "window_start_utc", nullable = false)
    private Instant windowStartUtc;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes;

    @Column(name = "kwh", nullable = false)
    private double kwh;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc;
}
