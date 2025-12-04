// src/main/java/distributedSystem/Monitoring/controller/MessageController.java
package distributedSystem.Monitoring.controller;

import distributedSystem.Monitoring.dto.DeviceSummaryDto;
import distributedSystem.Monitoring.service.WindowAggregator;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitoring")
public class MessageController {

    private final WindowAggregator windowAggregator;



    public MessageController(
            WindowAggregator windowAggregator) {
        this.windowAggregator = windowAggregator;
    }




    @GetMapping
    public List<DeviceSummaryDto> list() {
        return windowAggregator.listDeviceSummaries();

    }

    @GetMapping("/devices/{deviceId}/series")
    public List<Map<String, Object>> rawSeriesForDay(
            @PathVariable String deviceId,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "UTC") String tz,
            @RequestParam(name = "virtualHourMinutes", defaultValue = "#{${app.aggregate-minutes:60}}")
            int virtualHourMinutes
    ) {
        return windowAggregator.buildRawSeriesForDay(deviceId, date, tz, virtualHourMinutes);

    }
}
