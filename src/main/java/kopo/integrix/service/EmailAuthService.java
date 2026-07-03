package kopo.integrix.service;


/**
 * 이메일 인증 기능의 Service 계약입니다. 인증번호 발급, 메일 발송, 인증번호 검증 흐름을 정의합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.email.EmailAuthIssueResultDTO;
import kopo.integrix.dto.email.EmailAuthRequestDTO;
import kopo.integrix.dto.email.EmailVerifyRequestDTO;

public interface EmailAuthService {

    EmailAuthIssueResultDTO issueAuthCode(EmailAuthRequestDTO request);

    CommonResponseDTO verifyAuthCode(EmailVerifyRequestDTO request);

    boolean hasVerifiedEmail(String email, String type);

    void consumeVerifiedEmail(String email, String type);
}
