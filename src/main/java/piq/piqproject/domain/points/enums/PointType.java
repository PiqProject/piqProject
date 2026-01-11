package piq.piqproject.domain.points.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointType {
    CHARGE("충전"), // 돈 내고 충전
    USE("사용"), // 매칭/아이템 구매 등 사용
    REFUND("환불"), // 결제 취소로 인한 환불
    EVENT("이벤트"), // 관리자가 무료로 준 포인트
    ADMIN("관리자조정"); // 관리자 수동 차감/지급

    private final String description;
}