"""요청별 작업 조정기를 프로세스의 공통 LLM 클라이언트에 연결합니다."""
from fastapi import Depends

from app.core.llm import LLMClient
from app.core.orchestration import get_llm
from app.services.orchestration import AiTaskOrchestrator


def get_tasks(llm: LLMClient = Depends(get_llm)) -> AiTaskOrchestrator:
    return AiTaskOrchestrator(llm)
