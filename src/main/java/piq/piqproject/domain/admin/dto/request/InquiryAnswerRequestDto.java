package piq.piqproject.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryAnswerRequestDto {
    @NotBlank(message = "답변 내용은 필수입니다.")
    private String answer;
}