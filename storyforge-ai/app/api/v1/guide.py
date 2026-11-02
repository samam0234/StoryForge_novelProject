from fastapi import APIRouter, Depends

from app.core.llm import LLMClient, get_llm
from app.models.schemas import GuideChatRequest, GuideChatResponse
from app.services.guide_assistant import answer

router = APIRouter(prefix="/internal/guide", tags=["internal"])


@router.post("/chat", response_model=GuideChatResponse, response_model_by_alias=True)
async def chat(request: GuideChatRequest, llm: LLMClient = Depends(get_llm)) -> GuideChatResponse:
    return await answer(llm, request)
