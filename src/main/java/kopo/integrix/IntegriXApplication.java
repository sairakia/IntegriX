package kopo.integrix;


/**
 * Spring Boot 애플리케이션의 시작점입니다. main 메서드와 @SpringBootApplication을 통해 컴포넌트 스캔, 자동 설정, 내장 서버 실행이 시작됩니다.
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IntegriXApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegriXApplication.class, args);
    }

}
