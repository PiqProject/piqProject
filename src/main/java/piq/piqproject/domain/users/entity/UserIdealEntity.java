package piq.piqproject.domain.users.entity;

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
import piq.piqproject.domain.traits.entity.TraitOptionEntity;

@Entity
@Table(name = "user_ideals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdealEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private TraitOptionEntity idealOption;

    @Builder
    private UserIdealEntity(UserEntity user, TraitOptionEntity idealOption) {
        this.user = user;
        this.idealOption = idealOption;
    }

    public static UserIdealEntity of(UserEntity user, TraitOptionEntity idealOption) {
        return UserIdealEntity.builder()
                .user(user)
                .idealOption(idealOption)
                .build();
    }
}
