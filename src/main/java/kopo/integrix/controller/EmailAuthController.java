package kopo.integrix.controller;


/**
 * 이메일 인증 요청을 받는 Controller입니다. 인증번호 발송과 인증번호 확인 API를 EmailAuthService로 전달합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.email.EmailAuthIssueResultDTO;
import kopo.integrix.dto.email.EmailAuthRequestDTO;
import kopo.integrix.dto.email.EmailVerifyRequestDTO;
import kopo.integrix.service.EmailAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/email")
public class EmailAuthController {

    private final EmailAuthService emailAuthService;

    @PostMapping("/send")
    public Map<String, Object> sendAuthCode(@RequestBody EmailAuthRequestDTO request) {
        EmailAuthIssueResultDTO result = emailAuthService.issueAuthCode(request);
        return response(result.success(), result.message());
    }

    @PostMapping("/verify")
    public Map<String, Object> verifyAuthCode(@RequestBody EmailVerifyRequestDTO request) {
        CommonResponseDTO result = emailAuthService.verifyAuthCode(request);
        return response(result.success(), result.message());
    }

    private Map<String, Object> response(boolean success, String message) {
        return Map.of(
                "success", success,
                "message", message
        );
    }
}
