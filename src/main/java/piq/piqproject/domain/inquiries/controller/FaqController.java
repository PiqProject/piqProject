package piq.piqproject.domain.inquiries.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.inquiries.dto.response.FaqResponseDto;
import piq.piqproject.domain.inquiries.enums.InquiryCategory;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/faqs")
public class FaqController {

        /**
         * 자주 묻는 질문(FAQ) 조회
         * TODO: FAQ 질문리스트 제대로 만들기
         * - 서버 하드코딩 데이터 반환
         */
        @GetMapping
        public ResponseEntity<ListResponseDto<FaqResponseDto>> getFaqs() {
                List<FaqResponseDto> faqs = new ArrayList<>();

                // 1. 결제/환불
                faqs.add(new FaqResponseDto(InquiryCategory.PAYMENT,
                                "결제 취소 및 환불 규정이 어떻게 되나요?",
                                "결제일로부터 7일 이내이며, 지급된 포인트를 전혀 사용하지 않은 경우에 한해 전액 환불이 가능합니다. [마이페이지 > 결제내역]에서 취소 신청을 하실 수 있습니다."));

                // 2. 계정/로그인
                faqs.add(new FaqResponseDto(InquiryCategory.ACCOUNT,
                                "회원 탈퇴는 어떻게 하나요?",
                                "앱 내 [설정 > 계정 관리 > 회원 탈퇴] 메뉴를 통해 즉시 탈퇴하실 수 있습니다. 탈퇴 시 모든 데이터는 삭제되며 복구할 수 없습니다."));

                // 3. 신고/제재
                faqs.add(new FaqResponseDto(InquiryCategory.REPORT,
                                "매칭이 너무 안 돼요. 팁이 있나요?",
                                "프로필 사진은 얼굴이 선명하게 나온 사진일수록 매칭 확률이 올라갑니다. 또한 상세 자기소개를 성실하게 작성하면 신뢰도가 높아져 매칭 성공률이 30% 이상 증가합니다!"));
                // 4. 버그/오류 제보
                faqs.add(new FaqResponseDto(InquiryCategory.ETC,
                                "추천되는 이성의 기준이 뭔가요?",
                                "회원님이 설정하신 [이상형 조건(나이, 거리 등)]을 최우선으로 반영하며, 비슷한 관심사를 가진 이성을 우선적으로 추천해 드립니다."));

                // 5.건의사항
                faqs.add(new FaqResponseDto(InquiryCategory.ACCOUNT,
                                "회원 탈퇴는 어떻게 하나요?",
                                "앱 내 [설정 > 계정 관리 > 회원 탈퇴] 메뉴를 통해 즉시 탈퇴하실 수 있습니다. 탈퇴 시 모든 데이터는 삭제되며 복구할 수 없습니다."));

                // 6. 기타
                faqs.add(new FaqResponseDto(InquiryCategory.ETC,
                                "비매너 유저를 신고하고 싶어요.",
                                "상대방 프로필 우측 상단의 [🚨] 버튼을 눌러 신고할 수 있습니다. 신고 내용은 관리자가 24시간 이내에 확인 후 조치합니다."));

                return ResponseEntity.ok(ListResponseDto.from(faqs));
        }
}
