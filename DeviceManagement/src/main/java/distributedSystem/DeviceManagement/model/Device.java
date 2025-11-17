package distributedSystem.DeviceManagement.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "devices")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;
    private int maximConsumptionValue;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(
            name = "user_id",                      // FK column on devices
            referencedColumnName = "user_id",      // PK column on device_user_refs
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_device_userref")
    )
    private DeviceUserRef userRef;
}
