package piq.piqproject.domain.ideals.entity;

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

@Getter
@Entity
@Table(name = "ideal_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdealOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private IdealCategoryEntity idealCategory;

    @Column(nullable = false)
    private String name;

    @Builder 
    private IdealOptionEntity (IdealCategoryEntity idealCategory, String name) {
        this.idealCategory = idealCategory;
        this.name = name;
    }

    public static IdealOptionEntity of (IdealCategoryEntity idealCategory, String name) {
        return IdealOptionEntity.builder()
                        .idealCategory(idealCategory)
                        .name(name) 
                        .build();
    }
}
