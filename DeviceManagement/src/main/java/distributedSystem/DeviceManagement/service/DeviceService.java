package distributedSystem.DeviceManagement.service;

import distributedSystem.DeviceManagement.dto.DeviceRequest;
import distributedSystem.DeviceManagement.dto.DeviceResponse;
import distributedSystem.DeviceManagement.dto.UpdateDeviceMetaRequest;
import distributedSystem.DeviceManagement.model.Device;
import distributedSystem.DeviceManagement.model.DeviceUserRef;
import distributedSystem.DeviceManagement.repository.DeviceRepository;
import distributedSystem.DeviceManagement.repository.DeviceUserRefRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.http.ResponseEntity.notFound;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepo;
    private final DeviceUserRefRepository userRefRepo;

    public DeviceService(DeviceRepository deviceRepo, DeviceUserRefRepository userRefRepo) {
        this.deviceRepo = deviceRepo;
        this.userRefRepo = userRefRepo;
    }

    public List<DeviceResponse> getAll() {
        return deviceRepo.findAll().stream()
                .map(d -> new DeviceResponse(
                        d.getId(), d.getName(), d.getMaximConsumptionValue(),
                        d.getUserRef() != null ? d.getUserRef().getUser_id() : null))
                .toList();
    }

    public DeviceResponse getById(Long id) {
        Device d = deviceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found: " + id));
        return new DeviceResponse(d.getId(), d.getName(), d.getMaximConsumptionValue(), d.getUserRef() != null ? d.getUserRef().getUser_id() : null);
    }

    public DeviceResponse create(DeviceRequest req) {
        DeviceUserRef ref = null;
        if (req.getUserId() != null) {
            ref = userRefRepo.findById(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown userId: " + req.getUserId()));
        }
        Device saved = deviceRepo.save(Device.builder()
                .name(req.getName())
                .maximConsumptionValue(req.getMaximConsumptionValue())
                .userRef(ref)
                .build());
        return new DeviceResponse(saved.getId(), saved.getName(), saved.getMaximConsumptionValue(), ref != null ? ref.getUser_id() : null);
    }



    public DeviceResponse update(Long deviceId, Long userId) {
        Device device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        if (userId != null) {
            DeviceUserRef ref = userRefRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown userId: " + userId));
            device.setUserRef(ref);
        }

        Device saved = deviceRepo.save(device);

        return new DeviceResponse(
                saved.getId(),
                saved.getName(),
                saved.getMaximConsumptionValue(),
                saved.getUserRef() != null ? saved.getUserRef().getUser_id() : null
        );
    }

    public List<DeviceUserRef> getAllUserIds()
    {
        return userRefRepo.findAll();
    }



    @Transactional
    public DeviceResponse updateNameAndConsumption(Long id, UpdateDeviceMetaRequest req) {
        Device device = deviceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found with id: " + id));

        device.setName(req.getName());
        device.setMaximConsumptionValue(req.getMaximConsumptionValue());
        return toResponse(device);
    }


    public void delete(Long id) {
        deviceRepo.deleteById(id);
    }


    private DeviceResponse toResponse(Device d) {
        Long userId = (d.getUserRef() != null) ? d.getUserRef().getUser_id() : null;
        return DeviceResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .maximConsumptionValue(d.getMaximConsumptionValue())
                .userId(userId)
                .build();
    }

}
