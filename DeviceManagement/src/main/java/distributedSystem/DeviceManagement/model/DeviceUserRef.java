package distributedSystem.DeviceManagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "device_user_ref")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceUserRef {

    @Id
    @Column(name = "user_id")
    private Long user_id;
}
