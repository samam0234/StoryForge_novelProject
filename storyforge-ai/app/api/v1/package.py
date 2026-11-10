from fastapi import APIRouter, Depends

from app.api.dependencies import get_tasks
from app.services.orchestration import AiTaskOrchestrator
from app.models.schemas import PackageContinuityRequest, PackageContinuityResult

router = APIRouter(prefix="/internal/package", tags=["internal"])


@router.post("/continuity", response_model=PackageContinuityResult, response_model_by_alias=True)
async def continuity(request: PackageContinuityRequest, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> PackageContinuityResult:
    return await tasks.check_package(request)
