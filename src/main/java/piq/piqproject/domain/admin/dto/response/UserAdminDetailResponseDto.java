package piq.piqproject.domain.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;

@Getter
@Builder
@AllArgsConstructor
public class UserAdminDetailResponseDto {
        // --- 기본 정보 ---
        private Long userId;
        private String nickname;
        private String email;
        private String introduce;
        private String kakaoTalkId;
        private String instagramId;
        private Integer age;
        private Gender gender;
        private String voiceUrl;
        private String mbti;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private String university;

        // --- 점수 및 포인트 ---
        private Double totalScore;
        private int scoreCount;
        private Double averageScore; // 계산된 평균 점수 추가
        private Integer pqPoint;

        // --- 위치 정보 (Point 객체 대신 위도/경도로 분리) ---
        private String address;
        private Double latitude;
        private Double longitude;

        // --- 연관 데이터 (Entity 대신 내부 DTO 사용) ---
        private List<ImageDto> images;
        private List<ReviewDto> reviews;
        private List<String> interests; // 관심사는 키워드만 보여줘도 충분하므로 String List
        private List<TraitDto> ideals; // 이상형 (카테고리 + 옵션명)
        private List<TraitDto> traits; // 내 특성 (카테고리 + 옵션명)

        // =================================================================
        // Static Factory Method (Entity -> DTO 변환 로직)
        // =================================================================
        public static UserAdminDetailResponseDto from(UserEntity user) {
                return UserAdminDetailResponseDto.builder()
                                .userId(user.getId())
                                .nickname(user.getNickname())
                                .email(user.getEmail())
                                .introduce(user.getIntroduce())
                                .kakaoTalkId(user.getKakaoTalkId())
                                .instagramId(user.getInstagramId())
                                .age(user.getAge())
                                .gender(user.getGender())
                                .voiceUrl(user.getVoiceUrl())
                                .mbti(user.getMbti())
                                .isActive(user.isEnabled())
                                .createdAt(user.getCreatedAt())
                                .university(user.getUniversity())

                                .totalScore(user.getTotalScore())
                                .scoreCount(user.getScoreCount())
                                .averageScore(user.getAverageScore())
                                .pqPoint(user.getPqPoint())

                                .address(user.getAddress())
                                // UserEntity에 만들어둔 getLatitude(), getLongitude() 편의 메서드 사용
                                .latitude(user.getLatitude())
                                .longitude(user.getLongitude())

                                // 1. 이미지 변환
                                .images(user.getImages().stream()
                                                .map(img -> new ImageDto(img.getImageId(), img.getImageUrl(),
                                                                img.getIsMainImage()))
                                                .collect(Collectors.toList()))

                                // 2. 리뷰 변환 (필요한 정보만)
                                .reviews(user.getReviews().stream()
                                                .map(review -> new ReviewDto(review.getId(), review.getContent(),
                                                                review.getRate()))
                                                .collect(Collectors.toList()))

                                // 3. 관심사 변환 (키워드만 추출)
                                .interests(user.getUserInterests().stream()
                                                .map(ui -> ui.getInterest().getKeyword())
                                                .collect(Collectors.toList()))

                                // 4. 이상형 변환 (카테고리 + 옵션명)
                                .ideals(user.getUserIdeals().stream()
                                                .map(ui -> new TraitDto(
                                                                ui.getIdealOption().getCategory().getName(),
                                                                ui.getIdealOption().getName()))
                                                .collect(Collectors.toList()))

                                // 5. 내 특성 변환 (카테고리 + 옵션명)
                                .traits(user.getUserTraits().stream()
                                                .map(ut -> new TraitDto(
                                                                ut.getTraitOption().getCategory().getName(),
                                                                ut.getTraitOption().getName()))
                                                .collect(Collectors.toList()))

                                .build();
        }

        // =================================================================
        // Inner DTO Classes (외부로 노출할 데이터 구조 정의)
        // =================================================================

        @Getter
        @AllArgsConstructor
        public static class ImageDto {
                private Long imageId;
                private String imageUrl;
                private boolean isMain;
        }

        @Getter
        @AllArgsConstructor
        public static class ReviewDto {
                private Long reviewId;
                private String content;
                private int rate;
        }

        @Getter
        @AllArgsConstructor
        public static class TraitDto {
                private String category; // 예: "성격", "외모"
                private String option; // 예: "차분한", "고양이상"
        }
}