package piq.piqproject.domain.users.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserIdealRequestDto {
    @NotEmpty(message = "plz, enter ideal option ids")
    private List<Long> idealOptionIds;
}