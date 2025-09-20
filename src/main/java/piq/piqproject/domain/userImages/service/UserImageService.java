package piq.piqproject.domain.userimages.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.error.exception.UnauthorizedException;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.common.file.FileUtil;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.userimages.repository.UserImageRepository;
import piq.piqproject.domain.users.entity.UserEntity;

@Service
@RequiredArgsConstructor
@Transactional
public class UserImageService {

    private final UserImageRepository userImageRepository;
    private final FileUploader fileUploader; // LocalUploader 또는 S3Uploader가 주입
    private final FileUtil fileUtil;

    private static final int MAX_IMAGE_COUNT = 6; // 비즈니스 규칙: 사용자당 최대 이미지 개수

    /**
     * 사용자의 이미지를 업로드합니다.
     */
    @Transactional
    public void uploadImage(UserEntity user, MultipartFile imageFile) {
        // 1. 비즈니스 규칙 검증: 이미지 개수 제한
        validateImageCount(user);

        // 2. 파일 유효성 검증: 이미지 파일인지 확인
        if (!fileUtil.isImageFile(imageFile)) {
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, " 이미지 파일이 아닙니다.");
        }

        // 3. 파일 경로/이름 생성
        // fullPath는 루트에서부터가 아닌 사진을 저장하기로한 최상위 디렉토리까지만을 의미한다.
        // 저장위치의 최상위 C:/는 FileUploader가 관리한다.
        String directoryPath = fileUtil.createDirectoryPath();
        String fileName = fileUtil.createUniqueFileName(imageFile.getOriginalFilename());
        String relativePath = directoryPath + "/" + fileName;

        // 4. FileUploader를 통해 파일을 스토리지에 업로드하고, 최종 URL을 받아옴
        // fullPath는 절대경로가 아님을 유의, 실제 저장 위치는 FileUploader 구현체에 따라 다름
        // fullPath 예시: /images/2025/09/17/~~~~.jpg
        // imageUrl은 절대경로가된다.
        String imageUrl = fileUploader.upload(imageFile, relativePath);

        // 5. DB에 이미지 정보 저장
        // 현재 대표 이미지가 없는 경우, 이 이미지를 대표 이미지로 설정
        boolean isMain = !userImageRepository.existsByUserAndIsMainImage(user, true);

        UserImageEntity newImage = UserImageEntity.builder()
                .user(user)
                .imageUrl(imageUrl)
                .isMainImage(isMain)
                .build();

        userImageRepository.save(newImage);
    }

    private void validateImageCount(UserEntity user) {
        long currentImageCount = userImageRepository.countByUser(user);
        if (currentImageCount >= MAX_IMAGE_COUNT) {
            throw new InternalServerException(ErrorCode.FILE_NUMBER_EXCEEDED);
        }
    }

    /**
     * 사용자의 이미지를 삭제합니다.
     * 이 메서드는 데이터베이스 변경과 외부 스토리지 I/O를 포함하므로,
     * 트랜잭션 경계와 예외 처리에 주의해야 합니다.
     *
     * @param user    삭제를 요청하는 인증된 사용자
     * @param imageId 삭제할 이미지의 고유 ID
     */
    @Transactional
    public void deleteImage(UserEntity user, Long imageId) {
        // Step 1: 영속성 컨텍스트 내에서 엔티티 조회 (Fail-Fast)
        UserImageEntity imageToDelete = userImageRepository.findById(imageId)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCode.FILE_DELETE_ERROR,
                                "존재하지 않는 이미지입니다. ID: " + imageId));

        // Step 2: 권한 검증 (Authorization)
        validateOwnership(user, imageToDelete);

        // Step 3: 외부 스토리지의 물리적 파일 삭제
        // TODO ※ 이 작업은 데이터베이스 트랜잭션의 롤백 대상이 아님
        fileUploader.delete(imageToDelete.getImageUrl());

        // Step 4: 데이터베이스에서 엔티티 삭제
        userImageRepository.delete(imageToDelete);

        // Step 5: 비즈니스 규칙 후처리 (Edge Case Handling)
        handleMainImageAfterDeletion(user, imageToDelete);
    }

    /**
     * 이미지 삭제 요청에 대한 소유권(Ownership)을 검증합니다.
     * 
     * @param currentUser 요청을 보낸 사용자
     * @param image       삭제 대상 이미지
     */
    private void validateOwnership(UserEntity currentUser, UserImageEntity image) {
        if (!image.getUser().getId().equals(currentUser.getId())) {
            throw new InternalServerException(ErrorCode.AUTHORITY_ERROR, "해당 이미지를 삭제할 권한이 없습니다.");
        }
    }

    /**
     * 삭제된 이미지가 대표 이미지였을 경우, 후속 조치를 처리합니다.
     * 
     * @param user         이미지 소유자
     * @param deletedImage 방금 삭제된 이미지 엔티티
     */
    private void handleMainImageAfterDeletion(UserEntity user, UserImageEntity deletedImage) {
        // 삭제된 이미지가 대표 이미지가 아니었다면, 아무것도 할 필요 없음
        if (!deletedImage.getIsMainImage()) {
            return;
        }

        // 대표 이미지가 삭제되었으므로, 남은 이미지 중 하나를 새 대표로 지정
        List<UserImageEntity> remainingImages = userImageRepository.findAllByUser(user);
        if (!remainingImages.isEmpty()) {
            UserImageEntity newMainImage = remainingImages.get(0); // 예: 첫 번째 이미지를 새 대표로
            newMainImage.setMainImage(true);
            // userImageRepository.save(newMainImage)는 필요 없음
            // -> 영속성 컨텍스트의 'Dirty Checking'에 의해 트랜잭션 커밋 시 자동 업데이트됨
        }
    }

    /**
     * 대표 이미지 설정
     */
    @Transactional
    public void setMainImage(UserEntity user, Long imageId) {
        // 1. 현재 유저의 기존 대표 이미지를 찾아서 false로 변경
        userImageRepository.findByUserAndIsMainImage(user, true)
                .ifPresent(oldMainImage -> oldMainImage.setMainImage(false));

        // 2. 새로 지정된 이미지에 대한 UserImageEntity를 가져옴
        UserImageEntity newMainImage = userImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이미지입니다."));

        // 본인 소유의 이미지가 맞는지 확인하는 로직
        if (!newMainImage.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException(ErrorCode.AUTHORITY_ERROR, "이미지를 변경할 권한이 없습니다.");
        }

        newMainImage.setMainImage(true);
    }

}