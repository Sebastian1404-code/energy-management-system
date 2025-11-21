package distributedSystem.DeviceManagement.controller;

import distributedSystem.DeviceManagement.dto.DeviceRequest;
import distributedSystem.DeviceManagement.dto.DeviceResponse;
import distributedSystem.DeviceManagement.dto.UpdateDeviceMetaRequest;
import distributedSystem.DeviceManagement.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @GetMapping
    public List<DeviceResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> create(@RequestBody DeviceRequest req) {
        DeviceResponse saved = service.create(req);
        return ResponseEntity.created(URI.create("/api/devices/" + saved.getId())).body(saved);
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<DeviceResponse> update(
            @PathVariable Long deviceId,
            @RequestBody Long userId) {
        return ResponseEntity.ok(service.update(deviceId, userId));
    }


    @PatchMapping("/{id}/meta")
    public ResponseEntity<DeviceResponse> updateMeta(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDeviceMetaRequest req) {
        return ResponseEntity.ok(service.updateNameAndConsumption(id, req));
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
