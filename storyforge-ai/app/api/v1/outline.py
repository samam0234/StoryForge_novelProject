from fastapi import APIRouter, Depends

from app.api.dependencies import get_tasks
from app.services.orchestration import AiTaskOrchestrator
from app.models.schemas import OutlineGenerateRequest, OutlineGenerateResponse

router = APIRouter()


@router.post("/outline", response_model=OutlineGenerateResponse, response_model_by_alias=True)
async def outline(request: OutlineGenerateRequest, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> OutlineGenerateResponse:
    return await tasks.generate_outline(request)
