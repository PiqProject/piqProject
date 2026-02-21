package piq.piqproject.domain.traits.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class CreateTraitOptionRequestDto {
    @NotEmpty(message = "Please enter options.")
    private List<@NotBlank(message = "Option cannot be blank.") String> options;
}
