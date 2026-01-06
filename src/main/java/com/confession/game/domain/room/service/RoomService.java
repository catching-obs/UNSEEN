package com.confession.game.domain.room.service;

import com.confession.game.domain.confession.entity.Confession;
import com.confession.game.domain.player.entity.Player;
import com.confession.game.domain.room.entity.Room;
import com.confession.game.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Room getOrCreateRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseGet(() -> {
                    Room newRoom = Room.builder()
                            .roomId(roomId)
                            .build();
                    return roomRepository.save(newRoom);
                });
    }

    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));
    }

    public Player joinRoom(String roomId, String playerId, String playerName, String sessionId) {
        Room room = getOrCreateRoom(roomId);
        Player player = room.addPlayer(playerId, playerName, sessionId);
        roomRepository.save(room);

        log.info("플레이어 {} ({})가 방 {}에 참가했습니다.", playerName, playerId, roomId);
        return player;
    }

    public void leaveRoom(String roomId, String playerId) {
        Room room = getRoom(roomId);
        room.removePlayer(playerId);

        if (room.isEmpty()) {
            roomRepository.deleteById(roomId);
            log.info("방 {}이(가) 삭제되었습니다.", roomId);
        } else {
            roomRepository.save(room);
        }

        log.info("플레이어 {}가 방 {}을(를) 나갔습니다.", playerId, roomId);
    }

    public void startGame(String roomId) {
        Room room = getRoom(roomId);
        room.startGame();
        roomRepository.save(room);

        log.info("방 {}에서 게임 시작. 대상: {}", roomId, room.getCurrentTarget());
    }

    public Confession sendConfession(String roomId, String senderId, String message) {
        Room room = getRoom(roomId);

        if (senderId.equals(room.getCurrentTarget())) {
            throw new IllegalArgumentException("대상자는 고해성사 메시지를 보낼 수 없습니다.");
        }

        Confession confession = room.addConfession(senderId, message);
        roomRepository.save(room);

        log.info("고해성사 메시지 전송: {}", message);
        return confession;
    }

    public void sendExplanation(String roomId, String playerId, String confessionId, String explanation) {
        Room room = getRoom(roomId);

        if (!playerId.equals(room.getCurrentTarget())) {
            throw new IllegalArgumentException("대상자만 해명할 수 있습니다.");
        }

        room.addExplanation(confessionId, explanation);
        roomRepository.save(room);

        log.info("해명 전송: {}", explanation);
    }

    public Room.VoteResult vote(String roomId, String playerId, boolean agree) {
        Room room = getRoom(roomId);

        if (playerId.equals(room.getCurrentTarget())) {
            throw new IllegalArgumentException("대상자는 투표할 수 없습니다.");
        }

        Room.VoteResult result = room.vote(playerId, agree);
        roomRepository.save(room);

        if (result.isComplete()) {
            log.info("투표 완료. 모두 동의: {}", result.isAllAgree());
        }

        return result;
    }

    public void selectNextTarget(String roomId, String currentPlayerId, String targetId) {
        Room room = getRoom(roomId);

        if (!currentPlayerId.equals(room.getCurrentTarget())) {
            throw new IllegalArgumentException("현재 대상자만 다음 대상을 선택할 수 있습니다.");
        }

        room.selectNextTarget(targetId);
        roomRepository.save(room);

        log.info("새로운 대상 선택됨: {}", targetId);
    }
}