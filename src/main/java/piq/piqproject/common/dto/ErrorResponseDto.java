package piq.piqproject.common.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorResponseDto {
    private final HttpStatus status;
    private final String code;
    private final String message;

    @Builder
    private ErrorResponseDto(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public static ErrorResponseDto of(HttpStatus status, String code, String message) {
        return ErrorResponseDto.builder()
                .status(status)
                .code(code)
                .message(message)
                .build();
    }
}
