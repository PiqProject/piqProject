package piq.piqproject.domain.users.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.users.dao.UserDao;
import piq.piqproject.domain.users.dto.MyProfileResponseDto;
import piq.piqproject.domain.users.dto.UserProfileResponseDto;
import piq.piqproject.domain.users.dto.UserSimpleProfileResponseDto;
import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성 (의존성 주입)
public class UserService {

    private final UserDao userDao;

    public MyProfileResponseDto findMyProfile(UserEntity userEntity) {
        // userEntity가 이미 DB에서 조회된 객체이므로 별도 조회가 필요 없습니다.
        return MyProfileResponseDto.from(userEntity);
    }

    public void deleteUser(UserEntity userEntity) {
        // ID를 사용하거나 엔티티 자체를 사용하여 삭제 로직을 수행합니다.
        userDao.deleteByUserEntity(userEntity);
    }

    public List<UserSimpleProfileResponseDto> findAllProfilesByGender(Gender gender) {
        return userDao.findAllByGender(gender).stream()
                .map((user) -> UserSimpleProfileResponseDto.from(user))
                .collect(Collectors.toList());
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