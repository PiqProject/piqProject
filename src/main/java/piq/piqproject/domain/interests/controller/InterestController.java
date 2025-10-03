package piq.piqproject.domain.interests.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.interests.dto.request.InterestRequestDto;
import piq.piqproject.domain.interests.dto.response.InterestResponseDto;
import piq.piqproject.domain.interests.service.InterestService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interests")
public class InterestController {
    private final InterestService interestService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<InterestResponseDto> createInterest(
        @AuthenticationPrincipal UserEntity user,
        @Valid @RequestBody InterestRequestDto interestRequestDto 
    ) {
        log.info("Request to create an interest. User: {}", user.getEmail());
        return ResponseEntity.status(201)
                                .body(interestService.createInterest(interestRequestDto));
    }

    @GetMapping("/all")
    public ResponseEntity<ListResponseDto<InterestResponseDto>> getInterests() {
        log.info("Request to get an interest.");
        return ResponseEntity.ok(interestService.getInterests());
    }

}
