package piq.piqproject.config.jwt;

import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.users.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security가 인증 과정에서 호출하는 메서드입니다.
     * 이메일을 기반으로 DB에서 UserEntity를 조회하여 UserDetails 타입으로 반환합니다.
     * UserEntity가 UserDetails를 구현했으므로, UserEntity 객체 자체를 반환할 수 있습니다.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }
}