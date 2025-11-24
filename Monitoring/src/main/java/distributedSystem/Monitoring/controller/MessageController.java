package distributedSystem.Monitoring.controller;

import distributedSystem.Monitoring.model.DeviceReading;
import distributedSystem.Monitoring.service.RecentStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final RecentStore store;

    public MessageController(RecentStore store) {
        this.store = store;
    }

    @GetMapping
    public List<DeviceReading> latest(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return store.latest(limit);
    }

    @GetMapping("/count")
    public int count() {
        return store.size();
    }
}
