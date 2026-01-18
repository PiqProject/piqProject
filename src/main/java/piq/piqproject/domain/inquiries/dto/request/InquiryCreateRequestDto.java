package piq.piqproject.domain.inquiries.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.inquiries.enums.InquiryCategory;

@Getter
@NoArgsConstructor
public class InquiryCreateRequestDto {

    @NotNull(message = "카테고리는 필수입니다.")
    private InquiryCategory category; // PAYMENT, ACCOUNT, etc...

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;
}
