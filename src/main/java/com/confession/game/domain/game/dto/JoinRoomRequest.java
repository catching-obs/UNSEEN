package com.confession.game.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {
    private String roomId;
    private String playerId;
    private String playerName;
}