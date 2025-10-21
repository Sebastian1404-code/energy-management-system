package distributedSystem.DeviceManagement.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "device_user_refs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class DeviceUserRef {
    @Id
    @Column(name = "user_id")
    private Long userId;


}
