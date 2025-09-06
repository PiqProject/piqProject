package piq.piqproject.common.error.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ValidErrorResponseDto {
    private final HttpStatus status;
    private final String code;
    private final List<ErrorDetailsDto> errorDetails;

    @Builder
    private ValidErrorResponseDto(HttpStatus status, String code, List<ErrorDetailsDto> errorDetails) {
        this.status = status;
        this.code = code;
        this.errorDetails = errorDetails;
    }

    public static ValidErrorResponseDto of(HttpStatus status, List<ErrorDetailsDto> errorDetails) {
        return ValidErrorResponseDto.builder()
                .status(status)
                .code("Validation Error")
                .errorDetails(errorDetails)
                .build();
    }
}
