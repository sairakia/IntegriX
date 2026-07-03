package kopo.integrix.service.impl;


/**
 * 이메일 인증 Service 구현체입니다. 인증번호 생성, Redis 저장, 메일 발송, 인증번호 검증을 처리합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.email.EmailAuthIssueResultDTO;
import kopo.integrix.dto.email.EmailAuthRequestDTO;
import kopo.integrix.dto.email.EmailVerifyRequestDTO;
import kopo.integrix.entity.UserInfoEntity;
import kopo.integrix.service.EmailAuthService;
import kopo.integrix.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailAuthServiceImpl implements EmailAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long AUTH_CODE_TTL_MILLIS = 5 * 60 * 1000;
    private static final long VERIFIED_TTL_MILLIS = 30 * 60 * 1000;
    private static final String CODE_PREFIX = "email:code:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String TYPE_FIND_ID = "FIND_ID";
    private static final String TYPE_RESET_PASSWORD = "RESET_PASSWORD";

    private final JavaMailSender mailSender;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromMail;

    @Override
    public EmailAuthIssueResultDTO issueAuthCode(EmailAuthRequestDTO request) {
        String email = request.email();
        String userId = request.userId();
        String type = request.type();

        if (isBlank(email)) {
            return failure("이메일을 입력해주세요.");
        }

        if (isBlank(type)) {
            return failure("인증 유형을 확인할 수 없습니다.");
        }

        if (TYPE_RESET_PASSWORD.equals(type)) {
            CommonResponseDTO validation = validateResetPasswordRequest(userId, email);
            if (!validation.success()) {
                return failure(validation.message());
            }
        }

        if (TYPE_FIND_ID.equals(type) && userService.findByEmail(email)
                .filter(user -> STATUS_ACTIVE.equals(user.getStatus()))
                .isEmpty()) {
            return failure("해당 이메일로 가입된 계정이 없습니다.");
        }

        String code = createCode();

        try {
            mailSender.send(createMailMessage(email, type, code));
        } catch (Exception e) {
            return failure("인증 이메일 전송에 실패했습니다.");
        }

        redisTemplate.opsForValue().set(
                codeKey(type, email),
                passwordEncoder.encode(code),
                Duration.ofMillis(AUTH_CODE_TTL_MILLIS)
        );

        return new EmailAuthIssueResultDTO(
                true,
                "인증 코드가 전송되었습니다.",
                email,
                null,
                type,
                TYPE_RESET_PASSWORD.equals(type) ? userId : null,
                System.currentTimeMillis() + AUTH_CODE_TTL_MILLIS
        );
    }

    @Override
    public CommonResponseDTO verifyAuthCode(EmailVerifyRequestDTO request) {
        if (isBlank(request.email()) || isBlank(request.code()) || isBlank(request.type())) {
            return new CommonResponseDTO(false, "인증 정보를 모두 입력해주세요.", null);
        }

        String savedCodeHash = redisTemplate.opsForValue().get(codeKey(request.type(), request.email()));
        if (savedCodeHash == null) {
            return new CommonResponseDTO(false, "인증 코드가 만료되었습니다. 새 인증 코드를 요청해주세요.", null);
        }

        if (!passwordEncoder.matches(request.code(), savedCodeHash)) {
            return new CommonResponseDTO(false, "인증 코드가 올바르지 않습니다.", null);
        }

        redisTemplate.delete(codeKey(request.type(), request.email()));
        redisTemplate.opsForValue().set(
                verifiedKey(request.type(), request.email()),
                "true",
                Duration.ofMillis(VERIFIED_TTL_MILLIS)
        );

        return new CommonResponseDTO(true, "이메일 인증이 완료되었습니다.", null);
    }

    @Override
    public boolean hasVerifiedEmail(String email, String type) {
        return !isBlank(email)
                && !isBlank(type)
                && Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(type, email)));
    }

    @Override
    public void consumeVerifiedEmail(String email, String type) {
        redisTemplate.delete(verifiedKey(type, email));
    }

    private CommonResponseDTO validateResetPasswordRequest(String userId, String email) {
        if (isBlank(userId)) {
            return new CommonResponseDTO(false, "아이디를 입력해주세요.", null);
        }

        Optional<UserInfoEntity> rEntity = userService.findByUserId(userId);
        if (rEntity.isEmpty()) {
            return new CommonResponseDTO(false, "일치하는 계정이 없습니다.", null);
        }

        UserInfoEntity user = rEntity.get();
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            return new CommonResponseDTO(false, "비활성화된 계정입니다.", null);
        }

        if (!user.getEmail().equals(email)) {
            return new CommonResponseDTO(false, "아이디와 이메일이 일치하지 않습니다.", null);
        }

        return new CommonResponseDTO(true, "인증할 수 있는 계정입니다.", null);
    }

    private SimpleMailMessage createMailMessage(String email, String type, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromMail);
        message.setTo(email);

        if (TYPE_FIND_ID.equals(type)) {
            message.setSubject("[IntegriX] 아이디 찾기 인증 코드");
            message.setText("아이디 찾기 인증 코드는 [" + code + "] 입니다.\n5분 이내에 입력해주세요.");
        } else if (TYPE_RESET_PASSWORD.equals(type)) {
            message.setSubject("[IntegriX] 비밀번호 재설정 인증 코드");
            message.setText("비밀번호 재설정 인증 코드는 [" + code + "] 입니다.\n5분 이내에 입력해주세요.");
        } else {
            message.setSubject("[IntegriX] 이메일 인증 코드");
            message.setText("이메일 인증 코드는 [" + code + "] 입니다.\n5분 이내에 입력해주세요.");
        }

        return message;
    }

    private String createCode() {
        return String.valueOf(SECURE_RANDOM.nextInt(900000) + 100000);
    }

    private EmailAuthIssueResultDTO failure(String message) {
        return new EmailAuthIssueResultDTO(false, message, null, null, null, null, 0);
    }

    private String codeKey(String type, String email) {
        return CODE_PREFIX + type + ":" + email;
    }

    private String verifiedKey(String type, String email) {
        return VERIFIED_PREFIX + type + ":" + email;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
