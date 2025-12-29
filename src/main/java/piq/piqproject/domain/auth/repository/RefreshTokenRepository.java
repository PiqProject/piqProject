package piq.piqproject.domain.auth.repository;

// RefreshTokenRepository.java
import org.springframework.data.repository.CrudRepository;

import piq.piqproject.domain.auth.entity.RefreshTokenEntity;

//Redis와 통신하기 위한 Repository 인터페이스
public interface RefreshTokenRepository extends CrudRepository<RefreshTokenEntity, String> {
}