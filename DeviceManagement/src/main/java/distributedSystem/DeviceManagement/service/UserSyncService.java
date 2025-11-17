package distributedSystem.DeviceManagement.service;

import distributedSystem.DeviceManagement.model.DeviceUserRef;
import distributedSystem.DeviceManagement.repository.DeviceRepository;
import distributedSystem.DeviceManagement.repository.DeviceUserRefRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSyncService {

    private final DeviceUserRefRepository repo;
    private final DeviceRepository deviceRepository;

    public UserSyncService(DeviceUserRefRepository repo, DeviceRepository deviceRepository) {
        this.repo = repo;
        this.deviceRepository = deviceRepository;
    }

    public void handleUserCreated(Long userId) {
        repo.findById(userId).orElseGet(() -> repo.save(DeviceUserRef.builder().user_id(userId).build()));
    }

    public void handleUserDeleted(Long userId)
    {
        deviceRepository.detachUserFromDevices(userId);
        repo.findById(userId)
                .ifPresent(repo::delete);
    }

}
