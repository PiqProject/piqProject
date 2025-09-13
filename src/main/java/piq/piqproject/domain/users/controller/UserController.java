package piq.piqproject.domain.users.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.users.dto.MyProfileResponseDto;
import piq.piqproject.domain.users.dto.UserProfileResponseDto;
import piq.piqproject.domain.users.dto.UserSimpleProfileResponseDto;
import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 성별에 따라 필터링된 전체 사용자 프로필 목록을 조회합니다.
     * 
     * @param Gender "MALE" 또는 "FEMALE"
     * @return 프로필 DTO 목록
     */
    @GetMapping("/profiles")
    public ResponseEntity<ListResponseDto<UserSimpleProfileResponseDto>> getAllProfiles(
            @RequestParam("gender") Gender gender) {
        List<UserSimpleProfileResponseDto> profiles = userService.findAllProfilesByGender(gender);
        return ResponseEntity.ok(ListResponseDto.from(profiles));
    }

    /**
     * 현재 로그인된 사용자의 상세 프로필을 조회합니다.
     * 
     * @param userEntity @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @return 현재 사용자의 상세 프로필 정보 (MyProfileResponseDto)
     */
    @GetMapping("/profiles/me")
    public ResponseEntity<MyProfileResponseDto> getMyProfile(@AuthenticationPrincipal UserEntity userEntity) {
        MyProfileResponseDto myProfile = userService.findMyProfile(userEntity);
        return ResponseEntity.ok(myProfile);
    }

    /**
     * 특정 ID를 가진 사용자의 공개 프로필을 조회합니다.
     *
     * @param id 조회할 사용자의 PK (Long)
     * @return 특정 사용자의 프로필 정보
     */
    @GetMapping("/profiles/{id}")
    public ResponseEntity<UserProfileResponseDto> getUserProfile(@PathVariable("id") Long id) {
        // 1. 서비스를 호출하여 비즈니스 로직을 수행하고 DTO를 받습니다.
        UserProfileResponseDto userProfile = userService.findUserProfileById(id);

        // 2. 성공 응답(200 OK)과 함께 DTO를 ResponseEntity에 담아 반환합니다.
        return ResponseEntity.ok(userProfile);
    }

    /**
     * 현재 로그인된 사용자 계정을 삭제(탈퇴)합니다.
     * 
     * @param userEntity @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @return 성공 메시지
     */
    @PostMapping("/delete") // 이미지에 명시된 POST 메서드와 경로로 수정
    public ResponseEntity<String> deleteMyAccount(@AuthenticationPrincipal UserEntity userEntity) {
        userService.deleteUser(userEntity); // 서비스 계층에 사용자 엔티티를 직접 전달
        return ResponseEntity.ok("회원 탈퇴가 성공적으로 처리되었습니다.");
    }

}
