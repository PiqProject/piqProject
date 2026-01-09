package piq.piqproject.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.interests.dto.request.InterestRequestDto;
import piq.piqproject.domain.interests.dto.response.InterestResponseDto;
import piq.piqproject.domain.interests.service.InterestService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 관리자 전용 - Interest(관심사) 관리 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/interests")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminInterestController {
    private final InterestService interestService;

    /**
     * 관심사를 생성하는 API입니다.
     * 
     * @param user               인증된 관리자 정보
     * @param interestRequestDto 관심사 생성 요청 데이터
     * @return 생성된 관심사 정보
     */
    @PostMapping
    public ResponseEntity<InterestResponseDto> createInterest(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody InterestRequestDto interestRequestDto) {
        log.info("Request to create an interest. User: {}", user.getEmail());
        return ResponseEntity.status(201)
                .body(interestService.createInterest(interestRequestDto));
    }

    /**
     * 관심사를 수정하는 API입니다.
     * 
     * @param user               인증된 관리자 정보
     * @param interestId         수정할 관심사 ID
     * @param interestRequestDto 관심사 수정 요청 데이터
     * @return 수정된 관심사 정보
     */
    @PutMapping("/{interestId}")
    public ResponseEntity<InterestResponseDto> updateInterest(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("interestId") Long interestId,
            @Valid @RequestBody InterestRequestDto interestRequestDto) {
        log.info("Request to update an interest. User: {} interestId: {}", user.getEmail(), interestId);
        return ResponseEntity.ok(interestService.updateInterest(interestId, interestRequestDto));
    }

    /**
     * 관심사를 삭제하는 API입니다.
     * 
     * @param user       인증된 관리자 정보
     * @param interestId 삭제할 관심사 ID
     * @return 성공 메시지
     */
    @PostMapping("/{interestId}/delete")
    public ResponseEntity<String> deleteInterest(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("interestId") Long interestId) {
        log.info("Request to delete an interest. User: {} interestId: {}", user.getEmail(), interestId);
        interestService.deleteInterest(interestId);
        return ResponseEntity.ok("관심사 키워드 삭제에 성공하였습니다.");
    }
}
