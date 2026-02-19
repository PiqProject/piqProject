package piq.piqproject.domain.users.service;

import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_INTEREST;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.dto.CoordinateDto;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.interests.entity.InterestEntity;
import piq.piqproject.domain.interests.repository.InterestRepository;
import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.traits.entity.TraitOptionEntity;
import piq.piqproject.domain.traits.repository.TraitOptionRepository;
import piq.piqproject.domain.users.dto.request.UserIdealRequestDto;
import piq.piqproject.domain.users.dto.request.UserInterestRequestDto;
import piq.piqproject.domain.users.dto.request.UserIntroduceRequestDto;
import piq.piqproject.domain.users.dto.request.UserLocationRequestDto;
import piq.piqproject.domain.users.dto.request.UserProfileInitRequestDto;
import piq.piqproject.domain.users.dto.request.UserScoreRequestDto;
import piq.piqproject.domain.users.dto.request.UserTraitRequestDto;
import piq.piqproject.domain.users.dto.response.UserIdealResponseDto;
import piq.piqproject.domain.users.dto.response.UserInterestResponseDto;
import piq.piqproject.domain.users.dto.response.UserScoreResponseDto;
import piq.piqproject.domain.users.dto.response.UserTraitResponseDto;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.entity.UserIdealEntity;
import piq.piqproject.domain.users.entity.UserInterestEntity;
import piq.piqproject.domain.users.entity.UserTraitEntity;
import piq.piqproject.domain.users.repository.UserIdealRepository;
import piq.piqproject.domain.users.repository.UserInterestRepository;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.domain.users.repository.UserTraitRepository;
import piq.piqproject.domain.verification.entity.VerificationEntity;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.enums.VerificationStatus;
import piq.piqproject.domain.verification.repository.VerificationRepository;
import piq.piqproject.infra.external.kakao.service.KakaoGeocodingService;

/**
 * ProfileService는 사용자 프로필수정 비지니스로직을 담당합니다.
 * 
 * 주요 기능:
 * - 사용자 프로필 수정 (사진은 UserImageService에서 담당(분리))
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

        private final UserRepository userRepository;
        private final InterestRepository interestRepository;
        private final UserInterestRepository userInterestRepository;
        private final TraitOptionRepository traitOptionRepository;
        private final UserIdealRepository userIdealRepository;
        private final UserTraitRepository userTraitRepository;
        private final MatchingRepository matchingRepository;
        private final KakaoGeocodingService kakaoGeocodingService; // [주입 확인]
        private final VerificationRepository verificationRepository;

        @Transactional
        public ListResponseDto<UserInterestResponseDto> upsertUserInterests(UserEntity user,
                        UserInterestRequestDto userInterestRequestDto) {
                // 요청으로 들어온 관심사 ID들의 유효성을 검증
                List<Long> interestIds = userInterestRequestDto.getInterestIds();
                List<InterestEntity> interests = interestRepository.findAllById(interestIds);

                // 요청된 ID의 수와 실제 조회된 관심사의 수가 다르면 예외 발생
                if (interests.size() != interestIds.size()) {
                        throw new NotFoundException(NOT_FOUND_INTEREST);
                }

                List<UserInterestEntity> userInterests = userInterestRepository.findAllByUserId(user.getId());

                // 기존 관심사가 있다면 한 번의 쿼리로 모두 삭제하여 성능을 최적화
                if (!userInterests.isEmpty()) {
                        userInterestRepository.deleteAllInBatch(userInterests);
                }

                List<UserInterestEntity> newUserInterestList = interests.stream()
                                .map(interest -> UserInterestEntity.of(user, interest))
                                .toList();

                userInterestRepository.saveAll(newUserInterestList);

                List<UserInterestResponseDto> userInterestResponse = newUserInterestList.stream()
                                .map(UserInterestResponseDto::of)
                                .toList();

                return ListResponseDto.from(userInterestResponse);
        }

        @Transactional
        public ListResponseDto<UserIdealResponseDto> upsertUserIdeals(UserEntity user,
                        UserIdealRequestDto userIdealRequestDto) {
                // 요청으로 들어온 이상형 옵션 ID들의 유효성을 검증
                List<Long> idealOptionIds = userIdealRequestDto.getIdealOptionIds();
                List<TraitOptionEntity> idealOptions = traitOptionRepository.findAllById(idealOptionIds);

                // 요청된 ID의 수와 실제 조회된 관심사의 수가 다르면 예외 발생
                if (idealOptions.size() != idealOptionIds.size()) {
                        throw new NotFoundException(ErrorCode.NOT_FOUND_TRAIT_OPTION,
                                        "요청된 이상형 옵션 ID와 실제 조회된 이상형 옵션 ID의 수가 일치하지 않습니다.");
                }

                List<UserIdealEntity> userIdeals = userIdealRepository.findAllByUserId(user.getId());

                // 기존 이상형이 있다면 한 번의 쿼리로 모두 삭제하여 성능을 최적화
                if (!userIdeals.isEmpty()) {
                        userIdealRepository.deleteAllInBatch(userIdeals);
                }

                List<UserIdealEntity> userIdealList = idealOptions.stream()
                                .map(option -> UserIdealEntity.of(user, option))
                                .toList();

                List<UserIdealResponseDto> userIdealResponse = userIdealList.stream()
                                .map(UserIdealResponseDto::of)
                                .toList();
                return ListResponseDto.from(userIdealResponse);
        }

        @Transactional
        public UserScoreResponseDto scoreUser(UserEntity scorerUser, UserScoreRequestDto userScoreRequestDto) {

                Long scorerId = scorerUser.getId();
                Long targetId = userScoreRequestDto.getTargerUserId();

                // 1. 점수를 받을 유저(targetUser)를 조회합니다.
                UserEntity targetUser = userRepository.findById(targetId)
                                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER,
                                                "점수를 받을 유저를 찾을 수 없습니다."));

                // 2. 두 유저 간의 매칭 정보를 조회합니다.
                // sender, receiver 순서에 상관없이 매칭을 찾아야 합니다.
                MatchingEntity match = matchingRepository.findMatchBetweenUsers(scorerId, targetId)
                                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND,
                                                "두 유저 간의 매칭 정보를 찾을 수 없습니다."));

                // 3. 매칭 상태가 'SUCCESS'가 아니면 예외를 발생시킵니다.
                if (match.getStatus() != MatchingStatus.SUCCESS) {
                        throw new InvalidRequestException(ErrorCode.INVALID_MATCH_STATUS);
                }

                // 5. targetUser의 점수를 업데이트합니다. (평균 계산 로직은 UserEntity로 위임)
                int score = userScoreRequestDto.getScore();
                targetUser.updateScore(score);

                // 6. 변경된 유저의 최종 정보를 담아 DTO로 반환합니다.
                return UserScoreResponseDto.of(targetUser.getNickname(), targetUser.getAverageScore());
        }

        /**
         * 사용자의 실제 특성 목록을 생성하거나 전체 수정합니다. (Upsert)
         *
         * @param user                현재 사용자 엔티티
         * @param userTraitRequestDto 사용자가 선택한 특성 옵션 ID 목록을 담은 DTO
         * @return 업데이트된 사용자의 특성 목록 DTO
         */
        @Transactional
        public ListResponseDto<UserTraitResponseDto> upsertUserTraits(UserEntity user,
                        UserTraitRequestDto userTraitRequestDto) {

                // 1. 요청으로 들어온 특성 옵션 ID들의 유효성을 검증합니다.
                // 내 특성id들 조회
                List<Long> traitOptionIds = userTraitRequestDto.getTraitOptionIds();
                // 특성 정보들 조회
                List<TraitOptionEntity> traitOptions = traitOptionRepository.findAllById(traitOptionIds);

                // 2. 요청된 ID의 수와 실제 DB에서 조회된 엔티티의 수가 다르면, 유효하지 않은 ID가 포함된 것이므로 예외를 발생시킵니다.
                if (traitOptions.size() != traitOptionIds.size()) {
                        throw new NotFoundException(ErrorCode.NOT_FOUND_TRAIT_OPTION,
                                        "요청된 특성 옵션 ID 중 일부가 유효하지 않습니다.");
                }

                // 3. 사용자의 기존 특성 목록을 조회합니다.
                List<UserTraitEntity> userTraits = userTraitRepository.findAllByUserId(user.getId());

                // 4. 기존 특성이 있다면, 한 번의 DELETE 쿼리로 모두 삭제하여 성능을 최적화합니다.
                if (!userTraits.isEmpty()) {
                        userTraitRepository.deleteAllInBatch(userTraits);
                }

                // 5. 새로운 특성 목록을 생성합니다.
                List<UserTraitEntity> newUserTraitList = traitOptions.stream()
                                .map(option -> UserTraitEntity.of(user, option))
                                .toList(); // Java 16+

                // 6. 생성된 새 특성 목록을 한 번의 INSERT 쿼리(bulk insert)로 저장합니다.
                userTraitRepository.saveAll(newUserTraitList);

                // 7. 저장된 최종 특성 목록을 클라이언트에게 반환할 응답 DTO로 변환합니다.
                List<UserTraitResponseDto> userTraitResponse = newUserTraitList.stream()
                                .map(userTrait -> UserTraitResponseDto.from(userTrait.getTraitOption()))
                                .toList();

                return ListResponseDto.from(userTraitResponse);
        }

        /**
         * 사용자 주소 업데이트 (좌표 변환 포함)
         */
        @Transactional
        public void updateUserLocation(UserEntity principalUser, UserLocationRequestDto requestDto) {
                String newAddress = requestDto.getAddress();

                // 1. 카카오 API로 좌표 변환
                CoordinateDto coordinate = kakaoGeocodingService.getCoordinate(newAddress);

                // 좌표를 못 찾으면 예외 처리 (잘못된 주소)
                if (coordinate == null) {
                        throw new InvalidRequestException(ErrorCode.INTERNAL_SERVER_ERROR,
                                        "유효하지 않은 주소입니다. 도로명 주소를 정확히 입력해주세요.");
                }

                // 2. 영속성 컨텍스트를 위해 UserEntity 다시 조회 (안전한 업데이트)
                UserEntity user = userRepository.findById(principalUser.getId())
                                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

                // 3. Point 객체 생성 (핵심 로직)
                // SRID 4326은 WGS84(GPS 좌표계)를 의미합니다.
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

                // 주의: Point(x, y) 순서이므로 (경도, 위도) 순서로 넣어야 합니다.
                // x = Longitude (경도)
                // y = Latitude (위도)
                Point locationPoint = geometryFactory
                                .createPoint(new Coordinate(coordinate.getLongitude(), coordinate.getLatitude()));

                // 4. DB 업데이트
                user.updateLocation(newAddress, locationPoint);
        }

        /**
         * [신규 회원] 최초 프로필 정보 입력 (GUEST -> USER 승격)
         */
        @Transactional
        public void initUserProfile(UserEntity principalUser, UserProfileInitRequestDto requestDto) {
                // 1. 유저 조회
                UserEntity user = userRepository.findById(principalUser.getId())
                                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

                // 2. 주소를 좌표(Point)로 변환
                CoordinateDto coordinate = kakaoGeocodingService.getCoordinate(requestDto.getAddress());
                if (coordinate == null) {
                        throw new InvalidRequestException(ErrorCode.BAD_REQUEST, "유효하지 않은 주소입니다.");
                }

                // GeometryFactory 생성 (WGS84)
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
                Point locationPoint = geometryFactory.createPoint(
                                new Coordinate(coordinate.getLongitude(), coordinate.getLatitude()) // (경도, 위도)
                );

                // 3. 엔티티 업데이트 (권한 승격 포함)
                user.updateProfileInfo(
                                requestDto.getNickname(),
                                requestDto.getAge(),
                                requestDto.getGender(),
                                requestDto.getMbti(),
                                requestDto.getKakaoTalkId(),
                                requestDto.getIntroduce(),
                                requestDto.getUniversity(),
                                locationPoint,
                                requestDto.getAddress());
        }

        /**
         * 사용자 자기소개 수정
         */
        @Transactional
        public void updateUserIntroduce(Long userId, UserIntroduceRequestDto requestDto) {
                UserEntity user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

                VerificationEntity verification = VerificationEntity.of(user, ContentType.INTRO,
                                requestDto.getIntroduce(),
                                VerificationStatus.PENDING);
                verificationRepository.save(verification);
        }
}
