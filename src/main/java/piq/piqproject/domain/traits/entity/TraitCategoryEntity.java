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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "trait_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TraitCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TraitOptionEntity> options = new ArrayList<>();

    @Builder
    private TraitCategoryEntity(String name) {
        this.name = name;
    }

    public static TraitCategoryEntity of(String name) {
        return TraitCategoryEntity.builder()
                .name(name)
                .build();
    }
}
