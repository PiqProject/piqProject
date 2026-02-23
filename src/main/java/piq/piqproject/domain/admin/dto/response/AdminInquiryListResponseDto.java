package piq.piqproject.domain.admin.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.inquiries.entity.InquiryEntity;
import piq.piqproject.domain.inquiries.enums.InquiryCategory;
import piq.piqproject.domain.inquiries.enums.InquiryStatus;

@Getter
@Builder
@AllArgsConstructor
public class AdminInquiryListResponseDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userNickname;
    private InquiryCategory category;
    private String title;
    private InquiryStatus status;
    private LocalDateTime createdAt;

    public static AdminInquiryListResponseDto from(InquiryEntity entity) {
        return AdminInquiryListResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userEmail(entity.getUser().getEmail())
                .userNickname(entity.getUser().getNickname())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}