package distributedSystem.DeviceManagement.service;

import distributedSystem.DeviceManagement.model.DeviceUserRef;
import distributedSystem.DeviceManagement.repository.DeviceUserRefRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSyncService {

    private final DeviceUserRefRepository repo;

    public UserSyncService(DeviceUserRefRepository repo) {
        this.repo = repo;
    }

    public void handleUserCreated(Long userId) {
        repo.findById(userId).orElseGet(() -> repo.save(DeviceUserRef.builder().userId(userId).build()));
    }

}
