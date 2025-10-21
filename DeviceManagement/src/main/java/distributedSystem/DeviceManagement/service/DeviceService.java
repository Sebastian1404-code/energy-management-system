package distributedSystem.DeviceManagement.service;

import distributedSystem.DeviceManagement.dto.DeviceRequest;
import distributedSystem.DeviceManagement.dto.DeviceResponse;
import distributedSystem.DeviceManagement.model.Device;
import distributedSystem.DeviceManagement.model.DeviceUserRef;
import distributedSystem.DeviceManagement.repository.DeviceRepository;
import distributedSystem.DeviceManagement.repository.DeviceUserRefRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
                        d.getUserRef().getUserId()))
                .toList();
    }

    public DeviceResponse getById(Long id) {
        Device d = deviceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found: " + id));
        return new DeviceResponse(d.getId(), d.getName(), d.getMaximConsumptionValue(), d.getUserRef().getUserId());
    }

    public DeviceResponse create(DeviceRequest req) {
        DeviceUserRef ref = userRefRepo.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown userId: " + req.getUserId())); // prevents FK error
        Device saved = deviceRepo.save(Device.builder()
                .name(req.getName())
                .maximConsumptionValue(req.getMaximConsumptionValue())
                .userRef(ref)
                .build());
        return new DeviceResponse(saved.getId(), saved.getName(), saved.getMaximConsumptionValue(), ref.getUserId());
    }

    public DeviceResponse update(Long id, DeviceRequest req) {
        Device d = deviceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found: " + id));
        d.setName(req.getName());
        d.setMaximConsumptionValue(req.getMaximConsumptionValue());
        if (req.getUserId() != null && !req.getUserId().equals(d.getUserRef().getUserId())) {
            DeviceUserRef ref = userRefRepo.findById(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown userId: " + req.getUserId()));
            d.setUserRef(ref);
        }
        Device saved = deviceRepo.save(d);
        return new DeviceResponse(saved.getId(), saved.getName(), saved.getMaximConsumptionValue(), saved.getUserRef().getUserId());
    }

    public void delete(Long id) {
        deviceRepo.deleteById(id);
    }
}
