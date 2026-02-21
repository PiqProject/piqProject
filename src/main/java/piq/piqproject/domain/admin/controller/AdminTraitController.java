package piq.piqproject.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.annotation.AuditLog;
import piq.piqproject.domain.traits.dto.request.CreateTraitOptionRequestDto;
import piq.piqproject.domain.traits.dto.request.DeleteTraitOptionRequestDto;
import piq.piqproject.domain.traits.dto.request.TraitRequestDto;
import piq.piqproject.domain.traits.dto.response.TraitResponseDto;
import piq.piqproject.domain.traits.service.TraitService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 관리자 전용 - Trait(이상형) 관리 API
 * 법적 요구사항에 따라 관리자의 모든 행위를 중앙에서 관리하고 로깅하기 위한 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/traits")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminTraitController {
    private final TraitService traitService;

    /**
     * 하나의 이상형 카테고리와 하위 옵션들을 생성하는 API입니다.
     * 
     * @body 생성할 카테고리와 하위 옵션을 리스트 형태로 받아옵니다.
     * @return 생성된 카테고리 및 옵션 리스트
     */
    @PostMapping
    @AuditLog(action = "이상형 카테고리 하나와 옵션들 생성")
    public ResponseEntity<TraitResponseDto> createCategorieWithOptions(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody TraitRequestDto traitRequestDtos) {
        log.info("Request to create trait categories and options. User: {}", user.getEmail());

        return ResponseEntity.status(201)
                .body(traitService.createCategorieWithOptions(traitRequestDtos));
    }

    /**
     * 이상형 카테고리의 하위 옵션들을 생성하는 API입니다.
     * 
     * @param categoryId 이상형 카테고리 아이디
     * @body 카테고리에 추가될 하위 옵션을 리스트 형태로 받아옵니다.
     * @return 생성된 카테고리의 옵션 리스트
     */
    @PostMapping("/{categoryId}/options")
    @AuditLog(action = "이상형 카테고리 옵션 생성")
    public ResponseEntity<TraitResponseDto> addOptions(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody CreateTraitOptionRequestDto createTraitOptionRequestDto) {
        log.info("Request to add options for trait category {}. User: {}", categoryId, user.getEmail());

        return ResponseEntity.status(201)
                .body(traitService.addOptions(categoryId, createTraitOptionRequestDto));
    }

    /**
     * 이상형 카테고리 내 옵션을 삭제하는 API입니다.
     * 
     * @body 삭제할 옵션 정보
     * @return 성공 메시지
     */
    @PostMapping("/options/delete")
    @AuditLog(action = "이상형 카테고리의 옵션 삭제")
    public ResponseEntity<String> deleteOption(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody DeleteTraitOptionRequestDto deleteTraitOptionRequestDto) {
        log.info("Request to delete option for trait User: {}", user.getEmail());
        traitService.deleteOption(deleteTraitOptionRequestDto);

        return ResponseEntity.ok("카테고리 내 옵션이 삭제되었습니다.");
    }

    /**
     * 이상형 카테고리 및 하위 옵션들을 삭제하는 API입니다.
     * 
     * @param categoryId 삭제할 카테고리 ID
     * @return 성공 메시지
     */
    @PostMapping("/{categoryId}/delete")
    @AuditLog(action = "이상형 카테고리 삭제")
    public ResponseEntity<String> deleteCategory(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("categoryId") Long categoryId) {
        log.info("Request to delete category for trait CategoryId: {} User: {}", categoryId, user.getEmail());
        traitService.deleteCategory(categoryId);

        return ResponseEntity.ok("카테고리 및 하위 옵션들이 삭제되었습니다.");
    }
}
