package piq.piqproject.domain.posts.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CreateAnnouncementRequestDto {

    //todo: 제목 최대 길이에 대한 확정 필요 (현재 임시 50자)
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 50, message = "제목은 50자 이하로 입력해주세요.")
    private final String title;

    //todo: 내용 최대 길이에 대한 확정 필요 (현재 제한 없음)
    @NotBlank(message = "내용을 입력해주세요.")
    private final String content;
}
