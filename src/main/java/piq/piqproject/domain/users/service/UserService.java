package piq.piqproject.domain.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import piq.piqproject.domain.users.dao.UserDao;
import piq.piqproject.domain.users.dto.SignUpRequestDto;
import piq.piqproject.domain.users.entity.UserEntity;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성 (의존성 주입)
public class UserService /* implements UserDetailsService */ {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 비즈니스 로직을 처리하는 메소드
     * 
     * @param signUpRequestDto 회원가입 요청 DTO
     * @return 저장된 UserEntity
     */
    @Transactional // 데이터베이스에 변경 작업을 하므로 트랜잭션 처리
    public UserEntity signUp(SignUpRequestDto signUpRequestDto) {
        // 1. 이메일 중복 확인
        if (userDao.existsByEmail(signUpRequestDto.getEmail())) {
            // 실무에서는 custom exception을 정의하여 사용하는 것이 좋습니다.
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 2. DTO를 Entity로 변환 (이때 비밀번호 암호화가 이루어짐)
        UserEntity userEntity = signUpRequestDto.toEntity(passwordEncoder);

        // 3. 사용자 정보 저장
        return userDao.saveUser(userEntity);
    }

    /**
     * Spring Security가 사용자 인증 시 호출하는 메소드
     * 
     * @param username (우리 시스템에서는 email)
     * @return UserDetails 객체 (Spring Security가 사용하는 사용자 정보)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때 발생하는 예외
     */
    // @Override
    // @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    // public UserDetails loadUserByUsername(String username) throws
    // UsernameNotFoundException {
    // // 'username' 파라미터가 실제로는 우리 시스템의 'email'을 의미합니다.
    // UserEntity userEntity = userRepository.findByEmail(username)
    // .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 찾을 수 없습니다: " +
    // username));

    // // Spring Security가 이해할 수 있는 UserDetails 타입으로 변환하여 반환합니다.
    // // User 클래스는 UserDetails 인터페이스의 구현체입니다.
    // return new User(
    // userEntity.getEmail(),
    // userEntity.getPassword(),
    // // userEntity.getAuthorities() // UserEntity에 UserDetails를 직접 구현했다면 이 코드를 사용
    // // 아래는 UserEntity에 roles 필드(List<String>)가 있다는 가정 하에 권한을 설정하는 코드입니다.
    // userEntity.getRoles().stream()
    // .map(role -> new
    // org.springframework.security.core.authority.SimpleGrantedAuthority(role))
    // .collect(java.util.stream.Collectors.toList()));
    // }
}