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
public class InquiryDetailResponseDto {
    private Long id;
    private InquiryCategory category;
    private String title;
    private String content; // 질문 내용
    private InquiryStatus status;
    private LocalDateTime createdAt;

    // 답변 영역
    private String answer; // 답변 내용 (없으면 null)
    private LocalDateTime answeredAt;

    public static InquiryDetailResponseDto from(InquiryEntity entity) {
        return InquiryDetailResponseDto.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .answer(entity.getAnswer())
                .answeredAt(entity.getAnsweredAt())
                .build();
    }
}