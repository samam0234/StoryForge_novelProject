from fastapi import APIRouter, Depends

from app.api.dependencies import get_tasks
from app.services.orchestration import AiTaskOrchestrator
from app.models.schemas import GuideChatRequest, GuideChatResponse

router = APIRouter(prefix="/internal/guide", tags=["internal"])


@router.post("/chat", response_model=GuideChatResponse, response_model_by_alias=True)
async def chat(request: GuideChatRequest, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> GuideChatResponse:
    return await tasks.guide(request)
