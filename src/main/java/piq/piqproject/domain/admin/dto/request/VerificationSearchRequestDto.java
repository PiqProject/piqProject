package piq.piqproject.domain.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.enums.VerificationStatus;

@Getter
@Setter
@NoArgsConstructor
public class VerificationSearchRequestDto {
    private ContentType contentType; // IMAGE, VOICE, INTRO
    private VerificationStatus status; // PENDING, APPROVED, REJECTED
}
