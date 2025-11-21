package distributedSystem.DeviceManagement.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateDeviceMetaRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Min(0)
    private int maximConsumptionValue;
}
