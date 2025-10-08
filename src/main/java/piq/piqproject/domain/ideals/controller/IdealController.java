package piq.piqproject.domain.ideals.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
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
     * @return 생성되 카테고리 및 옵션 리스트 
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ListResponseDto<IdealResponseDto>> createCategoriesWithOptions(
        @AuthenticationPrincipal UserEntity user,
        @Valid @RequestBody List<IdealRequestDto> idealRequestDtos
    ) {
        log.info("Request to create ideal categories and options.User: {}", user.getEmail());
        
        return ResponseEntity.status(201)
                        .body(idealService.createCategoriesWithOptions(idealRequestDtos));
    }
    
}
