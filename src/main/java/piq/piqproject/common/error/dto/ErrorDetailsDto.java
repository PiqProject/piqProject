package piq.piqproject.common.error.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Validation 실패 시 발생하는 필드별 상세 오류 정보를 담는 DTO입니다.
 * <p>
 * ErrorResponseDto 내부에 리스트 형태로 포함되어 클라이언트에게 어떤 입력 필드에서 어떤 유효성 검사 규칙을 위반했는지 구체적인
 * 메세지를 제공합니다.
 */
@Getter
public class ErrorDetailsDto {
    private final String field;
    private final String message;

    @Builder
    private ErrorDetailsDto(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public static ErrorDetailsDto of(String field, String message) {
        return ErrorDetailsDto.builder()
                .field(field)
                .message(message)
                .build();
    }
}
