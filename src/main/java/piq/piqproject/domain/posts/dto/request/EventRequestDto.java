package piq.piqproject.domain.posts.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class EventRequestDto {
    // todo: 제목 최대 길이에 대한 확정 필요 (현재 임시 50자)
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 50, message = "제목은 50자 이하로 입력해주세요.")
    private String title;

    // todo: 내용 최대 길이에 대한 확정 필요 (현재 제한 없음)
    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // requestBody의 날짜/시간 필드를 지정된 패턴("yyyy-MM-dd HH:mm")으로 받아 LocalDateTime으로
    // 바인딩합니다.
    // LocalDateTime의 기본 JSON 형태: 'yyyy-MM-dd'T'HH:mm:ss'
    // 입력되지 않은 하위 시간 단위(초, 나노초 등)는 0으로 채워집니다.
    @NotNull(message = "이벤트 시작일은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "이벤트 종료일은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endDate;

    @AssertTrue(message = "시작일은 종료일보다 이전이어야 합니다.")
    public boolean isDateRangeValid() {
        // isBefore 사용시 엄격하게 검증 가능합니다.
        return startDate.isBefore(endDate);
    }
}
