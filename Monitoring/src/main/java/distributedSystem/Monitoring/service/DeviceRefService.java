package distributedSystem.Monitoring.service;


import distributedSystem.Monitoring.model.DeviceMonitoringRef;
import distributedSystem.Monitoring.repository.DeviceMonitoringRefRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceRefService {
    private final DeviceMonitoringRefRepository deviceMonitoringRefRepository;



    public DeviceRefService(DeviceMonitoringRefRepository deviceMonitoringRef) {
        this.deviceMonitoringRefRepository = deviceMonitoringRef;
    }

    public void handleDeviceCreated(Long deviceId) {
        deviceMonitoringRefRepository.findById(deviceId).orElseGet(() -> deviceMonitoringRefRepository.save(DeviceMonitoringRef.builder().device_id(deviceId).build()));
    }

    public void handleDeviceDeleted(Long deviceId)
    {
        deviceMonitoringRefRepository.findById(deviceId)
                .ifPresent(deviceMonitoringRefRepository::delete);
    }


}
