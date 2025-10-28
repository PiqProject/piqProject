package piq.piqproject.domain.ideals.controller;

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
import piq.piqproject.domain.ideals.dto.request.CreateIdealOptionRequestDto;
import piq.piqproject.domain.ideals.dto.request.DeleteIdealOptionRequestDto;
import piq.piqproject.domain.ideals.dto.request.IdealRequestDto;
import piq.piqproject.domain.ideals.dto.response.IdealResponseDto;
import piq.piqproject.domain.ideals.service.IdealService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ideals")
public class IdealController {
    private final IdealService idealService;

    /**
     * 이상형 카테고리와 하위 옵션들을 생성하는 API입니다.
     * 
     * @body 생성할 카테고리와 하위 옵션을 리스트 형태로 받아옵니다.
     * @return 생성된 카테고리 및 옵션 리스트
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ListResponseDto<IdealResponseDto>> createCategoriesWithOptions(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody List<IdealRequestDto> idealRequestDtos) {
        log.info("Request to create ideal categories and options.User: {}", user.getEmail());

        return ResponseEntity.status(201)
                .body(idealService.createCategoriesWithOptions(idealRequestDtos));
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
    public ResponseEntity<IdealResponseDto> addOptions(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody CreateIdealOptionRequestDto createIdealOptionRequestDto) {
        log.info("Request to add options for ideal category {}. User: {}", categoryId, user.getEmail());

        return ResponseEntity.status(201)
                .body(idealService.addOptions(categoryId, createIdealOptionRequestDto));
    }

    /**
     * 이상형 카테고리와 하위 옵션들을 조회하는 API입니다.
     * 
     * @return 저장되어 있는 카테고리와 하위 옵션 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<ListResponseDto<IdealResponseDto>> getIdeals() {
        log.info("Request to get categies and options for ideal ");

        return ResponseEntity.ok(idealService.getIdeals());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/options/delete")
    public ResponseEntity<String> deleteOptions(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody DeleteIdealOptionRequestDto deleteIdealOptionResponseDto) {
        log.info("Request to delete options for ideal User: {}", user.getEmail());
        idealService.deleteOptions(deleteIdealOptionResponseDto);

        return ResponseEntity.ok("카테고리 내 옵션이 삭제되었습니다.");
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{categoryId}/delete")
    public ResponseEntity<String> deleteCategoey(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("categoryId") Long categoryId) {
        log.info("Request to delete category for ideal CategoryId: {} User: {}", categoryId, user.getEmail());
        idealService.deleteCategoey(categoryId);

        return ResponseEntity.ok("카테고리 및 하위 옵션들이 삭제되었습니다.");
    }
}
