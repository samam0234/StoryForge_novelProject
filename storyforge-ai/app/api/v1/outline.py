from fastapi import APIRouter, Depends

from app.core.llm import LLMClient, get_llm
from app.models.schemas import OutlineGenerateRequest, OutlineGenerateResponse
from app.services.outline_generator import generate_outline

router = APIRouter()


@router.post("/outline", response_model=OutlineGenerateResponse, response_model_by_alias=True)
async def outline(request: OutlineGenerateRequest, llm: LLMClient = Depends(get_llm)) -> OutlineGenerateResponse:
    return await generate_outline(llm, request)
