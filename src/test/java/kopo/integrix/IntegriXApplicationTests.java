package kopo.integrix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "DB_URL=jdbc:mariadb://localhost:3306/integrix_test",
        "DB_USERNAME=test",
        "DB_PASSWORD=test",
        "MONGODB_URI=mongodb://localhost:27017/integrix_test",
        "MAIL_USERNAME=test@example.com",
        "MAIL_PASSWORD=test",
        "JWT_SECRET=test-jwt-secret-at-least-32-bytes-long",
        "REDIS_HOST=localhost",
        "TEXT_LLM_ENABLED=false",
        "IMAGE_LLM_ENABLED=false"
})
class IntegriXApplicationTests {

    @Test
    void contextLoads() {
    }

}
