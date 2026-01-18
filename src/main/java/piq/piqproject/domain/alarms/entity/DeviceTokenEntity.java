package piq.piqproject.domain.alarms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.users.entity.UserEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "device_token")
public class DeviceTokenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    /**
     * 푸시 알림용 디바이스 토큰.
     */
    @Column(name = "token", nullable = false)
    private String token;

    @Builder
    public DeviceTokenEntity(UserEntity user, String deviceType, String token) {
        this.user = user;
        this.deviceType = deviceType;
        this.token = token;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
