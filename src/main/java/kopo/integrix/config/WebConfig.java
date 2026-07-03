package kopo.integrix.config;


/**
 * 웹 MVC 관련 설정 파일입니다. 외부 업로드 디렉터리를 /uploads/** URL로 접근할 수 있게 리소스 매핑을 설정합니다.
 */
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns}")
    private String allowedOriginPatterns;

    @Value("${upload.profile-dir}")
    private String profileUploadDir;

    @Value("${upload.profile-url-path}")
    private String profileUrlPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);

        if (origins.length == 0) {
            return;
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String handlerPath = profileUrlPath.endsWith("/**") ? profileUrlPath : profileUrlPath + "/**";
        String resourceLocation = Path.of(profileUploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation = resourceLocation + "/";
        }

        registry.addResourceHandler(handlerPath)
                .addResourceLocations(resourceLocation);
    }
}
