## 제목 / 커밋번호

- **제목**: `feat(api): 게시판, Clone, 공유, 패키지포지, 작업 그래프, 프로필 추가`
- **커밋번호**: `40d1406`
- **브랜치**: `feature/auth`
- **일시**: 2026-11-03 06:40

## 주 내용

게시판, Clone, 공유, 패키지포지, 작업 그래프, 프로필의 백엔드를 추가합니다. 비공개 프로젝트와 공개 작품을 분리하기 위함입니다.

## 상세

- V2~V5 마이그레이션, `domain/community`, `domain/activity`, `service/community`, 커뮤니티 컨트롤러
- 내보내기 8종(`ManuscriptWriters`)
- 상태 머신은 바꾸지 않음. Clone은 시작 상태만 지정

## 결과

- `CommunityIntegrationTest`로 확인
- 스펙: [community.md](../../plan/community.md)
- 이 기록은 2026-11-04에 소급 작성했습니다.
