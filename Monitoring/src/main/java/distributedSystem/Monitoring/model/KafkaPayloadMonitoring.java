package distributedSystem.Monitoring.model;


import distributedSystem.Monitoring.events.DevMonType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class KafkaPayloadMonitoring {
    private Long deviceId;
    private DevMonType devMonType;
}
