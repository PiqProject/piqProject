package piq.piqproject.domain.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.users.dto.SignUpRequestDto;
import piq.piqproject.domain.users.service.UserService;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 컨트롤러 메소드의 파라미터 앞에 @Valid 어노테이션을 붙이면, Spring Boot가 자동으로 이 규칙들을 검사하고, 위반 시
    // MethodArgumentNotValidException을 발생시켜 400 Bad Request 에러를 응답
    @PostMapping("/api/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {

        userService.signUp(signUpRequestDto);

        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }
}