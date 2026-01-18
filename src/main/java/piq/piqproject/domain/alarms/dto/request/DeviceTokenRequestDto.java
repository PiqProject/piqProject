package piq.piqproject.domain.alarms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeviceTokenRequestDto {

    @NotBlank(message = "디바이스 토큰은 필수입니다.")
    private String token;

    @NotBlank(message = "디바이스 종류(deviceType)는 필수입니다.")
    private String deviceType;

    public DeviceTokenRequestDto(String token, String deviceType) {
        this.token = token;
        this.deviceType = deviceType;
    }
}
