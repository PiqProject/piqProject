package piq.piqproject.domain.interests.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.interests.dto.response.InterestResponseDto;
import piq.piqproject.domain.interests.service.InterestService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interests")
public class InterestController {
    private final InterestService interestService;

    @GetMapping("/all")
    public ResponseEntity<ListResponseDto<InterestResponseDto>> getInterests() {
        log.info("Request to get an interest.");
        return ResponseEntity.ok(interestService.getInterests());
    }

}
