package piq.piqproject.domain.search.repository.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import piq.piqproject.domain.search.document.UserDocument;

/**
 * Elasticsearch의 'users' 인덱스에 접근하기 위한 리포지토리입니다.
 * 기본적인 CRUD (save, findById, delete 등) 메서드를 제공합니다.
 * 
 * 제네릭 타입:
 * - UserDocument: 다루는 엔티티(문서)
 * - String: @Id 필드의 타입 (UserDocument의 id가 String이므로)
 */
@Repository
public interface UserDocumentRepository extends ElasticsearchRepository<UserDocument, String> {

    // 기본적인 CRUD는 자동으로 제공됩니다.
    // 필요하다면 findByGender(Gender gender) 처럼 쿼리 메서드를 추가할 수 있습니다.
}