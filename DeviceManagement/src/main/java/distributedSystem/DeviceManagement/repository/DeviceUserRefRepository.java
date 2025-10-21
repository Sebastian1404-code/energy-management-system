package distributedSystem.DeviceManagement.repository;

import distributedSystem.DeviceManagement.model.DeviceUserRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceUserRefRepository extends JpaRepository<DeviceUserRef, Long> {
}
