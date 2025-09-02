package piq.piqproject.common.error.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ErrorResponseDto {
    private final HttpStatus status;
    private final String code;
    private final String message;
    private final List<ErrorDetailsDto> errorDetails;

    @Builder
    private ErrorResponseDto(HttpStatus status, String code, String message, List<ErrorDetailsDto> errorDetails) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.errorDetails = errorDetails;
    }

    public static ErrorResponseDto of(HttpStatus status, String code, String message) {
        return ErrorResponseDto.builder()
                .status(status)
                .code(code)
                .message(message)
                .build();
    }

    public static ErrorResponseDto ofValidationErrors(HttpStatus status, List<ErrorDetailsDto> errorDetails) {
        return ErrorResponseDto.builder()
                .status(status)
                .code("Validation Error")
                .errorDetails(errorDetails)
                .build();
    }
}
