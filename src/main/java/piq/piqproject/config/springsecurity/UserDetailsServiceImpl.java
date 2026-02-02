package piq.piqproject.config.springsecurity;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.CustomException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security가 인증 과정에서 호출하는 메서드입니다.
     * 이메일을 기반으로 DB에서 UserEntity를 조회하여 UserDetails 타입으로 반환합니다.
     * UserEntity가 UserDetails를 구현했으므로, UserEntity 객체 자체를 반환할 수 있습니다.
     * param의 id는 jwt 토큰의 subject로 String 타입으로 들어오므로 Long 타입으로 변환하여 사용
     * 
     * @param id
     * @return UserDetails UserEntity
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws CustomException {
        return userRepository.findByIdWithRoles(Long.parseLong(username))
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "사용자를 찾을 수 없습니다: " + username));
    }
}