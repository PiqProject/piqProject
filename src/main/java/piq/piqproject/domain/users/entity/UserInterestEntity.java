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
import piq.piqproject.domain.interests.entity.InterestEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
@Table(name = "user_interests")
public class UserInterestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_id")
    private InterestEntity interest; 

    @Builder
    private UserInterestEntity(UserEntity user, InterestEntity interest) {
        this.user = user;
        this.interest = interest;
    }

    public static UserInterestEntity of(UserEntity user, InterestEntity interest) {
        return UserInterestEntity.builder()
                .user(user)
                .interest(interest)
                .build();
    }
}
