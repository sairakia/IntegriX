package kopo.integrix.service;


/**
 * 사용자 기능의 Service 계약입니다. 회원가입, 로그인, 사용자 조회, 비밀번호 변경, 프로필 이미지, 회원탈퇴 기능이 어떤 메서드로 제공되는지 정의합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.LoginRequestDTO;
import kopo.integrix.dto.PasswordResetRequestDTO;
import kopo.integrix.dto.SignupRequestDTO;
import kopo.integrix.entity.UserInfoEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface UserService {

    CommonResponseDTO login(LoginRequestDTO pDTO);

    CommonResponseDTO signup(SignupRequestDTO pDTO);

    CommonResponseDTO getCurrentUser(String userId);

    CommonResponseDTO updateProfileImage(String userId, MultipartFile profileImage);

    CommonResponseDTO changePassword(String userId, String currentPassword, String newPassword);

    CommonResponseDTO deleteAccount(String userId, String password);

    CommonResponseDTO signupAfterEmailVerified(SignupRequestDTO pDTO, Boolean emailVerified, String authEmail);

    CommonResponseDTO findIdAfterEmailVerified(Boolean emailVerified, String authEmail, String authType);

    CommonResponseDTO resetPasswordAfterEmailVerified(PasswordResetRequestDTO request,
                                                      Boolean emailVerified,
                                                      String authEmail,
                                                      String authType);

    boolean existsUserId(String userId);

    boolean existsEmail(String email);

    Optional<UserInfoEntity> findByEmail(String email);

    Optional<UserInfoEntity> findByUserId(String userId);

    void updatePassword(String userId, String email, String password);
}
