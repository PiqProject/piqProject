package piq.piqproject.domain.userimages.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.error.exception.UnauthorizedException;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.userimages.repository.UserImageRepository;
import piq.piqproject.domain.users.entity.UserEntity;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserImageService {

    private final UserImageRepository userImageRepository;
    private final FileUploader fileUploader;

    private static final int MAX_IMAGE_COUNT = 4; // 비즈니스 규칙: 사용자당 최대 이미지 개수

    /**
     * [DB 저장 전용 메서드]
     * Facade에서 호출되며, 이 메서드가 실행될 때 비로소 트랜잭션이 시작됩니다.
     * S3 업로드는 이미 끝난 상태입니다.
     */
    @Transactional
    public void saveImageToDb(UserEntity user, String imageUrl) {
        // 1. 비즈니스 규칙 검증 (이미지 개수 제한)
        // DB 락이 필요하다면 여기서 수행되므로 안전함
        validateImageCount(user);

        // 2. 첫 이미지인지 확인 (대표 이미지 자동 설정용)
        boolean isMain = !userImageRepository.existsByUserAndIsMainImage(user, true);

        // 3. 엔티티 생성 및 저장
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
     * 사용자의 이미지를 삭제
     *
     * @param user    삭제를 요청하는 인증된 사용자
     * @param imageId 삭제할 이미지의 고유 ID
     */
    @Transactional
    public void deleteImage(UserEntity user, Long imageId) {
        // 1. 영속성 컨텍스트 내에서 엔티티 조회
        UserImageEntity imageToDelete = userImageRepository.findById(imageId)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCode.FILE_DELETE_ERROR,
                                "존재하지 않는 이미지입니다. ID. " + imageId));

        // 2. 권한 검증
        validateOwnership(user, imageToDelete);

        String imageUrl = imageToDelete.getImageUrl();
        boolean wasMainImage = imageToDelete.getIsMainImage();

        // 3. DB 삭제 (먼저 수행)
        userImageRepository.delete(imageToDelete);

        // 4. 후처리 (대표 이미지 재설정) - DB 삭제가 일어났으므로 트랜잭션 내에서 수행
        if (wasMainImage) {
            handleMainImageAfterDeletion(user, imageId);
        }

        // 5. [트랜잭션 동기화] S3 파일 삭제는 커밋 후에 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    fileUploader.delete(imageUrl);
                    log.info("S3 이미지 삭제 완료: {}", imageUrl);
                } catch (Exception e) {
                    // TODO: S3 이미지 삭제 실패 시 DB에 저장하고 스케쥴러로 정리하는 로직 필요
                    log.error(" 이미지 삭제 실패. URL: {}", imageUrl, e);
                }
            }
        });
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
    private void handleMainImageAfterDeletion(UserEntity user, Long deletedImageId) {
        List<UserImageEntity> remainingImages = userImageRepository.findAllByUser(user);

        // 삭제된 이미지가 리스트에 포함되어 있다면 제외 (안전장치)
        UserImageEntity newMainImage = remainingImages.stream()
                .filter(img -> !img.getImageId().equals(deletedImageId)) // ID로 비교해서 제외
                .findFirst()
                .orElse(null);

        if (newMainImage != null) {
            newMainImage.setMainImage(true);
        }
    }

    /**
     * 대표 이미지 설정
     */
    @Transactional
    public void setMainImage(UserEntity user, Long imageId) {
        // 1. 기존 대표 이미지 해제
        userImageRepository.findByUserAndIsMainImage(user, true)
                .ifPresent(oldMainImage -> oldMainImage.setMainImage(false));

        // 2. 새 대표 이미지 설정
        UserImageEntity newMainImage = userImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "존재하지 않는 이미지입니다."));

        // 3. 권한 확인
        if (!newMainImage.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException(ErrorCode.AUTHORITY_ERROR, "이미지를 변경할 권한이 없습니다.");
        }

        newMainImage.setMainImage(true);
    }

}