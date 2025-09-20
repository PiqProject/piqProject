package piq.piqproject.domain.users.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
@RequiredArgsConstructor
public class UserDao {

    private final UserRepository userRepository;

    public Optional<UserEntity> findByIdWithImages(Long id) {
        return userRepository.findByIdWithImages(id);
    }

    /**
     * 새로운 사용자 정보를 데이터베이스에 저장합니다.
     * 
     * @param userEntity 저장할 사용자 엔티티
     * @return 저장된 사용자 엔티티
     */
    public UserEntity saveUser(UserEntity userEntity) {
        return userRepository.save(userEntity);
    }

    /**
     * 주어진 이메일이 데이터베이스에 존재하는지 확인합니다.
     * 
     * @param email 확인할 이메일
     * @return 존재하면 true, 그렇지 않으면 false
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 이메일을 기준으로 사용자 정보를 조회합니다.
     * 
     * @param email 조회할 이메일
     * @return Optional<UserEntity>
     */
    public Optional<UserEntity> findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    public void deleteByUserEntity(UserEntity userEntity) {
        userRepository.delete(userEntity);
    }

    public Page<UserEntity> findAllByGender(Gender gender, Pageable pageable) {
        // Repository에 pageable 객체를 그대로 전달합니다.
        return userRepository.findAllByGender(gender, pageable);
    }

    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}
