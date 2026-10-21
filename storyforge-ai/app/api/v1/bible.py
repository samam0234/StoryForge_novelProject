from fastapi import APIRouter, Depends

from app.core.llm import LLMClient, get_llm
from app.models.schemas import BibleGenerateRequest, BibleGenerateResponse
from app.services.bible_generator import generate_bible

router = APIRouter()


@router.post("/bible", response_model=BibleGenerateResponse, response_model_by_alias=True)
async def bible(request: BibleGenerateRequest, llm: LLMClient = Depends(get_llm)) -> BibleGenerateResponse:
    return await generate_bible(llm, request)
