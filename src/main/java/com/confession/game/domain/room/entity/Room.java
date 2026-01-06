package com.confession.game.domain.room.entity;

import com.confession.game.domain.confession.entity.Confession;
import com.confession.game.domain.player.entity.Player;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private String roomId;

    @Builder.Default
    private Map<String, Player> players = new ConcurrentHashMap<>();

    @Builder.Default
    private GameState gameState = GameState.WAITING;

    private String currentTarget;

    @Builder.Default
    private List<String> targetHistory = new ArrayList<>();

    @Builder.Default
    private List<Confession> confessions = new ArrayList<>();

    @Builder.Default
    private Map<String, Boolean> votes = new ConcurrentHashMap<>();

    public Player addPlayer(String playerId, String playerName, String sessionId) {
        Player player = players.get(playerId);
        if (player != null) {
            player.updateSessionId(sessionId);
            return player;
        }

        player = Player.builder()
                .id(playerId)
                .name(playerName)
                .sessionId(sessionId)
                .build();

        players.put(playerId, player);
        return player;
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);

        if (playerId.equals(currentTarget)) {
            resetGame();
        }
    }

    public void startGame() {
        if (players.size() < 2) {
            throw new IllegalStateException("최소 2명 이상의 플레이어가 필요합니다.");
        }

        this.gameState = GameState.PLAYING;
        selectRandomTarget();
    }

    private void selectRandomTarget() {
        List<String> availablePlayers = players.keySet().stream()
                .filter(id -> !targetHistory.contains(id))
                .toList();

        if (availablePlayers.isEmpty()) {
            targetHistory.clear();
            selectRandomTarget();
            return;
        }

        Random random = new Random();
        this.currentTarget = availablePlayers.get(random.nextInt(availablePlayers.size()));
        this.targetHistory.add(currentTarget);
        this.confessions.clear();
        this.votes.clear();
    }

    public void selectNextTarget(String targetId) {
        if (targetHistory.contains(targetId)) {
            throw new IllegalArgumentException("이미 고해성사 대상이 된 플레이어입니다.");
        }

        if (!players.containsKey(targetId)) {
            throw new IllegalArgumentException("존재하지 않는 플레이어입니다.");
        }

        this.currentTarget = targetId;
        this.targetHistory.add(targetId);
        this.confessions.clear();
        this.votes.clear();
    }

    public Confession addConfession(String senderId, String message) {
        Confession confession = Confession.builder()
                .id(UUID.randomUUID().toString())
                .senderId(senderId)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        confessions.add(confession);
        return confession;
    }

    public void addExplanation(String confessionId, String explanation) {
        Confession confession = confessions.stream()
                .filter(c -> c.getId().equals(confessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("고해성사 메시지를 찾을 수 없습니다."));

        confession.addExplanation(explanation);
    }

    public VoteResult vote(String playerId, boolean agree) {
        votes.put(playerId, agree);

        int requiredVotes = players.size() - 1; // 대상 제외
        boolean complete = votes.size() >= requiredVotes;
        boolean allAgree = votes.values().stream().allMatch(v -> v);

        return VoteResult.builder()
                .complete(complete)
                .allAgree(allAgree)
                .votes(votes.size())
                .required(requiredVotes)
                .build();
    }

    public void resetGame() {
        this.gameState = GameState.WAITING;
        this.currentTarget = null;
        this.confessions.clear();
        this.votes.clear();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class VoteResult {
        private boolean complete;
        private boolean allAgree;
        private int votes;
        private int required;
    }

    public enum GameState {
        WAITING, PLAYING
    }
}