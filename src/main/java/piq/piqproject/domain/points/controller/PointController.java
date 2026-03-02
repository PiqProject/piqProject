package piq.piqproject.domain.points.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.points.dto.response.PointHistoryResponseDto;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointService pointService;

    @GetMapping("/history")
    public ResponseEntity<ListResponseDto<PointHistoryResponseDto>> getPointHistories(
            @AuthenticationPrincipal UserEntity user) {
        log.info("Request to get point histories. User: {}", user.getUsername());

        ListResponseDto<PointHistoryResponseDto> response = pointService.getPointHistories(user);
        return ResponseEntity.ok(response);
    }
}
