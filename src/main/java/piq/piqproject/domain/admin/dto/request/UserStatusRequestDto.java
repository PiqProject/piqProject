package piq.piqproject.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserStatusRequestDto {
    @NotNull
    private Boolean isActive; // true: 활성(해제), false: 비활성(정지)
}