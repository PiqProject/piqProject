package piq.piqproject.domain.users.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.reviews.entity.ReviewEntity;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.users.enums.Gender;
import piq.piqproject.domain.users.enums.Role;

/*
 * 비밀번호 (Password): 사용자의 비밀번호를 반환합니다.
 * 사용자 이름 (Username): 사용자를 식별할 수 있는 고유한 이름(ID)을 반환합니다.
 * 권한 목록 (Authorities): 사용자가 가진 권한(Role) 목록을 GrantedAuthority 객체의 컬렉션으로 반환합니다. (예: "ROLE_USER", "ROLE_ADMIN")
 * 계정 만료 여부 (isAccountNonExpired): 계정이 만료되었는지 여부를 반환합니다.
 * 계정 잠김 여부 (isAccountNonLocked): 계정이 잠겨있는지 여부를 반환합니다.
 * 자격 증명 만료 여부 (isCredentialsNonExpired): 비밀번호가 만료되었는지 여부를 반환합니다.
 * 계정 활성화 여부 (isEnabled): 계정이 활성화 상태인지 여부를 반환합니다.
 * Spring Security는 인증 과정에서 직접 UserEntity 같은 도메인 객체를 알지 못합니다.대신 UserDetails라는 표준화된 인터페이스를 통해 사용자의 정보를 전달받고,이 정보를 기반으로 인증 및 권한 검사를 수행합니다.
 */

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 엔티티는 기본 생성자가 필요하지만, 외부에서 직접 인스턴스화하는 것을 막기 위해 protected로 설정
public class UserEntity extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // @formatter:off DB에서 ID 자동 생성 (MySQL/PostgreSQL의 auto_increment) 나중에 삭제기능넣었을때 ID재사용 고려해야함
    @Column(name = "id")
    private Long id;

    @Column(name="nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name="email", nullable = false, unique = true, length = 100) // email을 로그인 ID로 사용할 것임
    private String email;

    @Column(name="password", nullable = false, length = 255) // 비밀번호는 암호화되므로 길이 여유있게
    private String password;

    @Column(name="kakao_talk_id", nullable = false, length = 100) 
    private String kakaoTalkId;

    @Column(name="instagram_id", nullable = true, length = 100) 
    private String instagramId;

    @Column(name="age", nullable = false) 
    private Integer age;

    @Enumerated(EnumType.STRING) // 지금은 Enum 타입을 DB에 문자열로 저장 -> 이후 postgresql에서 string을 쓸지 Gender enum을 쓸지 고민
    @Column(name="gender", nullable = false) 
    private Gender gender;

    // mappedBy: UserImage 엔티티의 'user' 필드에 의해 매핑되었음을 의미
    // cascade: User가 삭제되면 관련된 Image도 함께 삭제되도록 설정
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserImageEntity> images = new ArrayList<>();

    @Column(name="mbti", nullable = true, length = 10) // NULL 허용, MBTI는 4글자
    private String mbti;

    @Column(name="score", nullable = false) //신뢰점수
    private Double score;

    @Column(name="pq_point", nullable = false) //돈
    private Integer pqPoint;

    @Column(name="introduce", nullable = false, columnDefinition = "TEXT") // 긴 텍스트를 위해 TEXT 타입 지정
    private String introduce;

    @Column(name="is_active", nullable = false) 
    private Boolean isActive; // 활성화 여부

 // --- 기존 @ElementCollection 필드를 아래 코드로 교체 ---
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserRoleEntity> roles = new ArrayList<>();

    /**
     * TODO: 실제로 OneToMany는 지양
     *
     * [이유]
     * - 성능 및 메모리 문제 (N+1 쿼리)
     * ➡️ 부모 엔티티가 자식 엔티티의 컬렉션을 직접 가지는 것은 필요하지도 않은 수많은 데이터를 메모리에 올릴 위험이 존재
     * - 관리의 복잡성과 책임 소재의 모호함
     * ➡️ 높은 결합도: 연관관계의 주인인 Post가 아닌 User에서도 컬렉션을 통해 Post 데이터의 상태를 변경할 가능성이 존재 (데이터 흐름을 추적하기 어려움)
     *
     * 따라서 보통 비즈니스 로직에서 관리하는 것을 지향한다고 하며 데이터가 적을 경우에만 OneToMany를 사용하는 것이 좋다고 함
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<ReviewEntity> reviews = new ArrayList<>();

    // Builder 패턴을 사용하여 객체 생성 가능 (new로 불가)
    @Builder
    public UserEntity(String email,String nickname, String password, String kakaoTalkId, String instagramId,
            Integer age, Gender gender, String mbti, Double score,
            Integer pqPoint, String introduce, Boolean isActive) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.kakaoTalkId = kakaoTalkId;
        this.instagramId = instagramId;
        this.age = age;
        this.gender = gender;
        this.mbti = mbti;
        this.score = score;
        this.pqPoint = pqPoint;
        this.introduce = introduce;
        this.isActive = isActive;
    }

    /**
     * DTO 객체를 UserEntity로 변환하는 메소드 -> 현재 이 Dto는 회원가입 요청에 사용되므로 회원가입 관련 기본정보가 포함됨
     * Service 레이어에서 이 메소드를 호출하여 엔티티를 생성하고 저장
     * 비밀번호 암호화 로직을 포함합니다.
     *
     * @param RequestDto fields, password는 암호화되어야 함
     * @return UserEntity
     */
    public static UserEntity of (String email, String nickname, String password, String kakaoTalkId, String instagramId,
            Integer age, Gender gender, String mbti, Double score,
            Integer pqPoint, String introduce, Boolean isActive ) {
        UserEntity user = UserEntity.builder()
                .email(email)
                .nickname(nickname)
                .password(password)
                .kakaoTalkId(kakaoTalkId)
                .instagramId(instagramId)
                .age(age)
                .gender(gender)
                .mbti(mbti)
                .score(.0)
                .introduce(introduce)
                // ▼ 회원가입 시 서버에서 설정해주는 기본값들
                .pqPoint(0)
                .isActive(true) // 예시: 가입 시 바로 활성 상태
                .build();

                 user.addRole(Role.USER); // 기본 권한 부여
                return user;
    }

     //== 연관관계 편의 메서드 (양방향 관계에서 중요) ==//
    public void addRole(Role role) {
        UserRoleEntity newUserRole = UserRoleEntity.builder()
        .user(this) // '나' 자신(UserEntity)을 설정
        .role(role)
        .build();
        this.roles.add(newUserRole);
    }
    
    /**
     * 사용자가 가진 권한 목록을 반환합니다.
     * @return Collection<? extends GrantedAuthority>
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getKey()))
                .collect(Collectors.toList());
    }

    /**
     * 사용자를 식별할 수 있는 이름(ID)을 반환 (UserDetails의 username)
     * 여기서는 email을 ID로 사용
     * @return String
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * 사용자의 비밀번호를 반환합니다.
     * @return String
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 계정이 만료되지 않았는지 여부를 반환합니다.
     * (true: 만료되지 않음)
     * @return boolean
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 정책이 없으므로 항상 true
    }

    /**
     * 계정이 잠겨있지 않은지 여부를 반환합니다.
     * (true: 잠기지 않음)
     * @return boolean
     */
    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 정책이 없으므로 항상 true
    }

    /**
     * 자격 증명(비밀번호)이 만료되지 않았는지 여부를 반환합니다.
     * (true: 만료되지 않음)
     * @return boolean
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 정책이 없으므로 항상 true
    }

    /**
     * 계정이 활성화 상태인지 여부를 반환합니다.
     * (true: 활성화)
     * @return boolean
     */
    @Override
    public boolean isEnabled() {
        return this.isActive; // DB의 isActive 필드와 연동
    }
     
}