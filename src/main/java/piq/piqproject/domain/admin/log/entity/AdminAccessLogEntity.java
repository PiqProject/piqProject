package piq.piqproject.domain.admin.log.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;

@Entity
@Table(name = "admin_access_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccessLogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminId; // 행위자(관리자) ID(PK)

    @Column(nullable = false)
    private String adminEmail;

    @Column(nullable = false)
    private String ip; // 접속 IP

    @Column(nullable = false)
    private String httpMethod;

    @Column(nullable = false)
    private String url; // 요청한 URL

    @Column(nullable = false)
    private String action; // 수행한 작업 (예: "회원 강제 탈퇴")

    @Column(columnDefinition = "TEXT")
    private String target; // 대상 정보 (파라미터 등)

    @Builder
    public AdminAccessLogEntity(Long adminId, String adminEmail, String ip, String httpMethod, String url,
            String action, String target) {
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.ip = ip;
        this.httpMethod = httpMethod;
        this.url = url;
        this.action = action;
        this.target = target;
    }
}