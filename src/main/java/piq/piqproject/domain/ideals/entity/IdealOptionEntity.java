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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.users.entity.UserIdealEntity;

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
    private IdealCategoryEntity category;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "idealOption", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserIdealEntity> userIdeals = new ArrayList<>();

    @Builder 
    private IdealOptionEntity (IdealCategoryEntity category, String name) {
        this.category = category;
        this.name = name;
    }

    public static IdealOptionEntity of (IdealCategoryEntity category, String name) {
        return IdealOptionEntity.builder()
                        .category(category)
                        .name(name) 
                        .build();
    }
}
