package kopo.integrix.config;


/**
 * 비밀번호 암호화에 사용할 PasswordEncoder를 Bean으로 등록하는 설정 파일입니다. BCryptPasswordEncoder를 통해 비밀번호를 단방향 해시로 저장합니다.
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원가입과 비밀번호 재설정 시 평문 비밀번호를 BCrypt 해시로 저장하기 위한 공통 Bean입니다.
        return new BCryptPasswordEncoder();
    }
}
