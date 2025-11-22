package piq.piqproject.domain.traits.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.users.entity.UserIdealEntity;
import piq.piqproject.domain.users.entity.UserTraitEntity;

@Getter
@Entity
@Table(name = "trait_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TraitOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TraitCategoryEntity category;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "idealOption", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserIdealEntity> userIdeals = new ArrayList<>();

    /**
     * 이 특성 옵션을 '자신의 특성'으로 보유한 모든 사용자-특성 관계 목록입니다.
     * 이 TraitOption이 삭제되면, 관련된 모든 UserTraitEntity도 함께 삭제됩니다.
     */
    @OneToMany(mappedBy = "traitOption", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserTraitEntity> userTraits = new ArrayList<>();

    @Builder
    private TraitOptionEntity(TraitCategoryEntity category, String name) {
        this.category = category;
        this.name = name;
    }

    public static TraitOptionEntity of(TraitCategoryEntity category, String name) {
        return TraitOptionEntity.builder()
                .category(category)
                .name(name)
                .build();
    }
}
