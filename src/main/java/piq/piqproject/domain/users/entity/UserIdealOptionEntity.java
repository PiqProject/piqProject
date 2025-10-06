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
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.ideals.entity.IdealOptionEntity;

@Entity
@Table(name = "user_ideal_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdealOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private IdealOptionEntity idealOption; 
}
