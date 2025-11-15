package piq.piqproject.domain.traits.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.traits.dto.request.CreateTraitOptionRequestDto;
import piq.piqproject.domain.traits.dto.request.DeleteTraitOptionRequestDto;
import piq.piqproject.domain.traits.dto.request.TraitRequestDto;
import piq.piqproject.domain.traits.dto.response.TraitResponseDto;
import piq.piqproject.domain.traits.service.TraitService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/traits")
public class traitController {
    private final TraitService traitService;

    /**
     * 이상형 카테고리와 하위 옵션들을 생성하는 API입니다.
     * 
     * @body 생성할 카테고리와 하위 옵션을 리스트 형태로 받아옵니다.
     * @return 생성된 카테고리 및 옵션 리스트
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ListResponseDto<TraitResponseDto>> createCategoriesWithOptions(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody List<TraitRequestDto> traitRequestDtos) {
        log.info("Request to create trait categories and options.User: {}", user.getEmail());

        return ResponseEntity.status(201)
                .body(traitService.createCategoriesWithOptions(traitRequestDtos));
    }

    /**
     * 이상형 카테고리의 하위 옵션들을 생성하는 API입니다.
     * 
     * @param categoryId 이상형 카테고리 아이디
     * @body 카테고리에 추가될 하위 옵션을 리스트 형태로 받아옵니다.
     * @return 생성된 카테고리의 옵션 리스트
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{categoryId}/options")
    public ResponseEntity<TraitResponseDto> addOptions(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody CreateTraitOptionRequestDto createTraitOptionRequestDto) {
        log.info("Request to add options for trait category {}. User: {}", categoryId, user.getEmail());

        return ResponseEntity.status(201)
                .body(traitService.addOptions(categoryId, createTraitOptionRequestDto));
    }

    /**
     * 이상형 카테고리와 하위 옵션들을 조회하는 API입니다.
     * 
     * @return 저장되어 있는 카테고리와 하위 옵션 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<ListResponseDto<TraitResponseDto>> getTraits() {
        log.info("Request to get categories and options for trait ");

        return ResponseEntity.ok(traitService.getTraits());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/options/delete")
    public ResponseEntity<String> deleteOptions(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody DeleteTraitOptionRequestDto deleteTraitOptionRequestDto) {
        log.info("Request to delete options for trait User: {}", user.getEmail());
        traitService.deleteOptions(deleteTraitOptionRequestDto);

        return ResponseEntity.ok("카테고리 내 옵션이 삭제되었습니다.");
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{categoryId}/delete")
    public ResponseEntity<String> deleteCategoey(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("categoryId") Long categoryId) {
        log.info("Request to delete category for trait CategoryId: {} User: {}", categoryId, user.getEmail());
        traitService.deleteCategory(categoryId);

        return ResponseEntity.ok("카테고리 및 하위 옵션들이 삭제되었습니다.");
    }
}
