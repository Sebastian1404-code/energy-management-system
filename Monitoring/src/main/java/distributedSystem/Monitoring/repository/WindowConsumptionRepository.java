// src/main/java/distributedSystem/Monitoring/repository/WindowConsumptionRepository.java
package distributedSystem.Monitoring.repository;

import distributedSystem.Monitoring.model.WindowConsumption;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    Optional<WindowConsumption> findTopByDeviceIdOrderByWindowStartUtcDesc(String deviceId);


    @Query(value = """
  SELECT DISTINCT ON (device_id) *
    FROM window_consumption
   ORDER BY device_id, window_start_utc DESC
  """, nativeQuery = true)
    List<WindowConsumption> findLatestWindowPerDeviceNative();

    @Query(value = """
    SELECT wc.window_start_utc, wc.kwh, wc.sample_count, wc.window_minutes
      FROM window_consumption wc
     WHERE wc.device_id = ?1
       AND wc.window_start_utc >= ?2
       AND wc.window_start_utc <  ?3
     ORDER BY wc.window_start_utc
    """, nativeQuery = true)
    List<Object[]> findRawWindowsForDay(String deviceId, Instant startInclusiveUtc, Instant endExclusiveUtc);


}
