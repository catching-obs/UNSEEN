package com.confession.game.domain.game.dto;

import com.confession.game.domain.confession.dto.ConfessionDto;
import com.confession.game.domain.player.dto.PlayerDto;
import com.confession.game.domain.room.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStateResponse {
    private String roomId;
    private List<PlayerDto> players;
    private String gameState;
    private String currentTarget;
    private List<String> targetHistory;
    private List<ConfessionDto> confessions;
    private VoteStatus votes;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteStatus {
        private int count;
        private int required;
    }

    public static RoomStateResponse from(Room room) {
        return RoomStateResponse.builder()
                .roomId(room.getRoomId())
                .players(room.getPlayers().values().stream()
                        .map(PlayerDto::from)
                        .toList())
                .gameState(room.getGameState().name())
                .currentTarget(room.getCurrentTarget())
                .targetHistory(room.getTargetHistory())
                .confessions(room.getConfessions().stream()
                        .map(ConfessionDto::from)
                        .toList())
                .votes(VoteStatus.builder()
                        .count(room.getVotes().size())
                        .required(room.getPlayers().size() - 1)
                        .build())
                .build();
    }
}