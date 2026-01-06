package com.confession.game.global.handler;

import com.confession.game.domain.confession.dto.ConfessionDto;
import com.confession.game.domain.confession.entity.Confession;
import com.confession.game.domain.game.dto.JoinRoomRequest;
import com.confession.game.domain.game.dto.RoomStateResponse;
import com.confession.game.domain.player.dto.PlayerDto;
import com.confession.game.domain.player.entity.Player;
import com.confession.game.domain.room.entity.Room;
import com.confession.game.domain.room.service.RoomService;
import com.confession.game.global.common.BaseResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final RoomService roomService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // sessionId -> {roomId, playerId}
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    // roomId -> Set<sessionId>
    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("받은 메시지: {}", payload);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.get("type").asText();
            JsonNode data = jsonNode.get("data");

            switch (type) {
                case "join-room" -> handleJoinRoom(session, data);
                case "start-game" -> handleStartGame(session, data);
                case "send-chat-message" -> handleChatMessage(session, data);
                case "send-confession" -> handleConfession(session, data);
                case "send-explanation" -> handleExplanation(session, data);
                case "vote" -> handleVote(session, data);
                case "select-next-target" -> handleSelectNextTarget(session, data);
                case "leave-room" -> handleLeaveRoom(session);
                default -> sendError(session, "알 수 없는 메시지 타입: " + type);
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
            sendError(session, "메시지 처리 실패: " + e.getMessage());
        }
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode data) throws IOException {
        String roomId = data.get("roomId").asText();
        String playerId = data.get("playerId").asText();
        String playerName = data.get("playerName").asText();

        Player player = roomService.joinRoom(roomId, playerId, playerName, session.getId());
        sessions.put(session.getId(), new SessionInfo(roomId, playerId));

        // 방 세션 목록에 추가
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        Room room = roomService.getRoom(roomId);

        // 참가 성공 응답
        sendToSession(session, BaseResponse.of("join-room-success", Map.of(
                "player", PlayerDto.from(player),
                "room", RoomStateResponse.from(room)
        )));

        // 방의 모든 사람에게 플레이어 목록 업데이트
        broadcastToRoom(roomId, BaseResponse.of("player-list-updated", Map.of(
                "players", room.getPlayers().values().stream()
                        .map(PlayerDto::from)
                        .toList()
        )));
    }

    private void handleStartGame(WebSocketSession session, JsonNode data) throws IOException {
        SessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) {
            sendError(session, "세션 정보를 찾을 수 없습니다.");
            return;
        }

        try {
            roomService.startGame(sessionInfo.roomId);
            Room room = roomService.getRoom(sessionInfo.roomId);

            broadcastToRoom(sessionInfo.roomId, BaseResponse.of("game-started", Map.of(
                    "target", room.getCurrentTarget(),
                    "targetName", room.getPlayers().get(room.getCurrentTarget()).getName()
            )));
        } catch (IllegalStateException e) {
            sendError(session, e.getMessage());
        }
    }

    private void handleChatMessage(WebSocketSession session, JsonNode data) throws IOException {
        SessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) {
            sendError(session, "세션 정보를 찾을 수 없습니다.");
            return;
        }

        String message = data.get("message").asText();
        Room room = roomService.getRoom(sessionInfo.roomId);
        Player sender = room.getPlayers().get(sessionInfo.playerId);

        // 대상자를 제외한 모든 플레이어에게 메시지 전송
        String currentTarget = room.getCurrentTarget();
        Map<String, WebSocketSession> roomSessionMap = roomSessions.get(sessionInfo.roomId);

        if (roomSessionMap != null) {
            roomSessionMap.forEach((sessionId, webSocketSession) -> {
                SessionInfo info = sessions.get(sessionId);
                if (info != null && !info.playerId.equals(currentTarget)) {
                    try {
                        sendToSession(webSocketSession, BaseResponse.of("chat-message", Map.of(
                                "senderId", sender.getId(),
                                "senderName", sender.getName(),
                                "message", message,
                                "timestamp", System.currentTimeMillis()
                        )));
                    } catch (IOException e) {
                        log.error("메시지 전송 실패", e);
                    }
                }
            });
        }
    }

    private void handleConfession(WebSocketSession session, JsonNode data) throws IOException {
        SessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) {
            sendError(session, "세션 정보를 찾을 수 없습니다.");
            return;
        }

        String message = data.get("message").asText();

        try {
            Confession confession = roomService.sendConfession(sessionInfo.roomId, sessionInfo.playerId, message);
            Room room = roomService.getRoom(sessionInfo.roomId);

            // 발신자에게 전송 완료 알림
            sendToSession(session, BaseResponse.of("confession-sent", Map.of(
                    "confessionId", confession.getId()
            )));

            // 대상자에게만 익명 메시지 전송
            String targetSessionId = findSessionId(sessionInfo.roomId, room.getCurrentTarget());
            if (targetSessionId != null) {
                WebSocketSession targetSession = roomSessions.get(sessionInfo.roomId).get(targetSessionId);
                if (targetSession != null) {
                    sendToSession(targetSession, BaseResponse.of("confession-received", ConfessionDto.from(confession)));
                }
            }
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
        }
    }

    private void handleExplanation(WebSocketSession session, JsonNode data) throws IOException {
        SessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) {
            sendError(session, "세션 정보를 찾을 수 없습니다.");
            return;
        }

        String confessionId = data.get("confessionId").asText();
        String explanation = data.get("explanation").asText();

        try {
            roomService.sendExplanation(sessionInfo.roomId, sessionInfo.playerId, confessionId, explanation);

            broadcastToRoom(sessionInfo.roomId, BaseResponse.of("explanation-received", Map.of(
                    "confessionId", confessionId,
                    "explanation", explanation,
                    "timestamp", System.currentTimeMillis()
            )));
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
        }
    }

    private void handleVote(WebSocketSession session, JsonNode data) throws IOException {
        SessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) {
            sendError(session, "세션 정보를 찾을 수 없습니다.");
            return;
        }

        boolean agree = data.get("agree").asBoolean();

        try {
            Room.VoteResult result = roomService.vote(sessionInfo.roomId, sessionInfo.playerId, agree);

            broadcastToRoom(sessionInfo.roomId, BaseResponse.of("vote-updated", Map.of(
                    "votes", result.getVotes(),
                    "required", result.getRequired()
            )));

            if (result.isComplete()) {
                broadcastToRoom(sessionInfo.roomId, BaseResponse.of("vote-complete", Map.of(
                        "allAgree", result.isAllAgree()
                )));
            }
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
        }
    }

    private void handleSelectNextTarget(WebSocketSession session, JsonNode data) throws IOException {
        SessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) {
            sendError(session, "세션 정보를 찾을 수 없습니다.");
            return;
        }

        String targetId = data.get("targetId").asText();

        try {
            roomService.selectNextTarget(sessionInfo.roomId, sessionInfo.playerId, targetId);
            Room room = roomService.getRoom(sessionInfo.roomId);

            broadcastToRoom(sessionInfo.roomId, BaseResponse.of("new-target-selected", Map.of(
                    "target", room.getCurrentTarget(),
                    "targetName", room.getPlayers().get(room.getCurrentTarget()).getName()
            )));
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
        }
    }

    private void handleLeaveRoom(WebSocketSession session) throws IOException {
        SessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) return;

        try {
            Room room = roomService.getRoom(sessionInfo.roomId);
            boolean wasTarget = sessionInfo.playerId.equals(room.getCurrentTarget());

            roomService.leaveRoom(sessionInfo.roomId, sessionInfo.playerId);

            // 방 세션 목록에서 제거
            Map<String, WebSocketSession> roomSessionMap = roomSessions.get(sessionInfo.roomId);
            if (roomSessionMap != null) {
                roomSessionMap.remove(session.getId());
                if (roomSessionMap.isEmpty()) {
                    roomSessions.remove(sessionInfo.roomId);
                }
            }

            sessions.remove(session.getId());

            // 남은 플레이어들에게 업데이트 전송
            if (!room.isEmpty()) {
                Room updatedRoom = roomService.getRoom(sessionInfo.roomId);
                broadcastToRoom(sessionInfo.roomId, BaseResponse.of("player-list-updated", Map.of(
                        "players", updatedRoom.getPlayers().values().stream()
                                .map(PlayerDto::from)
                                .toList()
                )));

                if (wasTarget) {
                    broadcastToRoom(sessionInfo.roomId, BaseResponse.of("game-reset", Map.of(
                            "message", "대상자가 나가서 게임이 초기화되었습니다."
                    )));
                }
            }
        } catch (IllegalArgumentException e) {
            // 방이 이미 삭제된 경우
            log.warn("방을 찾을 수 없습니다: {}", sessionInfo.roomId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("클라이언트 연결 해제: {}", session.getId());
        handleLeaveRoom(session);
    }

    private void sendToSession(WebSocketSession session, BaseResponse<?> response) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        }
    }

    private void broadcastToRoom(String roomId, BaseResponse<?> response) throws IOException {
        Map<String, WebSocketSession> roomSessionMap = roomSessions.get(roomId);
        if (roomSessionMap != null) {
            String json = objectMapper.writeValueAsString(response);
            for (WebSocketSession session : roomSessionMap.values()) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        sendToSession(session, BaseResponse.error(message));
    }

    private String findSessionId(String roomId, String playerId) {
        Map<String, WebSocketSession> roomSessionMap = roomSessions.get(roomId);
        if (roomSessionMap == null) return null;

        return roomSessionMap.keySet().stream()
                .filter(sessionId -> {
                    SessionInfo info = sessions.get(sessionId);
                    return info != null && info.playerId.equals(playerId);
                })
                .findFirst()
                .orElse(null);
    }

    private record SessionInfo(String roomId, String playerId) {
    }
}