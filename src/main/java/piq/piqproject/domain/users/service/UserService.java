package piq.piqproject.domain.users.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.users.dto.response.MyProfileResponseDto;
import piq.piqproject.domain.users.dto.response.UserProfileResponseDto;
import piq.piqproject.domain.users.dto.response.UserSimpleProfileResponseDto;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;
import piq.piqproject.domain.users.enums.Role;
import piq.piqproject.domain.users.enums.SocialType;
import piq.piqproject.domain.users.repository.UserRepository;

/**
 * UserService는 사용자를 가져오거나 삭제하는 비즈니스 로직을 담당합니다.
 *
 * 주요 기능:
 * - 사용자 프로필 조회
 * - 사용자 삭제
 * - 성별에 따른 사용자 목록 조회
 * - 관리자 계정 생성
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // DI 주입

    @Transactional(readOnly = true)
    public MyProfileResponseDto findMyProfile(Long userId) {
        // 트랜잭션 안에서 Fetch Join으로 DB 조회 (세션이 살아있음)
        UserEntity userEntity = userRepository.findByIdWithImages(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 안전하게 DTO 변환
        return MyProfileResponseDto.from(userEntity);
    }

    public void deleteUser(UserEntity userEntity) {
        // ID를 사용하거나 엔티티 자체를 사용하여 삭제 로직을 수행합니다.
        userRepository.delete(userEntity);
    }

    public Page<UserSimpleProfileResponseDto> findAllProfilesByGender(Gender gender, Pageable pageable) {
        // 1. DAO를 호출하여 Page<UserEntity>를 받습니다.
        Page<UserEntity> userEntityPage = userRepository.findAllByGender(gender, pageable);

        // 2. Page 객체가 제공하는 map() 함수를 사용하여 내용물(Entity)을 DTO로 변환합니다.
        return userEntityPage.map(user -> UserSimpleProfileResponseDto.from(user));
    }

    /**
     * 사용자 ID(PK)를 기반으로 특정 사용자의 공개 프로필을 조회합니다.
     *
     * @param id 조회할 사용자의 ID
     * @return UserProfileResponseDto
     * @throws NotFoundException 해당 ID의 사용자가 없을 경우 발생
     */
    @Transactional(readOnly = true)
    public UserProfileResponseDto findUserProfileById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "해당 ID의 사용자를 찾을 수 없습니다: " + id));

        // 2. 조회된 UserEntity를 DTO로 변환하여 반환합니다.
        return UserProfileResponseDto.from(user);
    }

    @Transactional
    public void createAdminAccount(String email, String password) {
        // 1. 이메일 중복 확인 (중복된 이메일은 허용하지 않음)
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 암호화 (중요!)
        String encodedPassword = passwordEncoder.encode(password);

        // 3. UserEntity 생성
        // 기본 Point 객체 생성 (JTS 라이브러리 사용)
        // SRID 4326 = WGS84 (GPS 위경도 좌표계)
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // 예시: 서울 시청 좌표 (경도 126.9780, 위도 37.5665)
        // 주의: Coordinate(x, y) 순서이므로 (경도, 위도) 순서로 넣어야 합니다.
        Point defaultLocation = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        UserEntity adminUser = UserEntity.of(
                email,
                "Admin",
                encodedPassword,
                "kakaoAdmin",
                "instagramAdmin",
                30,
                Gender.MALE,
                "MBTI",
                0.0,
                100000000,
                "자기소개",
                true,
                true,
                true,
                "서울시청", // address (관리자용 기본 주소)
                defaultLocation,
                "광운대학교",
                SocialType.NONE, // 소셜 타입
                null); // 소셜 ID
        adminUser.addRole(Role.ADMIN); // 관리자 권한 부여

        // 4. DB에 저장
        userRepository.save(adminUser);
    }

    @Transactional
    public void updateAppAlarmStatus(Long userId, Boolean isAppAlarm) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "해당 ID의 사용자를 찾을 수 없습니다: " + userId));
        user.updateAppAlarmStatus(isAppAlarm);
    }

    @Transactional
    public void updateWebAlarmStatus(Long id, Boolean isWebAlarm) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "해당 ID의 사용자를 찾을 수 없습니다: " + id));
        user.updateWebAlarmStatus(isWebAlarm);
    }

}