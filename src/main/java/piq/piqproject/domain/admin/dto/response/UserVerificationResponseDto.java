package piq.piqproject.domain.admin.dto.response;

import piq.piqproject.domain.verification.entity.VerificationEntity;

public record UserVerificationResponseDto(
        Long id,
        Long userId,
        String contentValue,
        String contentType,
        String status,
        String createdAt,
        String updatedAt) {
    public static UserVerificationResponseDto of(VerificationEntity verification) {
        return new UserVerificationResponseDto(
                verification.getId(),
                verification.getUser().getId(),
                verification.getContentValue(),
                verification.getContentType().name(),
                verification.getStatus().name(),
                verification.getCreatedAt().toString(),
                verification.getUpdatedAt().toString());
    }
}
