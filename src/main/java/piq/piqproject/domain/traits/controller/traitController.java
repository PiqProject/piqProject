package piq.piqproject.domain.traits.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.traits.dto.response.TraitResponseDto;
import piq.piqproject.domain.traits.service.TraitService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/traits")
public class traitController {
    private final TraitService traitService;

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

}
