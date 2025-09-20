package piq.piqproject.domain.userimages.entity;

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

//Todo: 이미지를 어떻게 전달받을지 고민 필요
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "userImages") // DB 테이블명과 매핑
public class UserImageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    // UserImages table에 userId field가 생성되고, 그 값은 UserEntity의 id를 참조하는 FK가 됨
    @ManyToOne(fetch = FetchType.LAZY) // UserImage 입장에서 User는 하나
    @JoinColumn(name = "userId") // DB의 FK 컬럼명
    private UserEntity user;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isMainImage;

    @Builder
    public UserImageEntity(UserEntity user, String imageUrl, Boolean isMainImage) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.isMainImage = isMainImage;
    }

    // 대표 이미지 설정을 위한 편의 메서드
    public void setMainImage(Boolean isMain) {
        this.isMainImage = isMain;
    }
}