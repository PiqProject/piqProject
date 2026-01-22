package piq.piqproject.domain.inquiries.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.inquiries.enums.InquiryCategory;

@Getter
@AllArgsConstructor
public class FaqResponseDto implements Listable {
    private InquiryCategory category;
    private String question;
    private String answer;
}