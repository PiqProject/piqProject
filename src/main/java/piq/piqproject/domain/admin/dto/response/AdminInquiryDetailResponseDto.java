package piq.piqproject.domain.admin.dto.response;

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
public class AdminInquiryDetailResponseDto {
    private Long id;

    // 작성자 정보 (CS 처리를 위해 필요)
    private Long userId;
    private String userEmail;
    private String userNickname;

    private InquiryCategory category;
    private String title;
    private String content;
    private InquiryStatus status;
    private LocalDateTime createdAt;

    // 답변 정보
    private String answer;
    private LocalDateTime answeredAt;
    private String answeredBy;

    public static AdminInquiryDetailResponseDto from(InquiryEntity entity) {
        return AdminInquiryDetailResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userEmail(entity.getUser().getEmail())
                .userNickname(entity.getUser().getNickname())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .answer(entity.getAnswer())
                .answeredAt(entity.getAnsweredAt())
                .answeredBy(entity.getAnsweredBy())
                .build();
    }
}