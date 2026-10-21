// Spring Boot API만 가리킵니다. FastAPI 주소는 넣지 않습니다.
// 개발 서버는 proxy.conf.json으로 /api 를 백엔드에 전달합니다.
export const environment = {
  apiBaseUrl: '/api/v1',
};
