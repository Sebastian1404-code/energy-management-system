package distributedSystem.Monitoring.repository;

import distributedSystem.Monitoring.model.DeviceMonitoringRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceMonitoringRefRepository extends JpaRepository<DeviceMonitoringRef, Long> {


}
