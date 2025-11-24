package distributedSystem.Monitoring.repository;


import distributedSystem.Monitoring.model.HourlyConsumption;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, Long> {

    @Modifying
    @Transactional
    @Query(
            value = """
        INSERT INTO hourly_consumption (device_id, hour_start_utc, kwh, sample_count, updated_at_utc)
        VALUES (?1, ?2, ?3, ?4, ?5)
        ON CONFLICT (device_id, hour_start_utc)
        DO UPDATE SET
            kwh = hourly_consumption.kwh + EXCLUDED.kwh,
            sample_count = hourly_consumption.sample_count + EXCLUDED.sample_count,
            updated_at_utc = EXCLUDED.updated_at_utc
        """,
            nativeQuery = true
    )
    void upsertAdd(String deviceId, Instant hourStartUtc, double addKwh, int addSamples, Instant updatedAtUtc);
}
