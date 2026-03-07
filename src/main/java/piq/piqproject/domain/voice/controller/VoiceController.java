package piq.piqproject.domain.voice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.voice.service.VoiceService;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/voice")
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadVoice(
            @AuthenticationPrincipal UserEntity user,
            @RequestPart("voiceFile") MultipartFile voiceFile) {

        String voiceUrl = voiceService.uploadVoice(user, voiceFile);
        return ResponseEntity.ok(Map.of(
                "message", "Voice uploaded successfully.",
                "voiceUrl", voiceUrl
        ));
    }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteVoice(
            @AuthenticationPrincipal UserEntity user) {

        voiceService.deleteVoice(user);
        log.info("Voice deleted successfully.(DB)");
        return ResponseEntity.ok("Voice deleted successfully.");
    }
}