package piq.piqproject.domain.inquiries.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InquiryCategory {
    PAYMENT("결제/환불"),
    ACCOUNT("계정/로그인"),
    REPORT("신고/제재"),
    BUG("버그/오류 제보"),
    ETC("기타");

    private final String description;
}