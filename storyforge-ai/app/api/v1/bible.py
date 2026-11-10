from fastapi import APIRouter, Depends

from app.api.dependencies import get_tasks
from app.services.orchestration import AiTaskOrchestrator
from app.models.schemas import BibleGenerateRequest, BibleGenerateResponse

router = APIRouter()


@router.post("/bible", response_model=BibleGenerateResponse, response_model_by_alias=True)
async def bible(request: BibleGenerateRequest, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> BibleGenerateResponse:
    return await tasks.generate_bible(request)
