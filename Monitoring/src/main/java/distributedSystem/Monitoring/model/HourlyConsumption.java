package distributedSystem.Monitoring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "hourly_consumption",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "hour_start_utc"})
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HourlyConsumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "hour_start_utc", nullable = false)
    private Instant hourStartUtc;

    @Column(name = "kwh", nullable = false)
    private double kwh;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc;



}
