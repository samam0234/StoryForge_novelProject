# 제3자 구성 요소 고지

StoryForge는 아래 오픈소스 구성 요소를 사용합니다. 각 구성 요소는 **각자의 라이선스**를 따르며, 이 저장소의 [LICENSE](../../../LICENSE)는 그 라이선스를 바꾸지 않습니다.

> 이 목록은 직접 사용하는 주요 구성 요소 기준이며 **전체 의존성 목록이 아닙니다.** 라이선스는 각 프로젝트의 공식 표기를 따라 적었으니, 배포나 상업적 사용 전에 실제 설치 버전으로 다시 확인하세요. 전체 목록은 `npm ls`, `./mvnw dependency:tree`, `pip list`로 뽑을 수 있습니다.

## 저장소에 포함된 파일

| 구성 요소 | 위치 | 라이선스 | 비고 |
|-----------|------|----------|------|
| 나눔고딕 (NanumGothic Regular, Bold) | `storyforge-backend/src/main/resources/fonts/` | SIL Open Font License 1.1 | PDF 내보내기에 내장. 글꼴과 함께 `OFL.txt`를 배포해야 합니다. 저작권: NHN Corporation |
| Maven Wrapper | `storyforge-backend/mvnw`, `.mvn/` | Apache-2.0 | start.spring.io에서 생성 |

## 실행 중 외부에서 불러오는 리소스 (저장소에 없음)

| 구성 요소 | 출처 | 라이선스 |
|-----------|------|----------|
| Pretendard | jsDelivr CDN (`orioncactus/pretendard`) | SIL Open Font License 1.1 |
| Material Icons | Google Fonts | Apache-2.0 |

## 프론트엔드 (`storyforge-frontend`)

| 구성 요소 | 라이선스 |
|-----------|----------|
| Angular, Angular Material, Angular CDK | MIT |
| RxJS | Apache-2.0 |
| TipTap (`@tiptap/*`) | MIT |
| diff | BSD-3-Clause |
| Vitest | MIT |

## 운영 콘솔 (`storyforge-console`)

운영 콘솔은 Angular(MIT), RxJS(Apache-2.0), Vitest(MIT)를 사용합니다. 외부 글꼴이나 이미지 리소스를 추가로 불러오지 않습니다. 실제 배포 의존성은 별도 `package-lock.json`으로 고정합니다.

## 백엔드 (`storyforge-backend`)

| 구성 요소 | 라이선스 |
|-----------|----------|
| Spring Boot, Spring Framework, Spring Security, Spring Data | Apache-2.0 |
| springdoc-openapi | Apache-2.0 |
| Resilience4j | Apache-2.0 |
| Flyway | Apache-2.0 |
| PostgreSQL JDBC 드라이버 | BSD-2-Clause |
| Apache POI | Apache-2.0 |
| OpenPDF | LGPL-2.1 / MPL-2.0 (이중 라이선스) |
| commonmark-java | BSD-2-Clause |
| Testcontainers | MIT |

> OpenPDF는 LGPL/MPL 계열입니다. 이 프로젝트는 수정 없이 라이브러리로 사용하지만, 애플리케이션을 **배포**할 때는 해당 라이선스의 고지·소스 제공 조건을 확인하세요.

## AI 서비스 (`storyforge-ai`)

| 구성 요소 | 라이선스 |
|-----------|----------|
| FastAPI | MIT |
| Pydantic, pydantic-settings | MIT |
| Uvicorn | BSD-3-Clause |
| Jinja2 | BSD-3-Clause |
| Anthropic Python SDK (`anthropic`) | MIT |
| pytest, httpx | MIT / BSD-3-Clause |

## 인프라

| 구성 요소 | 라이선스 |
|-----------|----------|
| PostgreSQL | PostgreSQL License (BSD 계열) |
| Docker 이미지 `postgres:16` | 이미지에 포함된 각 구성 요소의 라이선스 |

## 외부 서비스

`LLM_PROVIDER=anthropic`으로 쓰는 Claude API는 Anthropic의 이용 약관을 따릅니다. 생성된 글의 권리와 사용 조건은 해당 약관을 확인하세요.
