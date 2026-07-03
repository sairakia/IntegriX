# IntegriX 면접 질문 답변 + 코드 위치

기준 답변 파일: `docs/interview-answers.md`


## 기본 프로젝트 구조

### 1. 본인 프로젝트의 전체 패키지 구조를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix`

위치 설명:
- `src/main/java/kopo/integrix`: 프로젝트의 백엔드 루트 패키지입니다. config, controller, service, repository, entity, dto, security 같은 전체 구조를 여기서 확인할 수 있습니다.

`kopo.integrix` 아래에 역할별로 패키지를 나눴습니다.

- `config`: Spring Security, CORS, WebClient, 비밀번호 암호화, 업로드 리소스 매핑 같은 설정 클래스가 있습니다.
- `controller`: 프론트엔드 요청을 받는 API 계층입니다. 로그인, 회원가입, 분석, 신고, 대시보드, 분석기록 API가 있습니다.
- `service`: 실제 비즈니스 로직을 처리하는 계층입니다.
- `service.impl`: Service 인터페이스의 구현체입니다.
- `repository`: MariaDB JPA Repository와 MongoDB Repository가 있습니다.
- `entity`: MariaDB 테이블과 매핑되는 JPA Entity가 있습니다.
- `dto`: 요청/응답에 사용하는 DTO가 있습니다.
- `security`: JWT 생성, 검증, 인증 필터가 있습니다.
- `util`: URL 정규화 같은 공통 유틸이 있습니다.

### 2. Controller, Service, Repository의 역할 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/controller`, `src/main/java/kopo/integrix/service`, `src/main/java/kopo/integrix/repository`

위치 설명:
- `src/main/java/kopo/integrix/controller`: HTTP 요청을 받는 API 계층입니다. 각 Controller가 어떤 URL을 받고 어떤 Service를 호출하는지 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service`: Service 인터페이스가 있는 계층입니다. 기능별 비즈니스 로직의 계약을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/repository`: MariaDB JPA Repository가 있는 계층입니다. Entity와 DB 접근 메서드를 확인할 수 있습니다.

Controller는 HTTP 요청을 받고 응답을 반환합니다.  
Service는 회원가입, 로그인, 분석 점수 계산, 토큰 재발급 같은 비즈니스 로직을 처리합니다.  
Repository는 DB와 직접 연결되어 데이터를 조회, 저장, 삭제합니다.

예를 들어 회원가입 요청은 `UserController`가 받고, 검증과 저장 로직은 `UserServiceImpl`에서 처리하며, 실제 DB 저장은 `UserInfoRepository`가 담당합니다.

### 3. 본인 프로젝트에서 가장 중요한 기능 하나를 선택해서 요청 흐름을 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`, `src/main/java/kopo/integrix/dto/url/UrlAnalysisRequestDTO.java`, `src/main/java/kopo/integrix/dto/url/UrlAnalysisResponseDTO.java`, `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`, `src/main/java/kopo/integrix/dto/mongo/AnalysisResultDTO.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`: URL 분석 요청을 받는 Controller입니다. /api/url/analyze 요청을 받아 UrlAnalysisService로 전달합니다.
- `src/main/java/kopo/integrix/dto/url/UrlAnalysisRequestDTO.java`: URL 분석 요청 DTO입니다. 분석할 url 값을 받습니다.
- `src/main/java/kopo/integrix/dto/url/UrlAnalysisResponseDTO.java`: URL 분석 응답 DTO입니다. 위험 점수, 신뢰 수준, 점수 반영 요소, 상세 분석 항목을 담습니다.
- `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`: URL 위험도 분석 핵심 로직입니다. URL 정규화, HTTPS/SSL 검사, 키워드/하이픈/IP/단축URL 검사, 신고 DB 반영, 점수 계산을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/dto/mongo/AnalysisResultDTO.java`: MongoDB에 저장되는 분석 결과 문서 DTO입니다. 사용자 ID, 분석 타입, 점수, 결과 라벨, 원본 응답을 저장합니다.

URL 위험도 분석 흐름을 예로 들 수 있습니다.

1. 사용자가 프론트엔드에서 URL을 입력하고 분석 버튼을 누릅니다.
2. 프론트엔드는 `/api/url/analyze`로 `url` 값을 담아 POST 요청합니다.
3. `UrlAnalysisController.analyzeUrl()`이 `UrlAnalysisRequestDTO`를 받습니다.
4. Controller는 `UrlAnalysisServiceImpl.analyzeUrl()`을 호출합니다.
5. Service에서 URL 형식을 정규화하고 HTTPS, SSL 인증서, 도메인 패턴, 단축 URL, IP 주소, 하이픈 사용 여부 등을 검사합니다.
6. 내부 신고 DB와 Google Safe Browsing API를 조회해 외부 위험 신호도 반영합니다.
7. 검사 결과를 바탕으로 위험 점수와 `scoreFactors`를 계산합니다.
8. 분석 결과는 MongoDB에 저장하고, `UrlAnalysisResponseDTO`로 위험도 점수, 결과 라벨, 상세 분석 항목을 프론트엔드에 반환합니다.

### 4. 본인 프로젝트에서 가장 중요한 Controller 클래스는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`: URL 분석 요청을 받는 Controller입니다. /api/url/analyze 요청을 받아 UrlAnalysisService로 전달합니다.

`UrlAnalysisController`가 가장 중요한 Controller 중 하나입니다.  
프로젝트의 핵심 기능이 URL 위험도 분석이고, 이 Controller가 사용자의 URL 분석 요청을 받아 분석 Service로 전달하기 때문입니다.

### 5. 해당 Controller의 주요 메서드 하나를 선택해서 입력값과 반환값을 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`, `src/main/java/kopo/integrix/dto/url/UrlAnalysisRequestDTO.java`, `src/main/java/kopo/integrix/dto/url/UrlAnalysisResponseDTO.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`: URL 분석 요청을 받는 Controller입니다. /api/url/analyze 요청을 받아 UrlAnalysisService로 전달합니다.
- `src/main/java/kopo/integrix/dto/url/UrlAnalysisRequestDTO.java`: URL 분석 요청 DTO입니다. 분석할 url 값을 받습니다.
- `src/main/java/kopo/integrix/dto/url/UrlAnalysisResponseDTO.java`: URL 분석 응답 DTO입니다. 위험 점수, 신뢰 수준, 점수 반영 요소, 상세 분석 항목을 담습니다.

`UrlAnalysisController.analyzeUrl()`을 예로 들 수 있습니다.

- 입력값: `UrlAnalysisRequestDTO`
  - `url`: 분석할 웹사이트 주소
- 반환값: `ResponseEntity<?>`
  - 성공 시 `UrlAnalysisResponseDTO`
  - 실패 시 오류 메시지

`UrlAnalysisResponseDTO`에는 분석한 URL, 위험 점수, 신뢰 수준, 점수 반영 요소, HTTPS/SSL/도메인/외부 보안 DB 검사 결과, 권장 사항이 포함됩니다.

### 6. Service 클래스에서는 어떤 비즈니스 로직을 처리하고 있나요?

관련 위치: `src/main/java/kopo/integrix/service/impl`

위치 설명:
- `src/main/java/kopo/integrix/service/impl`: Service 구현체가 있는 계층입니다. 실제 로그인, 분석, 토큰, 이메일 인증, 이미지 저장 로직을 확인할 수 있습니다.

대표적으로 다음 로직을 처리합니다.

- `UserServiceImpl`: 로그인, 회원가입, 비밀번호 변경, 회원탈퇴, 프로필 이미지 저장
- `AuthTokenServiceImpl`: JWT 발급, refresh token 재발급, 로그아웃, Redis 토큰 관리
- `UrlAnalysisServiceImpl`: URL 정규화, 위험 점수 계산, 신고 DB 확인, Google Safe Browsing 조회
- `TextAnalysisServiceImpl`: 텍스트 위험 표현 분석, LLM 분석 결과 반영
- `ImageAnalysisServiceImpl`: 이미지 파일/URL 분석, LLM 기반 조작 가능성 판단
- `DashboardServiceImpl`: 대시보드 통계 조회
- `AnalysisHistoryServiceImpl`: 로그인 사용자의 분석 기록 조회

### 7. Repository는 어떤 Entity와 연결되어 있나요?

관련 위치: `src/main/java/kopo/integrix/repository/UserInfoRepository.java`, `src/main/java/kopo/integrix/entity/UserInfoEntity.java`, `src/main/java/kopo/integrix/repository/ReportFeedbackRepository.java`, `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`

위치 설명:
- `src/main/java/kopo/integrix/repository/UserInfoRepository.java`: UserInfoEntity와 연결된 JPA Repository입니다. findByUserId, findByEmail, existsBy... 메서드를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/entity/UserInfoEntity.java`: user_info 테이블과 매핑되는 Entity입니다. userId, email, password, role, status, profileImageUrl 컬럼을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/repository/ReportFeedbackRepository.java`: ReportFeedbackEntity와 연결된 JPA Repository입니다. 신고된 URL 조회와 저장 근거를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`: report_feedback 테이블과 매핑되는 Entity입니다. URL 신고 내용, 신고 사유, 사용자 ID 저장 구조를 확인할 수 있습니다.

- `UserInfoRepository`는 `UserInfoEntity`와 연결됩니다.
- `ReportFeedbackRepository`는 `ReportFeedbackEntity`와 연결됩니다.
- `AnalysisResultRepository`는 MongoDB 문서 DTO인 `AnalysisResultDTO`와 연결됩니다.

### 8. Repository 인터페이스에 구현 코드가 없는데 DB 조회가 가능한 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/repository/UserInfoRepository.java`

위치 설명:
- `src/main/java/kopo/integrix/repository/UserInfoRepository.java`: UserInfoEntity와 연결된 JPA Repository입니다. findByUserId, findByEmail, existsBy... 메서드를 확인할 수 있습니다.

Spring Data JPA가 Repository 인터페이스를 보고 런타임에 구현체를 자동 생성하기 때문입니다.  
예를 들어 `findByUserId()`처럼 메서드 이름 규칙을 지키면 Spring Data JPA가 메서드명을 분석해서 SQL을 만들어 실행합니다.

### 9. 본인 프로젝트에서 사용한 Entity 클래스 하나를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/entity/UserInfoEntity.java`

위치 설명:
- `src/main/java/kopo/integrix/entity/UserInfoEntity.java`: user_info 테이블과 매핑되는 Entity입니다. userId, email, password, role, status, profileImageUrl 컬럼을 확인할 수 있습니다.

`UserInfoEntity`는 `user_info` 테이블과 매핑됩니다.

주요 필드는 다음과 같습니다.

- `userId`: 기본키
- `email`: 이메일, unique
- `password`: BCrypt로 암호화된 비밀번호
- `name`: 사용자 이름
- `role`: 사용자 권한
- `status`: 계정 상태
- `profileImageUrl`: 프로필 이미지 URL

### 10. Entity와 DTO를 분리한 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/entity/UserInfoEntity.java`, `src/main/java/kopo/integrix/dto/UserResponseDTO.java`

위치 설명:
- `src/main/java/kopo/integrix/entity/UserInfoEntity.java`: user_info 테이블과 매핑되는 Entity입니다. userId, email, password, role, status, profileImageUrl 컬럼을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/dto/UserResponseDTO.java`: 사용자 응답 DTO입니다. Entity에서 비밀번호를 제외하고 프론트에 필요한 사용자 정보만 반환합니다.

Entity는 DB 구조를 표현하고, DTO는 API 요청/응답 구조를 표현합니다.  
둘을 분리하면 DB 컬럼이 외부 API에 그대로 노출되는 것을 막을 수 있고, 비밀번호 같은 민감정보를 응답에서 제외할 수 있습니다. 또한 DB 구조가 바뀌어도 API 응답 구조를 독립적으로 유지할 수 있습니다.

### 11. 요청 DTO와 응답 DTO를 따로 만든 이유가 있나요?

관련 위치: `src/main/java/kopo/integrix/dto/LoginRequestDTO.java`, `src/main/java/kopo/integrix/dto/SignupRequestDTO.java`, `src/main/java/kopo/integrix/dto/UserResponseDTO.java`, `src/main/java/kopo/integrix/dto/TokenResponseDTO.java`

위치 설명:
- `src/main/java/kopo/integrix/dto/LoginRequestDTO.java`: 로그인 요청 DTO입니다. 프론트에서 보내는 userId와 password 필드를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/dto/SignupRequestDTO.java`: 회원가입 요청 DTO입니다. 회원가입 입력값 구조를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/dto/UserResponseDTO.java`: 사용자 응답 DTO입니다. Entity에서 비밀번호를 제외하고 프론트에 필요한 사용자 정보만 반환합니다.
- `src/main/java/kopo/integrix/dto/TokenResponseDTO.java`: 로그인/재발급 성공 시 반환되는 토큰 응답 DTO입니다. access token, refresh token, 만료시간, 사용자 정보를 담습니다.

요청에 필요한 값과 응답에 필요한 값이 다르기 때문입니다.  
예를 들어 로그인 요청에는 `userId`, `password`가 필요하지만, 응답에는 비밀번호를 절대 포함하면 안 됩니다. 그래서 `LoginRequestDTO`, `TokenResponseDTO`, `UserResponseDTO`처럼 목적별로 나눴습니다.

### 12. 본인 프로젝트에서 DTO 없이 Entity를 그대로 반환한 부분이 있나요? 있다면 문제점은 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`, `src/main/java/kopo/integrix/dto/UserResponseDTO.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/dto/UserResponseDTO.java`: 사용자 응답 DTO입니다. Entity에서 비밀번호를 제외하고 프론트에 필요한 사용자 정보만 반환합니다.

주요 사용자 응답은 `UserResponseDTO`로 반환하고 있어 Entity를 그대로 반환하지 않습니다.  
Entity를 그대로 반환하면 비밀번호, 내부 상태값, DB 컬럼 구조가 외부에 노출될 수 있고, 양방향 연관관계가 있을 경우 순환 참조 문제도 생길 수 있습니다.

### 13. @RestController와 @Controller의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/controller`

위치 설명:
- `src/main/java/kopo/integrix/controller`: HTTP 요청을 받는 API 계층입니다. 각 Controller가 어떤 URL을 받고 어떤 Service를 호출하는지 확인할 수 있습니다.

`@Controller`는 보통 View 이름을 반환해서 서버 렌더링 화면을 보여줄 때 사용합니다.  
`@RestController`는 `@Controller`에 `@ResponseBody`가 합쳐진 형태라서 객체를 JSON 응답으로 바로 반환합니다.  
이 프로젝트는 React 프론트엔드와 통신하는 API 서버라 `@RestController`를 사용합니다.

### 14. @Service, @Repository, @Component의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/service/impl`, `src/main/java/kopo/integrix/repository`, `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl`: Service 구현체가 있는 계층입니다. 실제 로그인, 분석, 토큰, 이메일 인증, 이미지 저장 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/repository`: MariaDB JPA Repository가 있는 계층입니다. Entity와 DB 접근 메서드를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

셋 다 Spring Bean으로 등록되는 stereotype annotation입니다.

- `@Component`: 가장 일반적인 컴포넌트
- `@Service`: 비즈니스 로직 계층
- `@Repository`: DB 접근 계층. DB 예외를 Spring의 DataAccessException 계열로 변환하는 의미도 있습니다.

### 15. 서버 실행 시 가장 먼저 실행되는 클래스는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/IntegriXApplication.java`

위치 설명:
- `src/main/java/kopo/integrix/IntegriXApplication.java`: Spring Boot 애플리케이션의 시작 클래스입니다. main 메서드와 @SpringBootApplication을 확인할 수 있습니다.

`IntegriXApplication` 클래스입니다.  
이 클래스의 `main()` 메서드에서 `SpringApplication.run()`을 호출해 Spring Boot 애플리케이션이 시작됩니다.

### 16. @SpringBootApplication의 역할은 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/IntegriXApplication.java`

위치 설명:
- `src/main/java/kopo/integrix/IntegriXApplication.java`: Spring Boot 애플리케이션의 시작 클래스입니다. main 메서드와 @SpringBootApplication을 확인할 수 있습니다.

`@SpringBootApplication`은 세 가지 주요 기능을 합친 어노테이션입니다.

- `@SpringBootConfiguration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

즉 설정 클래스로 등록하고, Spring Boot 자동 설정을 적용하며, 현재 패키지 하위의 Controller, Service, Repository 등을 자동으로 스캔합니다.

## HTTP 기본

### 17. GET과 POST의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`, `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
- `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`: URL 분석 요청을 받는 Controller입니다. /api/url/analyze 요청을 받아 UrlAnalysisService로 전달합니다.

GET은 주로 데이터를 조회할 때 사용하고, 요청값이 URL 쿼리스트링에 붙습니다.  
POST는 데이터를 생성하거나 처리할 때 사용하고, 요청값은 주로 request body에 담깁니다.

예를 들어 `/user/me`는 현재 사용자 조회라 GET이고, `/user/login`은 로그인 처리를 위해 POST를 사용합니다.

### 18. HTTP 상태코드 200, 201, 400, 401, 403, 404, 500의 의미를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`, `src/main/java/kopo/integrix/dto/CommonResponseDTO.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/dto/CommonResponseDTO.java`: 공통 응답 DTO입니다. success, message, data 구조로 API 응답을 통일합니다.

- `200 OK`: 요청 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 요청값 오류
- `401 Unauthorized`: 인증 필요 또는 인증 실패
- `403 Forbidden`: 인증은 되었지만 권한 없음
- `404 Not Found`: 요청한 리소스 없음
- `500 Internal Server Error`: 서버 내부 오류

### 19. 401과 403의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

`401`은 로그인하지 않았거나 토큰이 유효하지 않은 상태입니다.  
`403`은 로그인은 했지만 해당 리소스에 접근할 권한이 없는 상태입니다.

### 20. 프론트엔드에서 요청값 이름을 바꾸면 백엔드에서는 어디를 수정해야 하나요?

관련 위치: `src/main/java/kopo/integrix/dto`, `src/main/java/kopo/integrix/controller`

위치 설명:
- `src/main/java/kopo/integrix/dto`: 요청 DTO와 응답 DTO가 모여 있는 패키지입니다. API 입출력 구조를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/controller`: HTTP 요청을 받는 API 계층입니다. 각 Controller가 어떤 URL을 받고 어떤 Service를 호출하는지 확인할 수 있습니다.

해당 API가 받는 요청 DTO를 수정해야 합니다.  
예를 들어 로그인 요청에서 `userId`를 `username`으로 바꾸면 `LoginRequestDTO`의 필드명과 이를 사용하는 Service 로직을 함께 확인해야 합니다.

## 외부 API

### 21. 본인 프로젝트에서 사용한 외부 Open API는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`, `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`: OpenAI 텍스트 분석 호출 전용 Client입니다. 요청 생성, 응답 파싱, 25초 대기 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`: OpenAI 이미지 분석 호출 전용 Client입니다. 이미지 base64 요청 생성, 응답 파싱, 35초 대기 설정을 확인할 수 있습니다.

- Google Safe Browsing API
- OpenAI Responses API

Google Safe Browsing은 URL 위험 여부 확인에 사용하고, OpenAI Responses API는 텍스트와 이미지 분석 보조에 사용합니다.

### 22. 해당 Open API를 사용한 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/TextAnalysisServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/ImageAnalysisServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`: URL 위험도 분석 핵심 로직입니다. URL 정규화, HTTPS/SSL 검사, 키워드/하이픈/IP/단축URL 검사, 신고 DB 반영, 점수 계산을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/TextAnalysisServiceImpl.java`: 텍스트 분석 비즈니스 로직입니다. LLM 결과 반영, 규칙 기반 분석, 위험 점수 계산, MongoDB 저장을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/ImageAnalysisServiceImpl.java`: 이미지 분석 비즈니스 로직입니다. 이미지 파일/URL 로딩, 메타데이터 분석, LLM 결과 반영, 위험 점수 계산, MongoDB 저장을 확인할 수 있습니다.

Google Safe Browsing은 악성 URL, 피싱 URL 여부를 외부 보안 DB 기반으로 확인하기 위해 사용했습니다.  
OpenAI API는 단순 키워드 규칙만으로 판단하기 어려운 텍스트 표현과 이미지 조작 가능성 분석을 보완하기 위해 사용했습니다.

### 23. 외부 API 호출 코드는 어느 클래스에 작성되어 있나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`, `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`: OpenAI 텍스트 분석 호출 전용 Client입니다. 요청 생성, 응답 파싱, 25초 대기 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`: OpenAI 이미지 분석 호출 전용 Client입니다. 이미지 base64 요청 생성, 응답 파싱, 35초 대기 설정을 확인할 수 있습니다.

- `SafeBrowsingServiceImpl`: Google Safe Browsing API 호출
- `TextLlmAnalysisClient`: 텍스트 LLM 분석 API 호출
- `ImageLlmAnalysisClient`: 이미지 LLM 분석 API 호출

### 24. 외부 API 호출을 Controller에 직접 작성했나요, Service 또는 Client 클래스로 분리했나요?

관련 위치: `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`, `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`, `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UrlAnalysisController.java`: URL 분석 요청을 받는 Controller입니다. /api/url/analyze 요청을 받아 UrlAnalysisService로 전달합니다.
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`: OpenAI 텍스트 분석 호출 전용 Client입니다. 요청 생성, 응답 파싱, 25초 대기 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`: OpenAI 이미지 분석 호출 전용 Client입니다. 이미지 base64 요청 생성, 응답 파싱, 35초 대기 설정을 확인할 수 있습니다.

Controller에 직접 작성하지 않았습니다.  
Controller는 요청만 받고, 실제 외부 API 호출은 Service 또는 Client 클래스로 분리했습니다. 이렇게 하면 Controller가 단순해지고, 외부 API 실패 처리나 응답 파싱 로직을 재사용하기 쉽습니다.

### 25. 외부 API 호출 시 사용한 방식은 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/config/WebClientConfig.java`, `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/config/WebClientConfig.java`: 외부 API 호출에 사용하는 공통 WebClient 설정 파일입니다. 연결 타임아웃 10초 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.

Spring의 `WebClient`를 사용했습니다.  
`WebClientConfig`에서 공통 WebClient Bean을 만들고, 외부 API 호출 클래스에서 주입받아 사용합니다.

### 26. 외부 API Key는 어디에 저장했나요?

관련 위치: `src/main/resources/application.yml`, `src/main/resources/application-prod.yml`, 서버 `/opt/integrix/integrix.env`

위치 설명:
- `src/main/resources/application.yml`: 로컬/기본 설정 파일입니다. DB, Redis, JWT 만료시간, 외부 API 환경변수 참조값을 확인할 수 있습니다.
- `src/main/resources/application-prod.yml`: 운영 환경 설정 파일입니다. prod profile에서 env 값으로 민감정보를 주입받는 구조를 확인할 수 있습니다.
- `/opt/integrix/integrix.env`: 해당 질문의 답변 근거가 되는 코드 위치입니다.

환경변수와 서버의 env 파일에 저장했습니다.  
`application.yml`에서는 `${GOOGLE_SAFE_BROWSING_API_KEY}`, `${TEXT_LLM_API_KEY}`처럼 환경변수로 주입받도록 했습니다.

### 27. API Key를 GitHub에 올리면 어떤 문제가 발생하나요?

관련 위치: `.gitignore`, `src/main/resources/application.yml`

위치 설명:
- `.gitignore`: Git에 올리면 안 되는 secret 파일과 빌드 산출물을 제외하는 설정입니다.
- `src/main/resources/application.yml`: 로컬/기본 설정 파일입니다. DB, Redis, JWT 만료시간, 외부 API 환경변수 참조값을 확인할 수 있습니다.

API Key가 외부에 노출되어 다른 사람이 무단으로 사용할 수 있습니다.  
그 결과 과금이 발생하거나, API 사용량 제한이 소진되거나, 악의적인 요청으로 서비스가 차단될 수 있습니다. 노출되면 즉시 키를 폐기하고 재발급해야 합니다.

### 28. application.yml에 저장한 민감정보는 어떻게 보호할 수 있나요?

관련 위치: `src/main/resources/application.yml`, `src/main/resources/application-prod.yml`, `.gitignore`, `application-secret.yml`

위치 설명:
- `src/main/resources/application.yml`: 로컬/기본 설정 파일입니다. DB, Redis, JWT 만료시간, 외부 API 환경변수 참조값을 확인할 수 있습니다.
- `src/main/resources/application-prod.yml`: 운영 환경 설정 파일입니다. prod profile에서 env 값으로 민감정보를 주입받는 구조를 확인할 수 있습니다.
- `.gitignore`: Git에 올리면 안 되는 secret 파일과 빌드 산출물을 제외하는 설정입니다.
- `application-secret.yml`: 로컬 전용 secret 설정 파일입니다. Git에는 올리지 않는 민감정보 파일입니다.

민감정보를 직접 적지 않고 환경변수로 분리합니다.  
로컬에서는 Git에 올리지 않는 별도 설정 파일을 사용하고, 배포 서버에서는 `/opt/integrix/integrix.env` 같은 env 파일 또는 서버 환경변수로 관리합니다. `.gitignore`에 secret 파일도 추가해야 합니다.

### 29. 외부 API 호출 실패 시 어떻게 처리하나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`, `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`: OpenAI 텍스트 분석 호출 전용 Client입니다. 요청 생성, 응답 파싱, 25초 대기 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`: OpenAI 이미지 분석 호출 전용 Client입니다. 이미지 base64 요청 생성, 응답 파싱, 35초 대기 설정을 확인할 수 있습니다.

외부 API 종류에 따라 다르게 처리합니다.  
Safe Browsing 조회 실패 시에는 "외부 보안 DB 조회에 실패했습니다"처럼 표시하고 나머지 URL 패턴 분석은 계속 진행합니다.  
반면 텍스트/이미지 LLM은 설정된 상태에서 실패하면 0점이나 안전 결과로 처리하지 않고, 분석 실패 메시지를 반환하도록 했습니다.

### 30. 외부 API가 응답하지 않거나 느리면 사용자 화면에는 어떤 문제가 생기나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`, `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/TextLlmAnalysisClient.java`: OpenAI 텍스트 분석 호출 전용 Client입니다. 요청 생성, 응답 파싱, 25초 대기 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/ImageLlmAnalysisClient.java`: OpenAI 이미지 분석 호출 전용 Client입니다. 이미지 base64 요청 생성, 응답 파싱, 35초 대기 설정을 확인할 수 있습니다.

분석 요청이 오래 걸려 로딩 상태가 길어질 수 있습니다.  
타임아웃이 없으면 사용자는 계속 "분석 중" 상태로 보거나, 서버 스레드가 오래 점유되어 전체 응답성이 떨어질 수 있습니다.

### 31. 타임아웃 처리는 했나요?

관련 위치: `src/main/java/kopo/integrix/config/WebClientConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/WebClientConfig.java`: 외부 API 호출에 사용하는 공통 WebClient 설정 파일입니다. 연결 타임아웃 10초 설정을 확인할 수 있습니다.

네. 공통 `WebClient`에는 연결 타임아웃을 10초로 설정했습니다.  
응답 대기 시간은 API별로 다르게 두었습니다. Google Safe Browsing은 10초, 텍스트 LLM은 25초, 이미지 LLM은 35초까지 기다립니다.

### 32. 외부 API 호출 횟수 제한이 있다면 어떻게 대응할 수 있나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`: URL 위험도 분석 핵심 로직입니다. URL 정규화, HTTPS/SSL 검사, 키워드/하이픈/IP/단축URL 검사, 신고 DB 반영, 점수 계산을 확인할 수 있습니다.

현재 코드에는 외부 API 호출 횟수 제한을 직접 제어하는 로직은 구현되어 있지 않습니다.  
추가로 대응한다면 다음 방식을 적용할 수 있습니다.

- 분석 결과 캐싱
- 같은 URL 반복 요청 제한
- 사용자별 요청 횟수 제한
- Redis를 이용한 rate limit
- 429 Too Many Requests 응답 처리
- 유료 API 사용량 모니터링

### 33. 외부 API 응답 JSON은 어떻게 DTO로 변환했나요?

관련 위치: `src/main/java/kopo/integrix/dto/url/SafeBrowsingResponseDTO.java`, `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/dto/url/SafeBrowsingResponseDTO.java`: Google Safe Browsing API 응답 JSON 구조와 매핑되는 DTO입니다.
- `src/main/java/kopo/integrix/service/impl/SafeBrowsingServiceImpl.java`: Google Safe Browsing API 호출 로직입니다. 외부 보안 DB 조회와 10초 응답 대기 제한을 확인할 수 있습니다.

외부 API별로 처리 방식이 다릅니다.  
Google Safe Browsing 응답은 `WebClient.bodyToMono(SafeBrowsingResponseDTO.class)`로 DTO에 역직렬화합니다.  
텍스트/이미지 LLM 응답은 `ObjectMapper.readTree()`로 JSON을 읽고, 필요한 필드를 `JsonNode`, `List<String>`, 내부 record로 변환해 사용합니다.

### 34. 외부 API 응답 중 어떤 필드를 DB에 저장하나요?

관련 위치: `src/main/java/kopo/integrix/dto/mongo/AnalysisResultDTO.java`, `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/TextAnalysisServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/ImageAnalysisServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/dto/mongo/AnalysisResultDTO.java`: MongoDB에 저장되는 분석 결과 문서 DTO입니다. 사용자 ID, 분석 타입, 점수, 결과 라벨, 원본 응답을 저장합니다.
- `src/main/java/kopo/integrix/service/impl/UrlAnalysisServiceImpl.java`: URL 위험도 분석 핵심 로직입니다. URL 정규화, HTTPS/SSL 검사, 키워드/하이픈/IP/단축URL 검사, 신고 DB 반영, 점수 계산을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/TextAnalysisServiceImpl.java`: 텍스트 분석 비즈니스 로직입니다. LLM 결과 반영, 규칙 기반 분석, 위험 점수 계산, MongoDB 저장을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/ImageAnalysisServiceImpl.java`: 이미지 분석 비즈니스 로직입니다. 이미지 파일/URL 로딩, 메타데이터 분석, LLM 결과 반영, 위험 점수 계산, MongoDB 저장을 확인할 수 있습니다.

전체 원본 응답보다는 최종 분석 결과에 필요한 값만 저장합니다.  
예를 들어 분석 타입, 입력값, 위험 점수, 결과 라벨, 분석 요약, 상세 항목, 생성시간, 사용자 ID 등을 MongoDB 분석 기록으로 저장합니다.

### 35. 외부 API 응답 전체를 저장하지 않고 필요한 값만 저장하는 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/dto/mongo/AnalysisResultDTO.java`

위치 설명:
- `src/main/java/kopo/integrix/dto/mongo/AnalysisResultDTO.java`: MongoDB에 저장되는 분석 결과 문서 DTO입니다. 사용자 ID, 분석 타입, 점수, 결과 라벨, 원본 응답을 저장합니다.

외부 API 응답 전체에는 불필요한 값이 많고, 구조가 변경될 수 있으며, 저장 용량도 커질 수 있습니다.  
필요한 값만 저장하면 DB 구조가 단순해지고, 개인정보나 외부 API 내부 응답을 불필요하게 보관하지 않아도 됩니다.

## JPA 질문

### 1. @Entity의 역할은 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/entity/UserInfoEntity.java`, `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`

위치 설명:
- `src/main/java/kopo/integrix/entity/UserInfoEntity.java`: user_info 테이블과 매핑되는 Entity입니다. userId, email, password, role, status, profileImageUrl 컬럼을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`: report_feedback 테이블과 매핑되는 Entity입니다. URL 신고 내용, 신고 사유, 사용자 ID 저장 구조를 확인할 수 있습니다.

`@Entity`는 해당 클래스가 JPA에서 관리하는 DB 테이블 매핑 객체임을 의미합니다.  
예를 들어 `UserInfoEntity`는 `user_info` 테이블과 매핑됩니다.

### 2. @Id와 @GeneratedValue의 역할은 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/entity/UserInfoEntity.java`, `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`

위치 설명:
- `src/main/java/kopo/integrix/entity/UserInfoEntity.java`: user_info 테이블과 매핑되는 Entity입니다. userId, email, password, role, status, profileImageUrl 컬럼을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`: report_feedback 테이블과 매핑되는 Entity입니다. URL 신고 내용, 신고 사유, 사용자 ID 저장 구조를 확인할 수 있습니다.

`@Id`는 기본키를 의미합니다.  
`@GeneratedValue`는 기본키 값을 DB 또는 JPA가 자동 생성하도록 합니다.

이 프로젝트의 `UserInfoEntity`는 `userId`를 직접 입력받는 기본키라 `@GeneratedValue`를 사용하지 않습니다. `ReportFeedbackEntity`처럼 숫자 ID를 사용하는 Entity에는 자동 증가 키를 사용할 수 있습니다.

### 3. @Column은 왜 사용하나요?

관련 위치: `src/main/java/kopo/integrix/entity/UserInfoEntity.java`, `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`

위치 설명:
- `src/main/java/kopo/integrix/entity/UserInfoEntity.java`: user_info 테이블과 매핑되는 Entity입니다. userId, email, password, role, status, profileImageUrl 컬럼을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/entity/ReportFeedbackEntity.java`: report_feedback 테이블과 매핑되는 Entity입니다. URL 신고 내용, 신고 사유, 사용자 ID 저장 구조를 확인할 수 있습니다.

Entity 필드와 DB 컬럼의 이름, 길이, nullable, unique 같은 제약을 지정하기 위해 사용합니다.  
예를 들어 `email`은 unique로 설정되어 중복 이메일 저장을 막습니다.

### 4. 본인 프로젝트에서 사용한 findBy 메서드가 있나요? 있다면 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/repository/UserInfoRepository.java`, `src/main/java/kopo/integrix/repository/mongo/AnalysisResultRepository.java`

위치 설명:
- `src/main/java/kopo/integrix/repository/UserInfoRepository.java`: UserInfoEntity와 연결된 JPA Repository입니다. findByUserId, findByEmail, existsBy... 메서드를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/repository/mongo/AnalysisResultRepository.java`: MongoDB 분석 결과 Repository입니다. 사용자별 분석 기록, 대시보드 통계 조회 메서드를 확인할 수 있습니다.

있습니다.

- `findByUserId(String userId)`: userId로 사용자 조회
- `findByEmail(String email)`: email로 사용자 조회
- `findByUserIdOrderByCreatedAtDesc(String userId)`: MongoDB 분석 기록을 최신순으로 조회

### 5. findByEmail(), findByUsername(), findById() 같은 메서드는 SQL로 보면 어떤 쿼리와 유사한가요?

관련 위치: `src/main/java/kopo/integrix/repository/UserInfoRepository.java`

위치 설명:
- `src/main/java/kopo/integrix/repository/UserInfoRepository.java`: UserInfoEntity와 연결된 JPA Repository입니다. findByUserId, findByEmail, existsBy... 메서드를 확인할 수 있습니다.

`findByEmail(email)`은 SQL로 보면 다음과 유사합니다.

```sql
SELECT * FROM user_info WHERE email = ?;
```

`findById(id)`는 기본키 기준 조회입니다.

```sql
SELECT * FROM user_info WHERE id = ?;
```

### 6. Optional을 사용한 부분이 있나요? 왜 사용했나요?

관련 위치: `src/main/java/kopo/integrix/repository/UserInfoRepository.java`, `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/repository/UserInfoRepository.java`: UserInfoEntity와 연결된 JPA Repository입니다. findByUserId, findByEmail, existsBy... 메서드를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.

`UserInfoRepository.findByUserId()`와 `findByEmail()`은 `Optional<UserInfoEntity>`를 반환합니다.  
조회 결과가 없을 수 있기 때문에 null 대신 Optional로 처리해서 NullPointerException을 줄이고, 존재 여부를 명확하게 검사합니다.

### 7. orElseThrow()를 사용한 부분이 있나요? 어떤 상황에서 예외가 발생하나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.

현재 주요 사용자 조회 로직은 `orElseThrow()`보다 `isEmpty()`로 직접 확인하고 메시지를 반환하는 방식을 사용합니다.  
`orElseThrow()`를 사용한다면 사용자가 없거나 필수 데이터가 없을 때 예외가 발생합니다.

### 8. @Transactional을 사용한 곳이 있나요? 왜 사용했나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.

`UserServiceImpl.signup()`과 `deleteAccount()`에 사용했습니다.  
회원가입 중 기존 비활성 계정 삭제와 새 계정 저장이 하나의 작업으로 처리되어야 하고, 회원탈퇴도 사용자 삭제와 관련 작업이 하나의 트랜잭션으로 처리되는 것이 안전하기 때문입니다.

### 9. 데이터를 저장할 때 save() 메서드는 insert와 update 중 무엇을 수행하나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`, `src/main/java/kopo/integrix/repository/UserInfoRepository.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/repository/UserInfoRepository.java`: UserInfoEntity와 연결된 JPA Repository입니다. findByUserId, findByEmail, existsBy... 메서드를 확인할 수 있습니다.

Entity가 새 객체이면 insert를 수행하고, 이미 존재하는 식별자를 가진 Entity이면 update를 수행합니다.  
JPA는 Entity의 식별자와 영속성 상태를 기준으로 insert/update를 판단합니다.

### 10. DB 테이블 컬럼명을 바꾸면 코드에서 어디를 수정해야 하나요?

관련 위치: `src/main/java/kopo/integrix/entity/UserInfoEntity.java`

위치 설명:
- `src/main/java/kopo/integrix/entity/UserInfoEntity.java`: user_info 테이블과 매핑되는 Entity입니다. userId, email, password, role, status, profileImageUrl 컬럼을 확인할 수 있습니다.

해당 Entity의 `@Column(name = "...")` 값을 수정해야 합니다.  
Repository 메서드명이나 Service 로직에서 해당 필드를 기준으로 조회하고 있다면 그 부분도 함께 확인해야 합니다.

### 11. @Valid를 사용한 부분이 있나요? 어떤 DTO에 적용했나요?

관련 위치: `src/main/java/kopo/integrix/dto`, `src/main/java/kopo/integrix/controller`

위치 설명:
- `src/main/java/kopo/integrix/dto`: 요청 DTO와 응답 DTO가 모여 있는 패키지입니다. API 입출력 구조를 확인할 수 있습니다.
- `src/main/java/kopo/integrix/controller`: HTTP 요청을 받는 API 계층입니다. 각 Controller가 어떤 URL을 받고 어떤 Service를 호출하는지 확인할 수 있습니다.

현재 코드에서는 `@Valid`, `@NotBlank`, `@Email`, `@Size` 같은 Bean Validation을 적극적으로 사용하지 않고, Service에서 직접 검증하고 메시지를 반환하는 방식입니다.  
개선한다면 `SignupRequestDTO`, `LoginRequestDTO`, `EmailAuthRequestDTO`, `ReportRequestDTO` 등에 적용할 수 있습니다.

### 12. @NotBlank, @NotNull, @Email, @Size의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/dto`

위치 설명:
- `src/main/java/kopo/integrix/dto`: 요청 DTO와 응답 DTO가 모여 있는 패키지입니다. API 입출력 구조를 확인할 수 있습니다.

- `@NotNull`: null만 금지합니다.
- `@NotBlank`: null, 빈 문자열, 공백 문자열을 금지합니다.
- `@Email`: 이메일 형식인지 검사합니다.
- `@Size`: 문자열, 컬렉션 등의 길이나 크기를 검사합니다.

### 13. 예외 발생 시 사용자에게 어떤 메시지를 반환하나요?

관련 위치: `src/main/java/kopo/integrix/dto/CommonResponseDTO.java`, `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/dto/CommonResponseDTO.java`: 공통 응답 DTO입니다. success, message, data 구조로 API 응답을 통일합니다.
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.

대부분 `CommonResponseDTO`로 성공 여부와 메시지를 반환합니다.  
예를 들어 로그인 실패 시 "아이디 또는 비밀번호가 올바르지 않습니다.", 인증 실패 시 "로그인이 필요합니다." 같은 메시지를 반환합니다.

## Spring Security 질문

### 1. 본인 프로젝트에 Spring Security를 왜 적용했나요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

JWT 기반 인증을 처리하고, 로그인해야 접근 가능한 API를 제한하기 위해 적용했습니다.  
또한 비밀번호 암호화, 인증 필터, SecurityContext 관리도 Spring Security 기반으로 처리합니다.

### 2. 인증과 인가의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`, `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

인증은 "사용자가 누구인지 확인하는 것"입니다. 예를 들어 JWT가 유효한지 확인합니다.  
인가는 "인증된 사용자가 특정 URL이나 기능에 접근할 수 있는지 확인하는 것"입니다.

### 3. 본인 프로젝트에서 로그인하지 않아도 접근 가능한 URL은 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

대표적으로 다음 URL은 비로그인 접근을 허용했습니다.

- `/email/**`
- `/user/login`
- `/user/signup`
- `/user/refresh`
- `/user/logout`
- `/user/find-id`
- `/user/reset-password`
- `/user/exists/**`
- `/dashboard/summary`
- `/api/dashboard/summary`
- `/api/url/analyze`
- `/api/text/analyze`
- `/api/image/analyze`
- `/api/report`
- `/uploads/**`

분석 기능은 게스트도 사용할 수 있게 허용했습니다.

### 4. 로그인해야만 접근 가능한 URL은 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

대표적으로 다음 API는 로그인해야 접근 가능합니다.

- `/user/me`
- `/user/profile-image`
- `/user/change-password`
- `/user/delete-account`
- `/user/history`

### 5. URL 접근 권한 설정은 어느 파일에 작성되어 있나요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

`SecurityConfig.java`에 작성되어 있습니다.

### 6. SecurityConfig 클래스의 핵심 설정을 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

핵심 설정은 다음과 같습니다.

- CORS 활성화
- CSRF 비활성화
- 세션을 사용하지 않는 Stateless 설정
- 공개 URL은 `permitAll()`
- 나머지 URL은 `authenticated()`
- `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록
- 인증 실패 시 `401 Unauthorized` 반환

### 7. SecurityFilterChain은 어떤 역할을 하나요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

요청이 Controller에 도달하기 전에 보안 필터들을 순서대로 실행합니다.  
JWT 필터도 이 체인에 포함되어 토큰을 검증하고 SecurityContext에 인증 정보를 저장합니다.

### 8. requestMatchers()는 어떤 역할을 하나요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

특정 URL 패턴이나 HTTP 메서드에 대해 접근 정책을 지정합니다.  
예를 들어 `/user/login`은 `permitAll()`, 나머지는 `authenticated()`로 설정할 수 있습니다.

### 9. permitAll()과 authenticated()의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

`permitAll()`은 로그인하지 않아도 접근을 허용합니다.  
`authenticated()`는 인증된 사용자만 접근을 허용합니다.

### 10. hasRole() 또는 hasAuthority()를 사용했나요? 사용했다면 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

현재 프로젝트는 일반 사용자 기능 중심이라 `hasRole()`이나 `hasAuthority()`를 사용하지 않았습니다.  
사용한다면 `hasRole("ADMIN")`은 내부적으로 `ROLE_ADMIN` 권한을 확인하고, `hasAuthority("ADMIN")`은 문자열 권한을 그대로 비교합니다.

### 11. 비밀번호는 DB에 평문으로 저장되나요?

관련 위치: `src/main/java/kopo/integrix/config/PasswordConfig.java`, `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/config/PasswordConfig.java`: BCryptPasswordEncoder Bean을 등록하는 설정 파일입니다.
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.

아니요. BCrypt로 해시한 값만 DB에 저장합니다.

### 12. PasswordEncoder는 어떤 역할을 하나요?

관련 위치: `src/main/java/kopo/integrix/config/PasswordConfig.java`, `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/config/PasswordConfig.java`: BCryptPasswordEncoder Bean을 등록하는 설정 파일입니다.
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.

비밀번호를 해시하고, 로그인 시 입력된 비밀번호가 저장된 해시와 일치하는지 검증합니다.

### 13. BCryptPasswordEncoder는 복호화가 가능한 방식인가요?

관련 위치: `src/main/java/kopo/integrix/config/PasswordConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/PasswordConfig.java`: BCryptPasswordEncoder Bean을 등록하는 설정 파일입니다.

아니요. BCrypt는 단방향 해시입니다.  
복호화하는 방식이 아니라, 입력된 비밀번호를 같은 알고리즘으로 검증해서 일치 여부만 확인합니다.

### 14. 로그인 시 비밀번호 비교는 어디에서 처리되나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.

`UserServiceImpl.login()`에서 처리합니다.

```java
passwordEncoder.matches(pDTO.password(), user.getPassword())
```

### 15. CSRF 설정을 비활성화했다면 그 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

현재 서비스는 세션 기반 인증이 아니라 JWT 기반 Stateless 인증 구조를 사용하기 때문에 Spring Security의 기본 CSRF 보호를 비활성화했습니다.  
또한 배포 환경에서는 프론트엔드와 백엔드를 같은 도메인으로 제공하고, JWT는 `HttpOnly`, `Secure`, `SameSite=Lax` 쿠키로 전달합니다.

다만 JWT를 쿠키에 저장하면 쿠키가 자동 전송되므로 CSRF 고려가 필요합니다. 현재는 SameSite로 위험을 완화하고 있고, 보안을 더 강화하려면 비밀번호 변경, 회원탈퇴 같은 상태 변경 요청에 CSRF 토큰 검증을 추가할 수 있습니다.

### 16. CORS 설정은 왜 필요한가요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`, `src/main/java/kopo/integrix/config/WebConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/config/WebConfig.java`: CORS, 프로필 이미지 업로드 디렉터리 리소스 매핑을 설정하는 파일입니다.

프론트엔드와 백엔드의 출처가 다를 때 브라우저가 요청을 차단할 수 있기 때문입니다.  
로컬 개발에서는 `localhost:5173` 프론트가 `localhost:11000` 백엔드로 요청하므로 CORS 설정이 필요합니다. 배포 환경에서는 같은 도메인으로 Nginx 프록시를 사용하므로 CORS 필요성이 줄어듭니다.

### 17. 프론트엔드와 백엔드 포트가 다르면 어떤 문제가 발생하나요?

관련 위치: `src/main/java/kopo/integrix/config/WebConfig.java`, 프론트 `C:\Users\8316-08\Downloads\IntegriX\src\app\api\config.ts`

위치 설명:
- `src/main/java/kopo/integrix/config/WebConfig.java`: CORS, 프로필 이미지 업로드 디렉터리 리소스 매핑을 설정하는 파일입니다.
- `C:\Users\8316-08\Downloads\IntegriX\src\app\api\config.ts`: 프론트엔드 API 기본 주소 설정 파일입니다. 배포에서는 같은 도메인 요청을 사용하도록 설정합니다.

브라우저는 프로토콜, 도메인, 포트 중 하나라도 다르면 다른 출처로 봅니다.  
따라서 CORS 설정이 없으면 API 요청이 브라우저에서 차단될 수 있고, 쿠키 인증을 사용하는 경우 credentials 설정도 맞춰야 합니다.

## JWT 질문

### 1. 본인 프로젝트는 세션 기반 로그인인가요, JWT 기반 로그인인가요?

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`, `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

JWT 기반 로그인입니다.  
서버 세션은 사용하지 않고 `SessionCreationPolicy.STATELESS`로 설정했습니다.

### 2. 세션 기반 인증과 JWT 기반 인증의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/config/SecurityConfig.java`, `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

세션 기반 인증은 서버가 세션 저장소에 로그인 상태를 저장하고, 브라우저는 세션 ID만 가집니다.  
JWT 기반 인증은 토큰 자체에 사용자 식별 정보와 만료시간이 들어 있고, 서버는 토큰 서명을 검증해서 인증합니다.

이 프로젝트는 JWT를 HttpOnly 쿠키에 저장해 사용합니다.

### 3. JWT는 어떤 구조로 되어 있나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

JWT는 세 부분으로 구성됩니다.

```text
Header.Payload.Signature
```

각 부분은 Base64URL로 인코딩됩니다.

### 4. JWT의 Header, Payload, Signature는 각각 어떤 역할을 하나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

- Header: 토큰 타입과 서명 알고리즘 정보가 들어갑니다. 이 프로젝트는 `typ=JWT`, `alg=HS256`을 사용합니다.
- Payload: 사용자 ID, 토큰 타입, 발급시간, 만료시간 같은 claim이 들어갑니다.
- Signature: Header와 Payload가 변조되지 않았는지 확인하기 위한 서명입니다.

### 5. JWT Payload에는 어떤 정보를 넣었나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

`JwtTokenProvider` 기준으로 다음 정보를 넣었습니다.

- `sub`: 사용자 ID
- `type`: `access` 또는 `refresh`
- `iat`: 발급 시간
- `exp`: 만료 시간

### 6. JWT Payload에 비밀번호를 넣으면 안 되는 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

JWT Payload는 암호화가 아니라 Base64URL 인코딩이라 누구나 디코딩해서 볼 수 있습니다.  
따라서 비밀번호, 주민번호, API Key 같은 민감정보를 넣으면 안 됩니다.

### 7. JWT는 암호화된 값인가요, 인코딩된 값인가요?

관련 위치: `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

일반적인 JWT는 암호화가 아니라 인코딩된 값입니다.  
서명은 되어 있어서 변조 여부는 검증할 수 있지만, Payload 내용은 디코딩해서 볼 수 있습니다.

### 8. Access Token과 Refresh Token의 차이를 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/security/JwtTokenProvider.java`, `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

Access Token은 API 요청 시 인증에 사용하는 짧은 수명의 토큰입니다.  
Refresh Token은 Access Token이 만료됐을 때 새 토큰을 발급받기 위한 긴 수명의 토큰입니다.

### 9. Access Token은 어떤 용도로 사용하나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

API 요청 시 사용자를 인증하는 데 사용합니다.  
`JwtAuthenticationFilter`가 access token을 검증하고 SecurityContext에 사용자 ID를 저장합니다.

### 10. Refresh Token은 어떤 용도로 사용하나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

Access Token이 만료됐을 때 `/user/refresh` API를 통해 새 access token과 refresh token을 발급받는 데 사용합니다.

### 11. Access Token은 언제 발급되나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`, `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

로그인 성공 시 발급됩니다.  
또한 refresh token으로 토큰을 재발급할 때도 새 access token이 발급됩니다.

### 12. Refresh Token은 언제 발급되나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`, `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

로그인 성공 시 access token과 함께 발급됩니다.  
토큰 재발급 시에도 기존 refresh token을 삭제하고 새 refresh token을 발급합니다.

### 13. Access Token의 만료시간은 어떻게 설정했나요?

관련 위치: `src/main/resources/application.yml`, `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/resources/application.yml`: 로컬/기본 설정 파일입니다. DB, Redis, JWT 만료시간, 외부 API 환경변수 참조값을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

`application.yml`에서 설정합니다.

```yaml
jwt:
  access-token-validity-millis: 1200000
```

현재 값은 1,200,000ms로 20분입니다.

### 14. Refresh Token의 만료시간은 어떻게 설정했나요?

관련 위치: `src/main/resources/application.yml`, `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/resources/application.yml`: 로컬/기본 설정 파일입니다. DB, Redis, JWT 만료시간, 외부 API 환경변수 참조값을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

`application.yml`에서 설정합니다.

```yaml
jwt:
  refresh-token-validity-millis: 604800000
```

현재 값은 604,800,000ms로 7일입니다.

### 15. Access Token을 너무 길게 설정하면 어떤 문제가 있나요?

관련 위치: `src/main/resources/application.yml`

위치 설명:
- `src/main/resources/application.yml`: 로컬/기본 설정 파일입니다. DB, Redis, JWT 만료시간, 외부 API 환경변수 참조값을 확인할 수 있습니다.

탈취됐을 때 공격자가 오래 사용할 수 있습니다.  
Access Token은 API 접근 권한을 직접 가지므로 짧게 유지하는 것이 안전합니다.

### 16. Refresh Token이 탈취되면 어떤 문제가 생기나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

공격자가 새로운 access token을 계속 발급받을 수 있습니다.  
그래서 이 프로젝트는 refresh token을 Redis에 저장하고, 재발급 시 기존 refresh token을 삭제하는 rotation 방식을 사용합니다.

### 17. 본인 프로젝트에서 Access Token은 어디에 저장하나요?

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.

브라우저의 `HttpOnly` 쿠키에 저장합니다. 쿠키 이름은 `accessToken`입니다.  
JavaScript에서 직접 읽지 못하도록 HttpOnly를 사용합니다.

### 18. 본인 프로젝트에서 Refresh Token은 어디에 저장하나요?

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`, `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

브라우저의 `HttpOnly` 쿠키에 저장하고, 서버에서는 Redis에도 해시한 refresh token 키를 저장합니다. 쿠키 이름은 `refreshToken`입니다.

### 19. Access Token을 HttpOnly 쿠키에 저장하는 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`, `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청의 쿠키 또는 Authorization 헤더에서 Access Token을 읽고 인증 정보를 SecurityContext에 저장합니다.

HttpOnly 쿠키는 JavaScript에서 직접 읽을 수 없어서 XSS 상황에서도 토큰이 직접 노출될 위험을 줄일 수 있습니다.  
이 프로젝트는 로그인 성공 시 Access Token과 Refresh Token을 HttpOnly 쿠키로 내려주고, 이후 요청에서는 서버가 쿠키의 토큰을 읽어 인증합니다.

### 20. Refresh Token을 Redis에 저장하는 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

JWT는 발급 후 서버가 상태를 갖지 않으면 강제로 무효화하기 어렵습니다.  
Refresh Token을 Redis에 저장하면 로그아웃, 만료, 재발급 시 서버가 유효 여부를 확인하고 삭제할 수 있습니다. TTL도 설정할 수 있어 자동 만료 처리가 가능합니다.

### 21. 로그인 성공 후 JWT 발급 흐름을 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`, `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`, `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`, `src/main/java/kopo/integrix/security/JwtTokenProvider.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
- `src/main/java/kopo/integrix/service/impl/UserServiceImpl.java`: 사용자 비즈니스 로직 구현체입니다. 로그인 검증, BCrypt 비밀번호 비교, 회원가입, 회원탈퇴, 프로필 이미지 저장 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.

1. `UserController.login()`이 로그인 요청을 받습니다.
2. `UserServiceImpl.login()`이 사용자와 비밀번호를 확인합니다.
3. 성공하면 `AuthTokenServiceImpl.issueTokenPair()`를 호출합니다.
4. `JwtTokenProvider`가 access token과 refresh token을 생성합니다.
5. refresh token은 Redis에 저장됩니다.
6. 두 토큰은 HttpOnly 쿠키로 응답에 담깁니다.

### 22. API 요청 시 JWT는 어디에 담아서 보내나요?

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`, `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

현재는 HttpOnly 쿠키에 담겨 자동 전송됩니다.  
호환성을 위해 `Authorization: Bearer ...` 헤더도 일부 지원합니다.

### 23. Authorization Header의 Bearer는 어떤 의미인가요?

관련 위치: `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`, `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

Bearer는 "이 토큰을 가진 사람이 권한을 가진다"는 의미의 인증 방식입니다.  
HTTP 요청 헤더에 다음처럼 보냅니다.

```http
Authorization: Bearer {accessToken}
```

### 24. JWT 검증은 Controller에서 하나요, Filter에서 하나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

Filter에서 합니다.  
`JwtAuthenticationFilter`가 요청마다 토큰을 확인하고 인증 정보를 저장합니다.

### 25. JWT Filter는 어떤 역할을 하나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

요청에서 `Authorization` 헤더나 `accessToken` 쿠키를 읽고, 토큰이 유효하면 사용자 ID를 꺼냅니다.  
그 후 `UsernamePasswordAuthenticationToken`을 만들어 SecurityContext에 저장합니다.

### 26. JWT Filter에서 토큰을 검증한 후 SecurityContext에는 무엇을 저장하나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.

`UsernamePasswordAuthenticationToken`을 저장합니다.  
principal에는 사용자 ID가 들어가고, 현재는 별도 권한 목록은 빈 리스트로 저장합니다.

### 27. SecurityContext에 인증 정보를 저장하는 이유는 무엇인가요?

관련 위치: `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`, `src/main/java/kopo/integrix/controller/UserController.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtAuthenticationFilter.java`: 요청마다 쿠키 또는 Authorization 헤더에서 JWT를 읽고 SecurityContext에 인증 정보를 저장하는 필터입니다.
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.

Controller에서 `@AuthenticationPrincipal String userId`로 현재 로그인 사용자를 받을 수 있게 하기 위해서입니다.  
또한 Spring Security의 `authenticated()` 인가 판단에도 사용됩니다.

### 28. Access Token이 만료되면 사용자는 어떻게 처리되나요?

관련 위치: `src/main/java/kopo/integrix/security/JwtTokenProvider.java`, `src/main/java/kopo/integrix/config/SecurityConfig.java`

위치 설명:
- `src/main/java/kopo/integrix/security/JwtTokenProvider.java`: JWT 생성, 서명, Payload 읽기, 만료 검증, 토큰 해시 처리를 담당합니다.
- `src/main/java/kopo/integrix/config/SecurityConfig.java`: Spring Security 핵심 설정 파일입니다. permitAll, authenticated, Stateless, JWT 필터, CSRF, CORS 설정을 확인할 수 있습니다.

Access Token이 만료되면 인증에 실패해 보호 API에서 `401 Unauthorized`가 발생합니다.  
프론트엔드는 refresh token으로 `/user/refresh`를 호출해 새 토큰을 받아 다시 요청할 수 있습니다.

### 29. Refresh Token으로 Access Token을 재발급하는 API가 있나요?

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.

있습니다.

```text
POST /user/refresh
```

### 30. Refresh Token 재발급 API의 동작 흐름을 설명해보세요.

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`, `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

1. `/user/refresh` 요청이 들어옵니다.
2. refresh token을 request body 또는 `refreshToken` 쿠키에서 읽습니다.
3. JWT 서명과 만료시간을 검증합니다.
4. Redis에 저장된 refresh token 해시 키가 있는지 확인합니다.
5. 유효하면 기존 Redis refresh token을 삭제합니다.
6. 새 access token과 refresh token을 발급합니다.
7. 새 refresh token을 Redis에 저장합니다.
8. 새 토큰들을 HttpOnly 쿠키로 내려줍니다.

### 31. Refresh Token이 DB에 저장된 값과 다르면 어떻게 처리하나요?

관련 위치: `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

Redis에 저장된 값과 일치하지 않으면 재발급을 거부합니다.  
응답 메시지는 "로그인 정보가 만료되었습니다. 다시 로그인해주세요."처럼 반환됩니다.

### 32. 로그아웃하면 Access Token과 Refresh Token은 어떻게 처리되나요?

관련 위치: `src/main/java/kopo/integrix/controller/UserController.java`, `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`

위치 설명:
- `src/main/java/kopo/integrix/controller/UserController.java`: 사용자 관련 API Controller입니다. 로그인, 회원가입, 토큰 재발급, 로그아웃, 내 정보, 프로필 이미지, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
- `src/main/java/kopo/integrix/service/impl/AuthTokenServiceImpl.java`: JWT 발급, refresh token Redis 저장/재발급, 로그아웃 시 blacklist 처리 로직을 확인할 수 있습니다.

로그아웃 시 브라우저 쿠키의 `accessToken`, `refreshToken`을 모두 삭제합니다.  
Redis에 저장된 refresh token도 삭제합니다.  
Access Token은 이미 발급된 JWT라 직접 삭제할 수 없기 때문에 남은 만료시간 동안 Redis blacklist에 등록해서 더 이상 인증되지 않게 처리합니다.

