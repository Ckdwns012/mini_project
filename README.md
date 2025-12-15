## 실행 환경
- Java 17
- Gradle 8.14.3 (Gradle Wrapper 포함)
- Spring Boot 3.5.7

본 프로젝트는 Java 17 및 Gradle Wrapper 기반으로 구성되어,
별도의 Gradle 설치 없이 실행 가능합니다.

## AI 연결
- LMSTUIO 설치 : https://lmstudio.ai/
- LEVEL 선택 : developer
- 최초모델 다운로드 : 추천ai 스킵 - 가운데 상단(select a model to load) - qwen 검색 - qwen3 VI 4B 다운로드
- 서버실행 : 왼쪽 2번째 아이콘 선택(DEVELOPER) - status 토글 버튼 on
- API연결 : SERVICE - aiService.java - aiAPI 주소 수정
