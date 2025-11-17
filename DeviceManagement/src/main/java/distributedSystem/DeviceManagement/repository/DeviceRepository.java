package distributedSystem.DeviceManagement.repository;

import distributedSystem.DeviceManagement.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByName(String name);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Device d set d.userRef = null where d.userRef.user_id = :userId")
    int detachUserFromDevices(@Param("userId") Long userId);
}


