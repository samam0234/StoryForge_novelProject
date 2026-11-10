"""기존 app.main:app 경로 호환. 루트 main.py의 동일 서버 인스턴스를 사용합니다."""
from main import app as app, create_app as create_app
