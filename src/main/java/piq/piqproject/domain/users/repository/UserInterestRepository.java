package piq.piqproject.domain.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.interests.entity.InterestEntity;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.entity.UserInterestEntity;

public interface UserInterestRepository extends JpaRepository<UserInterestEntity, Long> {

    void deleteAllByInterest(InterestEntity interest);

    boolean existsByUser(UserEntity user);

} 