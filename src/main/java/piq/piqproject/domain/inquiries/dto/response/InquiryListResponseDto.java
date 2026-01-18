package piq.piqproject.domain.inquiries.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.inquiries.entity.InquiryEntity;
import piq.piqproject.domain.inquiries.enums.InquiryCategory;
import piq.piqproject.domain.inquiries.enums.InquiryStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class InquiryListResponseDto {
    private Long id;
    private InquiryCategory category;
    private String title;
    private InquiryStatus status; // 답변 대기/완료
    private LocalDateTime createdAt;

    public static InquiryListResponseDto from(InquiryEntity entity) {
        return InquiryListResponseDto.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}