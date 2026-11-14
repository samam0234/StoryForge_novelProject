"""브라우저 세션은 Spring Boot가 맡고, 이 소켓은 그 연결이 열려 있는 동안 요청을 여러 번 처리합니다."""
import json
import logging

from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect

from app.api.dependencies import get_tasks
from app.models.schemas import GuideChatRequest, ScoutRequest
from app.services.orchestration import AiTaskOrchestrator
from app.services.story_scout import scout

logger = logging.getLogger(__name__)
router = APIRouter(tags=["internal"])


@router.websocket("/internal/ws")
async def live(websocket: WebSocket, tasks: AiTaskOrchestrator = Depends(get_tasks)) -> None:
    await websocket.accept()
    try:
        while True:
            raw = await websocket.receive_text()
            await _handle(websocket, tasks, raw)
    except WebSocketDisconnect:
        return
    except Exception:
        logger.exception("내부 웹소켓 세션이 끊겼습니다.")


async def _handle(websocket: WebSocket, tasks: AiTaskOrchestrator, raw: str) -> None:
    message_id = None
    try:
        payload = json.loads(raw)
        op = payload.get("op")
        message_id = payload.get("id")
        if op == "ping":
            await websocket.send_json({"op": "pong", "id": message_id})
        elif op == "guide":
            request = GuideChatRequest.model_validate(payload)
            result = await tasks.guide(request)
            await websocket.send_json({"op": "guide", "id": message_id, **result.model_dump(by_alias=True)})
        elif op == "scout":
            request = ScoutRequest.model_validate(payload)
            result = await scout(tasks.llm, request)
            await websocket.send_json({"op": "scout", "id": message_id, **result.model_dump(by_alias=True)})
        else:
            await websocket.send_json({"op": "error", "id": message_id, "message": "알 수 없는 요청입니다."})
    except Exception:
        logger.exception("내부 웹소켓 요청을 처리하지 못했습니다.")
        await websocket.send_json({"op": "error", "id": message_id, "message": "요청을 처리하지 못했습니다."})
