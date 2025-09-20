package piq.piqproject.domain.userimages.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.domain.userimages.service.UserImageService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class UserImageController {

    private final UserImageService userImageService;

    /**
     * 클라이언트로부터 이미지 파일을 받아 업로드
     *
     * @param user      @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 정보
     * @param imageFile @RequestPart를 통해 받은 이미지 파일 데이터
     * @return 성공 메시지
     */
    @PostMapping(value = "/upload", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<String> uploadImage(@AuthenticationPrincipal UserEntity user,
            @RequestPart(value = "imageFile") MultipartFile imageFile) {

        // 1. 받은 파일이 비어있는지 확인 (기본적인 유효성 검사)
        if (imageFile.isEmpty())
            throw new InvalidRequestException(ErrorCode.FILE_UPLOAD_ERROR, "업로드할 이미지 파일이 없습니다.");

        // 2. 비즈니스 로직 처리를 위해 서비스 계층으로 파일과 사용자 정보를 전달
        userImageService.uploadImage(user, imageFile);

        // 3. 성공 응답 반환
        return ResponseEntity.ok("이미지가 성공적으로 업로드되었습니다.");
    }

    /**
     * 특정 ID를 가진 이미지를 삭제합니다.
     *
     * @param user    현재 인증된 사용자 정보
     * @param imageId 삭제할 이미지의 ID (URL 경로로부터 받음)
     * @return 성공 메시지
     */
    @PostMapping("/{imageId}/delete")
    public ResponseEntity<String> deleteImage(@AuthenticationPrincipal UserEntity user,
            @PathVariable("imageId") Long imageId) {
        userImageService.deleteImage(user, imageId);
        return ResponseEntity.ok("이미지가 성공적으로 삭제되었습니다.");
    }

    // 대표 이미지 설정
    @PutMapping("/{imageId}/set-main")
    public ResponseEntity<String> setMainImage(@AuthenticationPrincipal UserEntity user,
            @PathVariable("imageId") Long imageId) {
        userImageService.setMainImage(user, imageId);
        return ResponseEntity.ok("대표 이미지가 성공적으로 변경되었습니다.");
    }
}