package distributedSystem.Monitoring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "device_monitoring_ref")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceMonitoringRef {

    @Id
    private Long device_id;
    private Long userId;
    private int maximConsumptionValue;
}
