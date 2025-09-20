package piq.piqproject.domain.users.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.users.dao.UserDao;
import piq.piqproject.domain.users.dto.response.MyProfileResponseDto;
import piq.piqproject.domain.users.dto.response.UserProfileResponseDto;
import piq.piqproject.domain.users.dto.response.UserSimpleProfileResponseDto;
import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성 (의존성 주입)
public class UserService {

    private final UserDao userDao;

    @Transactional(readOnly = true)
    public MyProfileResponseDto findMyProfile(Long userId) {
        // 트랜잭션 안에서 Fetch Join으로 DB 조회 (세션이 살아있음)
        UserEntity userEntity = userDao.findByIdWithImages(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 안전하게 DTO 변환
        return MyProfileResponseDto.from(userEntity);
    }

    public void deleteUser(UserEntity userEntity) {
        // ID를 사용하거나 엔티티 자체를 사용하여 삭제 로직을 수행합니다.
        userDao.deleteByUserEntity(userEntity);
    }

    public Page<UserSimpleProfileResponseDto> findAllProfilesByGender(Gender gender, Pageable pageable) {
        // 1. DAO를 호출하여 Page<UserEntity>를 받습니다.
        Page<UserEntity> userEntityPage = userDao.findAllByGender(gender, pageable);

        // 2. Page 객체가 제공하는 map() 함수를 사용하여 내용물(Entity)을 DTO로 변환합니다.
        return userEntityPage.map(user -> UserSimpleProfileResponseDto.from(user));
    }

    /**
     * 사용자 ID(PK)를 기반으로 특정 사용자의 공개 프로필을 조회합니다.
     *
     * @param id 조회할 사용자의 ID
     * @return UserProfileResponseDto
     * @throws EntityNotFoundException 해당 ID의 사용자가 없을 경우 발생
     */
    @Transactional(readOnly = true)
    public UserProfileResponseDto findUserProfileById(Long id) {
        UserEntity user = userDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + id));

        // 2. 조회된 UserEntity를 DTO로 변환하여 반환합니다.
        return UserProfileResponseDto.from(user);
    }

}