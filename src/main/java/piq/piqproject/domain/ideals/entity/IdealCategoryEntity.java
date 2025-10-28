package piq.piqproject.domain.ideals.entity;

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
@Table(name = "ideal_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdealCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IdealOptionEntity> options = new ArrayList<>();

    @Builder
    private IdealCategoryEntity (String name) {
        this.name = name;
    }

    public static IdealCategoryEntity of(String name) {
        return IdealCategoryEntity.builder()
                        .name(name)
                        .build();
    }
}
