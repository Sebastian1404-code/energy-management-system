// src/main/java/distributedSystem/Monitoring/repository/WindowConsumptionRepository.java
package distributedSystem.Monitoring.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface WindowConsumptionRepository extends CrudRepository<distributedSystem.Monitoring.model.WindowConsumption, Long> {

    @Modifying
    @Transactional               // ← add this
    @Query(value = """
        INSERT INTO window_consumption
          (device_id, window_start_utc, window_minutes, kwh, sample_count, updated_at_utc)
        VALUES
          (?1, ?2, ?3, ?4, ?5, ?6)
        ON CONFLICT (device_id, window_start_utc, window_minutes)
        DO UPDATE SET
          kwh = window_consumption.kwh + EXCLUDED.kwh,
          sample_count = window_consumption.sample_count + EXCLUDED.sample_count,
          updated_at_utc = EXCLUDED.updated_at_utc
        """, nativeQuery = true)
    void upsertAdd(String deviceId, Instant windowStartUtc, int windowMinutes,
                   double deltaKwh, int deltaSamples, Instant nowUtc);
}
