from fastapi import APIRouter, Depends

from app.core.llm import LLMClient, get_llm
from app.models.schemas import PackageContinuityRequest, PackageContinuityResult
from app.services.package_continuity import check_package

router = APIRouter(prefix="/internal/package", tags=["internal"])


@router.post("/continuity", response_model=PackageContinuityResult, response_model_by_alias=True)
async def continuity(request: PackageContinuityRequest, llm: LLMClient = Depends(get_llm)) -> PackageContinuityResult:
    return await check_package(llm, request)
