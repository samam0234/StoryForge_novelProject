package com.storyforge.config;

import com.storyforge.client.AiDtos.GuideAction;
import com.storyforge.client.AiDtos.GuideChatRequest;
import com.storyforge.client.AiDtos.GuideChatResponse;
import com.storyforge.client.AiLiveClient;
import com.storyforge.exception.AiServiceException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.community.GuideService;
import com.storyforge.service.orchestration.StoryScoutService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 프론트 웹소켓을 로그인 사용자 세션으로 받고, 가이드와 장면 개척만 AI 웹소켓으로 넘깁니다.
 */
@Component
public class LiveSessionHandler extends TextWebSocketHandler {

    private final ObjectMapper json;
    private final GuideService guides;
    private final StoryScoutService scouts;
    private final AiLiveClient live;

    public LiveSessionHandler(ObjectMapper json, GuideService guides, StoryScoutService scouts, AiLiveClient live) {
        this.json = json;
        this.guides = guides;
        this.scouts = scouts;
        this.live = live;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!(session.getAttributes().get("userId") instanceof Long)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("로그인이 필요합니다."));
            return;
        }
        try {
            session.getAttributes().put("ai", live.open());
        } catch (AiServiceException ignored) {
            session.getAttributes().remove("ai");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (session.getAttributes().get("ai") instanceof AiLiveClient.Session ai) {
            ai.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!(session.getAttributes().get("userId") instanceof Long userId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("로그인이 필요합니다."));
            return;
        }
        if (message.getPayloadLength() > 8000) {
            send(session, error(null, "메시지가 너무 깁니다."));
            return;
        }
        JsonNode node;
        try {
            node = json.readTree(message.getPayload());
        } catch (RuntimeException e) {
            send(session, error(null, "요청 형식이 올바르지 않습니다."));
            return;
        }
        String id = node.path("id").asText("");
        String op = node.path("op").asText("");
        try {
            if ("ping".equals(op)) {
                send(session, "{\"op\":\"pong\",\"id\":" + json.writeValueAsString(id) + "}");
            } else if ("guide".equals(op)) {
                send(session, guide(ai(session), userId, id, node.path("message").asText("")));
            } else if ("scout".equals(op)) {
                long projectId = node.path("projectId").asLong(0);
                send(session, ai(session).exchange(scouts.payload(userId, projectId, id)));
            } else {
                send(session, error(id, "알 수 없는 요청입니다."));
            }
        } catch (NotFoundException e) {
            send(session, error(id, e.getMessage()));
        } catch (AiServiceException e) {
            send(session, error(id, "AI 실시간 연결에 실패했습니다."));
        }
    }

    private String guide(AiLiveClient.Session ai, Long userId, String id, String message) throws Exception {
        String text = message == null ? "" : message.strip();
        if (text.isEmpty() || text.length() > 2000) {
            return error(id, "질문은 1자 이상 2000자 이하여야 합니다.");
        }
        String request = json.writeValueAsString(new GuideChatRequest(text, guides.contextFor(userId)));
        ObjectNode outbound = (ObjectNode) json.readTree(request);
        outbound.put("op", "guide");
        outbound.put("id", id);
        JsonNode raw = json.readTree(ai.exchange(outbound.toString()));
        if ("error".equals(raw.path("op").asText())) {
            return error(id, raw.path("message").asText("요청을 처리하지 못했습니다."));
        }
        List<GuideAction> actions = new ArrayList<>();
        for (JsonNode action : raw.path("actions")) {
            actions.add(new GuideAction(action.path("label").asText(), action.path("kind").asText(), action.path("target").asText()));
        }
        GuideChatResponse decorated = guides.decorate(new GuideChatResponse(raw.path("reply").asText(""), actions));
        ObjectNode response = json.createObjectNode();
        response.put("op", "guide");
        response.put("id", id);
        response.put("reply", decorated.reply());
        response.set("actions", json.valueToTree(decorated.actions()));
        return response.toString();
    }

    private AiLiveClient.Session ai(WebSocketSession session) {
        if (session.getAttributes().get("ai") instanceof AiLiveClient.Session value) {
            return value;
        }
        throw new AiServiceException("AI 실시간 연결에 실패했습니다.", null);
    }

    private String error(String id, String message) throws Exception {
        ObjectNode node = json.createObjectNode();
        node.put("op", "error");
        node.put("id", id == null ? "" : id);
        node.put("message", message);
        return node.toString();
    }

    private static void send(WebSocketSession session, String payload) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(payload));
        }
    }
}
