package piq.piqproject.domain.users.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자의 역할을 정의하는 Enum 클래스입니다.
 * Spring Security에서는 일반적으로 역할 이름 앞에 'ROLE_' 접두사를 붙입니다.
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "신");
    // 나중에 새로운 역할이 필요하면 여기에 추가하면 됩니다. 예: GUEST, MANAGER 등

    private final String key;
    private final String title;
}