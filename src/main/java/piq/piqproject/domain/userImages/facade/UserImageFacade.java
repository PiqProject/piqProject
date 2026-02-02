package piq.piqproject.domain.userimages.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.common.file.FileUtil;
import piq.piqproject.domain.userimages.service.UserImageService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.verification.entity.VerificationEntity;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.enums.VerificationStatus;
import piq.piqproject.domain.verification.repository.VerificationRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserImageFacade {

    private final UserImageService userImageService;
    private final FileUtil fileUtil;
    private final FileUploader fileUploader;
    private final VerificationRepository verificationRepository;

    /**
     * [이미지 업로드 오케스트레이션]
     * 1. 파일 검증
     * 2. S3 업로드 (DB 트랜잭션 없이 수행 -> DB 커넥션 점유 시간 최소화)
     * 3. DB 저장 (트랜잭션 내 수행)
     * 4. 실패 시 S3 파일 삭제 (보상 트랜잭션)
     */
    public void uploadImage(UserEntity user, MultipartFile imageFile) {
        // 1. 파일 유효성 검증 (확장자, 크기 등)
        if (!fileUtil.isImageFile(imageFile)) {
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "이미지 파일만 업로드할 수 있습니다.");
        }

        // 2. S3 저장 경로 및 파일명 생성
        String fileName = fileUtil.createUniqueFileName(imageFile.getOriginalFilename());
        String s3Path = "profile/" + user.getId() + "/" + fileName;

        // 3. S3 업로드 수행 (네트워크 I/O 발생 - 트랜잭션 밖에서 수행)
        String imageUrl = fileUploader.upload(imageFile, s3Path);

        // 이미지 검증 준비
        VerificationEntity verification = VerificationEntity.of(user, ContentType.IMAGE, imageUrl,
                VerificationStatus.PENDING);
        verificationRepository.save(verification);
    }

    /**
     * [이미지 삭제 위임]
     * 삭제 로직은 DB 데이터 삭제가 메인이므로 Service가 주도권을 가집니다.
     * Facade는 단순히 호출만 전달합니다.
     */
    public void deleteImage(UserEntity user, Long imageId) {
        userImageService.deleteImage(user, imageId);
    }

    /**
     * [대표 이미지 설정 위임]
     */
    public void setMainImage(UserEntity user, Long imageId) {
        userImageService.setMainImage(user, imageId);
    }
}