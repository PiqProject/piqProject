package piq.piqproject.domain.alarms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import piq.piqproject.domain.alarms.entity.DeviceTokenEntity;
import piq.piqproject.domain.users.entity.UserEntity;

import java.util.Optional;
import java.util.List;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, Long> {
    Optional<DeviceTokenEntity> findByToken(String token);

    Optional<DeviceTokenEntity> findByUserAndDeviceType(UserEntity user, String deviceType);

    List<DeviceTokenEntity> findByUserId(Long userId);

    List<DeviceTokenEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<DeviceTokenEntity> findByUserIdAndToken(Long userId, String token);
}
