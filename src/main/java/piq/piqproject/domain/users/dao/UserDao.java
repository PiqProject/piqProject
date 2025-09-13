package piq.piqproject.domain.users.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserDao {

    private final UserRepository userRepository;

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

    public List<UserEntity> findAllByGender(Gender gender) {
        return userRepository.findAllByGender(gender);
    }

    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }
}
