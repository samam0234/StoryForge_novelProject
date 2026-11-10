"""AI 서버 정식 ASGI 진입점: 서비스 루트에서 uvicorn main:app으로 실행합니다."""
from app.orchestration import create_app

app = create_app()
