# IntegriX

IntegriX는 텍스트, 이미지, URL을 분석하여 허위정보, 피싱 위험, AI 생성·합성·조작 의심 이미지, 위험 URL 여부를 확인할 수 있는 AI 기반 통합 보안 분석 플랫폼입니다.

## 주요 기능

- 텍스트 신뢰도 분석
- 이미지 AI 생성·합성·조작 의심 분석
- URL 위험도 분석
- Google Safe Browsing API 기반 URL 검사
- 이메일 인증 기반 회원가입
- JWT 기반 로그인 및 인증
- 분석 이력 조회
- 대시보드 통계
- 신고 피드백

## 기술 스택

- Backend: Spring Boot 4.x, Spring Security, Java 17
- Frontend: React, TypeScript, Vite, Tailwind CSS
- Database: MariaDB, MongoDB Atlas, Redis
- Cloud: AWS EC2, AWS RDS
- Web Server: Nginx
- API: Google Safe Browsing API, 외부 LLM API
- Auth/Security: JWT Token, HttpOnly Cookie, BCrypt, Stateless Authentication

## 시스템 구조

- MariaDB: 회원 정보와 신고 데이터 저장
- MongoDB Atlas: 분석 결과와 상세 이력 저장
- Redis: Refresh Token, 이메일 인증 코드, 인증 상태 저장
- Nginx: 정적 리소스 제공 및 Spring Boot API 리버스 프록시
- AWS EC2: 백엔드와 프론트엔드 배포

## 배포 주소

https://integrix.kr/
