package piq.piqproject.domain.search.document;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch의 'users' 인덱스에 저장될 문서(Document) 클래스입니다.
 * 추천 시스템에서 필터링 및 점수 계산에 필요한 핵심 데이터만 비정규화하여 저장합니다.
 * index == table,document == tuple,column == field, schema == mapping
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "users") // Elasticsearch 인덱스(Table) 이름 지정
public class UserDocument {

        @Id // Elasticsearch 문서의 고유 ID (RDB의 User ID와 동일하게 사용)
        @Field(type = FieldType.Keyword)
        private String id;

        // RDB의 PK (Long)를 별도 필드로도 저장 (쿼리 편의성 및 @Id가 String인 점 고려)
        @Field(type = FieldType.Long)
        private Long userId;

        @Field(type = FieldType.Keyword) // Keyword: 형태소 분석 없이 정확한 값 매칭 (Filter용)
        private Gender gender;

        @Field(type = FieldType.Integer)
        private Integer age;

        @Field(type = FieldType.Keyword)
        private String mbti;

        @Field(type = FieldType.Boolean)
        private Boolean isActive;

        // --- ▼ 핵심: 비정규화된 리스트 데이터 (JOIN 대체) ▼ ---

        @Field(type = FieldType.Long)
        private List<Long> interestIds; // 사용자의 관심사 ID 목록

        @Field(type = FieldType.Long)
        private List<Long> traitOptionIds; // 사용자가 가진 특성 ID 목록

        // --- ▲ 핵심: 비정규화된 리스트 데이터 (JOIN 대체) ▲ ---

        @Builder
        public UserDocument(Long userId, Gender gender, Integer age, String mbti, Boolean isActive,
                        List<Long> interestIds, List<Long> traitOptionIds) {
                this.id = String.valueOf(userId); // @Id는 String으로 관리하는 것이 일반적
                this.userId = userId;
                this.gender = gender;
                this.age = age;
                this.mbti = mbti;
                this.isActive = isActive;
                this.interestIds = interestIds;
                this.traitOptionIds = traitOptionIds;
        }

        /**
         * RDB의 UserEntity를 기반으로 UserDocument를 생성하는 정적 팩토리 메서드
         * (데이터 동기화 시 사용)
         */
        public static UserDocument from(UserEntity user) {
                return UserDocument.builder()
                                .userId(user.getId())
                                .gender(user.getGender())
                                .age(user.getAge())
                                .mbti(user.getMbti())
                                .isActive(user.isEnabled()) // isEnabled()가 isActive 필드 반환
                                // Lazy Loading 발생 지점 (Transactional 필요)
                                .interestIds(user.getUserInterests().stream()
                                                .map(ui -> ui.getInterest().getId())
                                                .collect(Collectors.toList()))
                                .traitOptionIds(user.getUserTraits().stream()
                                                .map(ut -> ut.getTraitOption().getId())
                                                .collect(Collectors.toList()))
                                .build();
        }
}